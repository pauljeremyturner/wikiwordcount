package com.paulturner.wikiwordcount.domain;

import java.nio.ByteBuffer;

public class ProcessingChunkWithBuffer {

    private long start;
    private long end;
    private ByteBuffer byteBuffer;


    public ProcessingChunkWithBuffer(long start, long end, ByteBuffer byteBuffer) {
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
}
