package com.paulturner.wikiwordcount.domain;

import java.nio.ByteBuffer;

public class Subchunk {

    private static final String TO_STRING_MASK = "Subchunk:: [start={}], [end={}]";

    private long start;
    private long end;
    private ByteBuffer byteBuffer;


    public Subchunk(long start, long end, ByteBuffer byteBuffer) {
        this.start = start;
        this.end = end;
        this.byteBuffer = byteBuffer;
    }


    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_MASK, start, end);
    }
}
