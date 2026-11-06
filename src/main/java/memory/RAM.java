package memory;

import memory.cells.ByteCell;
import memory.cells.DoubleWordCell;
import memory.cells.HalfWordCell;
import memory.cells.WordCell;

import java.util.ArrayList;
import java.util.List;

public class RAM {
    private List<ByteCell> data;

    public RAM(int bytesCount) {
        if (bytesCount % DoubleWordCell.BYTES_NUMBER != 0)
            throw new IllegalArgumentException();
        data = new ArrayList<>(bytesCount);
        for (int count = 0; count < bytesCount; ++count)
            data.add(new ByteCell());
    }

    public int getSize() {
        return data.size();
    }

    public byte getByte(int index) {
        return data.get(index).getValue();
    }

    public void setByte(int index, byte _byte) {
        data.get(index).setValue(_byte);
    }

    public short getHalfWord(int index) {
        return new HalfWordCell(data.subList(index, index + HalfWordCell.BYTES_NUMBER)).getValue();
    }

    public void setHalfWord(int index, short halfWord) {
        new HalfWordCell(data.subList(index, index + HalfWordCell.BYTES_NUMBER)).setValue(halfWord);
    }

    public int getWord(int index) {
        return new WordCell(data.subList(index, index + WordCell.BYTES_NUMBER)).getValue();
    }

    public void setWord(int index, int word) {
        new WordCell(data.subList(index, index + WordCell.BYTES_NUMBER)).setValue(word);
    }

    public long getDoubleWord(int index) {
        return new DoubleWordCell(data.subList(index, index + DoubleWordCell.BYTES_NUMBER)).getValue();
    }

    public void setDoubleWord(int index, long doubleWord) {
        new DoubleWordCell(data.subList(index, index + DoubleWordCell.BYTES_NUMBER)).setValue(doubleWord);
    }

    public void setByteIfEnabled(int index, byte value, boolean enabled) {
        if (enabled) {
            setByte(index, value);
        }
    }

    public List<WordCell> asWordList() {
        List<WordCell> words = new ArrayList<>(data.size() / WordCell.BYTES_NUMBER);
        for (int i = 0; i < data.size(); i += WordCell.BYTES_NUMBER)
            words.add(new WordCell(data.subList(i, i + WordCell.BYTES_NUMBER)));
        return words;
    }

    public void clear() {
        for (var cell : data)
            cell.clear();
    }
}
