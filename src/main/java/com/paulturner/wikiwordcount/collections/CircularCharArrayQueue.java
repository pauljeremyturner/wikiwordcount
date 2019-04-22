package com.paulturner.wikiwordcount.collections;

import com.mongodb.annotations.NotThreadSafe;

import java.util.Arrays;
import java.util.stream.IntStream;

@NotThreadSafe
public class CircularCharArrayQueue {

    private final int size;
    private final char[] charArray;
    private int position;

    public CircularCharArrayQueue(final int size) {
        this.size = size;
        charArray = new char[size];
        position = 0;
    }

    public void offer(final char c) {
        if (position < charArray.length) {
            charArray[position] = c;
            position++;
        } else {
            IntStream.range(0, charArray.length - 1).forEach(
                    i -> charArray[i] = charArray[i + 1]
            );
            charArray[charArray.length - 1] = c;
        }
    }

    public char[] subArrayToPosition() {
        return Arrays.copyOfRange(charArray, 0, position);
    }

    public boolean isEmpty() {
        return position == 0;
    }

    public void clear() {
        position = 0;
    }

    public boolean containsArray(final char[] chars) {
        int compareLength = chars.length;
        return Arrays.equals(chars, Arrays.copyOfRange(charArray, 0, compareLength));
    }

    @Override
    public String toString() {
        return new String(charArray);
    }
}
