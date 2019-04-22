package com.paulturner.wikiwordcount.collections;

import java.nio.charset.StandardCharsets;

public class CircularByteArrayQueue {
    private final int capacity;
    int writeIndex;
    private final int endIndex;
    private final byte[] buffer;

    public CircularByteArrayQueue(int capacity) {
        this.capacity = capacity;
        this.buffer = new byte[capacity];
        endIndex = capacity - 1;
        writeIndex = 0;
    }

    public void offer(byte b) {
        buffer[writeIndex] = b;
        setNextWritePosition();
    }

    public boolean contains(byte[] bytes) {

        int length = bytes.length;
        if (length > capacity) {
            return false;
        }

        int readIndex = writeIndex - length;
        readIndex = readIndex < 0 ? capacity + readIndex : readIndex;
        for (int i = 0; i < length; i++) {
            if (bytes[i] != buffer[readIndex]) {
                return false;
            }
            readIndex++;
            if (readIndex > endIndex) {
                readIndex = 0;
            }
        }
        return true;
    }


    private void setNextWritePosition() {
        writeIndex = (writeIndex == endIndex) ? 0 : writeIndex + 1;
    }

    @Override
    public String toString() {
        return new String(buffer, StandardCharsets.US_ASCII);
    }
}