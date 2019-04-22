package com.paulturner.wikiwordcount.collections;

public class CircularCharArrayQueue {
    private final int capacity;
    int writeIndex;
    private final int endIndex;
    private final char[] buffer;

    public CircularCharArrayQueue(int capacity) {
        this.capacity = capacity;
        this.buffer = new char[capacity];
        endIndex = capacity - 1;
        writeIndex = 0;
    }

    public void offer(char b) {
        buffer[writeIndex] = b;
        setNextWritePosition();
    }

    public boolean contains(char[] chars) {

        int length = chars.length;
        if (length > capacity) {
            return false;
        }

        int readIndex = writeIndex - length;
        readIndex = readIndex < 0 ? capacity + readIndex : readIndex;
        for (int i = 0; i < length; i++) {
            if (chars[i] != buffer[readIndex]) {
                return false;
            }
            readIndex++;
            if (readIndex > endIndex) {
                readIndex = 0;
            }
        }
        return true;
    }

    public char[] subarray(int length, char[] array) {
        int readIndex = writeIndex - length - 1;
        readIndex = readIndex < 0 ? capacity + readIndex : readIndex;
        for (int i = 0; i < length; i++) {
            array[i] = buffer[readIndex++];
            if (readIndex > endIndex) {
                readIndex = 0;

            }
        }
        return array;
    }


    private void setNextWritePosition() {
        writeIndex = (writeIndex == endIndex) ? 0 : writeIndex + 1;
    }

    @Override
    public String toString() {
        return new String(buffer);
    }
}