package cpu.bus;

import cpu.interrupts.exceptions.MemoryAddressOutOfBoundsException;
import cpu.ports.Port;
import memory.RAM;
import memory.cells.DoubleWordCell;
import memory.cells.HalfWordCell;
import memory.cells.WordCell;

import java.nio.ByteBuffer;

/**
 * Совмещённая (мультиплексированная) шина адреса и данных.
 * Разрядность данных — 64 бита, адрес на фазе адреса занимает младшие 32 линии AD.
 * Одно обращение к памяти занимает два такта: фаза адреса и фаза данных.
 */
public class DataBus {
    public static final int WIDTH_BITS = DoubleWordCell.SIZE;
    public static final int WIDTH_BYTES = DoubleWordCell.BYTES_NUMBER;

    public enum Phase {
        IDLE, ADDRESS, DATA
    }

    private final RAM memory;
    private Phase phase;
    private long adLines;
    private int latchedAddress;
    private int cycleCount;
    private String lastTransfer;
    private boolean prefetchValid;
    private int prefetchAlignedAddress;
    private long prefetchData;

    public DataBus(RAM memory) {
        this.memory = memory;
        clear();
    }

    public static int align(int address) {
        return address & ~(WIDTH_BYTES - 1);
    }

    public void clear() {
        phase = Phase.IDLE;
        adLines = 0;
        latchedAddress = 0;
        cycleCount = 0;
        lastTransfer = "idle";
        discardPrefetch();
    }

    public void discardPrefetch() {
        invalidatePrefetch();
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public Phase getPhase() {
        return phase;
    }

    public long getAdLines() {
        return adLines;
    }

    public String getLastTransfer() {
        return lastTransfer;
    }

    public boolean isPrefetchValid() {
        return prefetchValid;
    }

    public int fetchInstruction(int pc) throws MemoryAddressOutOfBoundsException {
        int aligned = align(pc);
        int offset = pc - aligned;
        if (offset < 0 || offset + WordCell.BYTES_NUMBER > WIDTH_BYTES) {
            return readWord(pc);
        }
        if (prefetchValid && prefetchAlignedAddress == aligned) {
            lastTransfer = String.format("IF hit 0x%08X", pc);
            return extractInt(prefetchData, offset);
        }
        long doubleWord = readDoubleWord(aligned);
        prefetchValid = true;
        prefetchAlignedAddress = aligned;
        prefetchData = doubleWord;
        lastTransfer = String.format("IF miss 0x%08X", pc);
        return extractInt(doubleWord, offset);
    }

    public byte readByte(int address) throws MemoryAddressOutOfBoundsException {
        return transferRead(address, 1)[0];
    }

    public short readHalfWord(int address) throws MemoryAddressOutOfBoundsException {
        return ByteBuffer.wrap(transferRead(address, HalfWordCell.BYTES_NUMBER)).getShort();
    }

    public int readWord(int address) throws MemoryAddressOutOfBoundsException {
        return ByteBuffer.wrap(transferRead(address, WordCell.BYTES_NUMBER)).getInt();
    }

    public void writeByte(int address, byte value) throws MemoryAddressOutOfBoundsException {
        transferWrite(address, new byte[]{value});
    }

    public void writeHalfWord(int address, short value) throws MemoryAddressOutOfBoundsException {
        transferWrite(address, ByteBuffer.allocate(HalfWordCell.BYTES_NUMBER).putShort(value).array());
    }

    public void writeWord(int address, int value) throws MemoryAddressOutOfBoundsException {
        transferWrite(address, ByteBuffer.allocate(WordCell.BYTES_NUMBER).putInt(value).array());
    }

    public int isolatedIn(Port port) {
        addressPhase(0);
        phase = Phase.DATA;
        int value = port.receiveDataFromPin();
        adLines = Integer.toUnsignedLong(value);
        cycleCount++;
        lastTransfer = String.format("IN -> 0x%08X", value);
        phase = Phase.IDLE;
        return value;
    }

    public void isolatedOut(Port port, int value) {
        addressPhase(0);
        phase = Phase.DATA;
        adLines = Integer.toUnsignedLong(value);
        port.sendDataToPin(value);
        cycleCount++;
        lastTransfer = String.format("OUT <- 0x%08X", value);
        phase = Phase.IDLE;
    }

    public String statusText() {
        return String.format(
                "<html>Data bus: %d-bit multiplexed AD<br>Bus cycles: %d<br>Last: %s</html>",
                WIDTH_BITS,
                cycleCount,
                lastTransfer
        );
    }

    private byte[] transferRead(int address, int size) throws MemoryAddressOutOfBoundsException {
        byte[] result = new byte[size];
        int copied = 0;
        while (copied < size) {
            int current = address + copied;
            int aligned = align(current);
            int offset = current - aligned;
            long doubleWord = readDoubleWord(aligned);
            byte[] line = longToBytes(doubleWord);
            int chunk = Math.min(size - copied, WIDTH_BYTES - offset);
            System.arraycopy(line, offset, result, copied, chunk);
            copied += chunk;
        }
        return result;
    }

    private void transferWrite(int address, byte[] payload) throws MemoryAddressOutOfBoundsException {
        int written = 0;
        while (written < payload.length) {
            int current = address + written;
            int aligned = align(current);
            int offset = current - aligned;
            int chunk = Math.min(payload.length - written, WIDTH_BYTES - offset);
            boolean[] enable = new boolean[WIDTH_BYTES];
            byte[] line = new byte[WIDTH_BYTES];
            for (int i = 0; i < chunk; ++i) {
                enable[offset + i] = true;
                line[offset + i] = payload[written + i];
            }
            writeLine(aligned, line, enable);
            written += chunk;
        }
    }

    public long readDoubleWord(int address) throws MemoryAddressOutOfBoundsException {
        int aligned = align(address);
        addressPhase(aligned);
        phase = Phase.DATA;
        try {
            adLines = memory.getDoubleWord(aligned);
        } catch (IndexOutOfBoundsException e) {
            phase = Phase.IDLE;
            throw new MemoryAddressOutOfBoundsException("Memory address out of bounds: " + aligned);
        }
        cycleCount++;
        lastTransfer = String.format("RD 0x%08X -> 0x%016X", aligned, adLines);
        phase = Phase.IDLE;
        return adLines;
    }

    private void writeLine(int aligned, byte[] line, boolean[] enable) throws MemoryAddressOutOfBoundsException {
        addressPhase(aligned);
        phase = Phase.DATA;
        try {
            for (int i = 0; i < WIDTH_BYTES; ++i) {
                memory.setByteIfEnabled(aligned + i, line[i], enable[i]);
            }
            adLines = 0;
            for (int i = 0; i < WIDTH_BYTES; ++i) {
                adLines = (adLines << 8) | (memory.getByte(aligned + i) & 0xFF);
            }
        } catch (IndexOutOfBoundsException e) {
            phase = Phase.IDLE;
            throw new MemoryAddressOutOfBoundsException("Memory address out of bounds: " + aligned);
        }
        cycleCount++;
        lastTransfer = String.format("WR 0x%08X <- 0x%016X", aligned, adLines);
        phase = Phase.IDLE;
        if (prefetchValid && prefetchAlignedAddress == aligned) {
            invalidatePrefetch();
        }
    }

    private void addressPhase(int aligned) {
        phase = Phase.ADDRESS;
        latchedAddress = aligned;
        adLines = Integer.toUnsignedLong(aligned);
        cycleCount++;
    }

    private void invalidatePrefetch() {
        prefetchValid = false;
        prefetchAlignedAddress = 0;
        prefetchData = 0;
    }

    private static int extractInt(long doubleWord, int byteOffset) {
        int shift = (WIDTH_BYTES - byteOffset - WordCell.BYTES_NUMBER) * 8;
        return (int) (doubleWord >>> shift);
    }

    private static byte[] longToBytes(long value) {
        return ByteBuffer.allocate(WIDTH_BYTES).putLong(value).array();
    }
}
