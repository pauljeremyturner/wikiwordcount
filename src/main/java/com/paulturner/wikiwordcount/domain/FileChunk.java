package com.paulturner.wikiwordcount.domain;


public class FileChunk {

    private static final String TO_STRING_MASK = "FileChunk:: [start=%d] [end=%d] [startbound=%s] [endbound=%s]";

    private long start;
    private long end;

    private boolean endBound;
    private boolean startBound;


    public FileChunk(long start, long end, boolean startBound, boolean endBound) {
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

    @Override
    public String toString() {
        return String.format(TO_STRING_MASK, start, end, startBound, endBound);
    }
}
