package com.paulturner.wikiwordcount.collections;

import com.mongodb.annotations.NotThreadSafe;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;

@NotThreadSafe
public class CircularByteArrayQueue {

    private final int size;
    private final byte[] byteArray;
    private int pos;

    public CircularByteArrayQueue(final int size) {
        this.size = size;
        byteArray = new byte[size];
        pos = 0;

    }

    public void offer(final byte b) {
        if (pos < byteArray.length) {
            byteArray[pos++] = b;
        } else {
            IntStream.range(0, byteArray.length - 1).forEach(i -> byteArray[i] = byteArray[i + 1]);
            byteArray[byteArray.length - 1] = b;
        }
    }

    public boolean containsArray(final byte[] bytes) {
        return Arrays.equals(Arrays.copyOfRange(byteArray, Math.max(0, byteArray.length - bytes.length), byteArray.length), bytes);
    }

    @Override
    public String toString() {
        return new String(byteArray, StandardCharsets.UTF_8);
    }
}
