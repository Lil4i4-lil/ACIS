package memory.cells;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DoubleWordCell implements Cell<Long> {
    public static final int SIZE = 64;
    public static final int BYTES_NUMBER = 8;
    private final List<ByteCell> bytes;

    public DoubleWordCell() {
        this(0L);
    }

    public DoubleWordCell(long doubleWord) {
        this.bytes = new ArrayList<>();
        var encoded = ByteBuffer.allocate(BYTES_NUMBER).putLong(doubleWord).array();
        for (var _byte : encoded) {
            this.bytes.add(new ByteCell(_byte));
        }
    }

    public DoubleWordCell(List<ByteCell> bytes) {
        this.bytes = bytes;
    }

    @Override
    public Long getValue() {
        long value = 0;
        for (var _byte : bytes) {
            value = (value << 8) + (_byte.getValue() & 0xFF);
        }
        return value;
    }

    @Override
    public void setValue(Long value) {
        var encoded = ByteBuffer.allocate(BYTES_NUMBER).putLong(value).array();
        for (int i = 0; i < BYTES_NUMBER; ++i) {
            this.bytes.get(i).setValue(encoded[i]);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.size(); ++i) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(bytes.get(i).toString());
        }
        return builder.toString();
    }

    @Override
    public void clear() {
        for (var cell : bytes)
            cell.clear();
    }

    @Override
    public ByteCell toByteCell() {
        return new ByteCell(bytes.get(BYTES_NUMBER - 1).getValue());
    }

    @Override
    public HalfWordCell toHalfWord() {
        List<ByteCell> half = new ArrayList<>();
        half.add(new ByteCell(bytes.get(BYTES_NUMBER - 2).getValue()));
        half.add(new ByteCell(bytes.get(BYTES_NUMBER - 1).getValue()));
        return new HalfWordCell(half);
    }

    @Override
    public WordCell toWordCell() {
        List<ByteCell> word = new ArrayList<>();
        for (int i = BYTES_NUMBER - WordCell.BYTES_NUMBER; i < BYTES_NUMBER; ++i) {
            word.add(new ByteCell(bytes.get(i).getValue()));
        }
        return new WordCell(word);
    }
}
