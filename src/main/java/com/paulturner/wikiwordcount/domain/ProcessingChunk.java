package com.paulturner.wikiwordcount.domain;

public class ProcessingChunk implements Comparable<ProcessingChunk> {

    private static final String TOSTRING_MASK = "ProcessingChunk:: [start=%d], [end=%d], [index=%d]";
    private static final String ID_MASK = "%d_%d";


    private long start;
    private long end;
    private long timestamp;
    private boolean processed;
    private boolean processing;
    private int index;

    public ProcessingChunk() {
    }

    public ProcessingChunk(long start, long end, int index) {
        this.start = start;
        this.end = end;
        this.timestamp = System.currentTimeMillis();
        this.index = index;
        processed = false;

    }

    @Override
    public int compareTo(ProcessingChunk processingChunk) {
        return Long.compare(this.start, processingChunk.start);
    }


    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }


    public boolean isProcessing() {
        return processing;
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_MASK, start, end, index);
    }
}
