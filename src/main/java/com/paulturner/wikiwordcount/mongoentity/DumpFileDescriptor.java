package com.paulturner.wikiwordcount.mongoentity;

import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;

import java.util.List;
import java.util.Objects;

public class DumpFileDescriptor implements Persistable<String> {

    @Id
    private String id;

    @Version
    private int version;

    private List<ProcessingChunk> processingChunks;

    private boolean complete = false;

    public DumpFileDescriptor(final List<ProcessingChunk> processingChunks) {
        this.processingChunks = processingChunks;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return Objects.isNull(getId());
    }

    public List<ProcessingChunk> getProcessingChunks() {
        return processingChunks;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
