package cpu.bus;

import cpu.CPU;
import cpu.interrupts.exceptions.MemoryAddressOutOfBoundsException;
import instructions.Instruction;
import instructions.special.Nop;
import memory.RAM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataBusTest {
    @Test
    void instructionPrefetchUsesOne64BitReadForTwoWords() throws MemoryAddressOutOfBoundsException {
        RAM ram = new RAM(64);
        DataBus bus = new DataBus(ram);
        ram.setWord(0, 0x12345678);
        ram.setWord(4, 0x9ABCDEF0);

        assertEquals(0x12345678, bus.fetchInstruction(0));
        assertEquals(2, bus.getCycleCount());
        assertEquals(0x9ABCDEF0, bus.fetchInstruction(4));
        assertEquals(2, bus.getCycleCount());
        assertTrue(bus.getLastTransfer().startsWith("IF hit"));
    }

    @Test
    void wordStoreDoesNotOverwriteTheOtherHalfOfTheBusLine() throws MemoryAddressOutOfBoundsException {
        RAM ram = new RAM(64);
        DataBus bus = new DataBus(ram);

        bus.writeWord(0, 0x11111111);
        bus.writeWord(4, 0x22222222);

        assertEquals(0x11111111, ram.getWord(0));
        assertEquals(0x22222222, ram.getWord(4));
        assertEquals(0x1111111122222222L, ram.getDoubleWord(0));
    }

    @Test
    void byteStoreUsesByteEnables() throws MemoryAddressOutOfBoundsException {
        RAM ram = new RAM(64);
        DataBus bus = new DataBus(ram);
        bus.writeWord(0, 0xAABBCCDD);
        bus.writeByte(1, (byte) 0x00);

        assertEquals(0xAA00CCDD, ram.getWord(0));
    }

    @Test
    void sequentialNopsShareOneMemoryFetch() throws Exception {
        CPU cpu = new CPU();
        int nop = new Nop().getOpcode() << (Instruction.SIZE - Instruction.OPCODE_SIZE);
        cpu.memory.setWord(0, nop);
        cpu.memory.setWord(4, nop);

        assertFalse(cpu.nextInstruction());
        assertEquals(2, cpu.dataBus.getCycleCount());
        assertFalse(cpu.nextInstruction());
        assertEquals(2, cpu.dataBus.getCycleCount());
    }
}
