package com.paulturner.wikiwordcount.domain;

import org.springframework.data.annotation.Id;

public class ProcessingChunk implements Comparable<ProcessingChunk> {

    private static final String TOSTRING_MASK = "ProcessingChunk:: [start=%d], [end=%d], [file=%s]";
    private static final String ID_MASK = "%d_%d";


    @Id
    private String id;

    private long start;
    private long end;
    private long timestamp;
    private boolean processed;
    private String file;

    public ProcessingChunk() {
    }

    public ProcessingChunk(long start, long end, long timestamp, String file) {
        this.start = start;
        this.end = end;
        this.timestamp = timestamp;
        this.file = file;
        processed = false;

    }

    @Override
    public int compareTo(ProcessingChunk processingChunk) {
        return Long.compare(this.start, processingChunk.start);
    }

    public String getId() {
        return String.format(ID_MASK, start, end);
    }

    public long getLength() {
        return end - start;
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

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_MASK, start, end, file);
    }
}
