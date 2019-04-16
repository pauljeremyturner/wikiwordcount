package com.paulturner.wikiwordcount.domain;


public class FileChunk {

    private long start;
    private long end;

    private boolean endBound;
    private boolean startBound;


    public FileChunk(long start, long end, boolean endBound, boolean startBound) {
        this.start = start;
        this.end = end;
        this.endBound = endBound;
        this.startBound = startBound;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public boolean isEndBound() {
        return endBound;
    }

    public boolean isStartBound() {
        return startBound;
    }
}
