package com.paulturner.wikiwordcount.collections;

import com.mongodb.annotations.NotThreadSafe;

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
            byteArray[pos] = b;
            pos++;
        } else {
            IntStream.range(0, byteArray.length - 1).forEach(
                    i -> byteArray[i] = byteArray[i + 1]
            );
            byteArray[byteArray.length - 1] = b;
        }
    }

    public void clear() {
        Arrays.fill(byteArray, (byte) 0);
        pos = 0;
    }

    public boolean containsArray(final byte[] bytes) {
        return Arrays.equals(bytes, byteArray);
    }

}
