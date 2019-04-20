package com.paulturner.wikiwordcount.cli;

import java.io.File;

public class CalculateOptions extends AbstractCommandOptions {

    private static final String TO_STRING_MASK = "CalculateOptions:: [file=%s] [mongo uri=%s] [chunk size=%d]";
    private static final String NAME_MASK = "dump-file-%s-%d-%d";
    private boolean useOffHelpBuffers;

    private CalculateOptions(
            File file, String mongoClientUri, int chunkSize, boolean useOffHelpBuffers
    ) {
        super(file, mongoClientUri, chunkSize);
        this.useOffHelpBuffers = useOffHelpBuffers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getSubchunkSize() {
        return 1024 * 1024 * 128;
    }

    public boolean isUseOffHelpBuffers() {
        return useOffHelpBuffers;
    }

    public String getUniqueDumpFileName() {
        return String.format(NAME_MASK, getFile().getName(), getFileSize(), getChunkSize());
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_MASK, getFile().getAbsolutePath(), getMongoClientUri(), getChunkSize());
    }

    public static class Builder {
        private File bFile;
        private String bMongoClientUri;
        private int bChunkSize;
        private boolean bUseOffHelpBuffers;
        private Builder() {

        }

        public Builder withFile(File file) {
            this.bFile = file;
            return this;
        }

        public Builder withMongoClientUri(String mongoClientUri) {
            this.bMongoClientUri = mongoClientUri;
            return this;
        }

        public Builder withChunkSize(int chunkSize) {
            this.bChunkSize = chunkSize;
            return this;
        }

        public Builder withUseOffHeapBuffers(boolean useOffHeapBuffers) {
            this.bUseOffHelpBuffers = useOffHeapBuffers;
            return this;
        }

        public CalculateOptions build() {
            return new CalculateOptions(this.bFile, this.bMongoClientUri, this.bChunkSize, this.bUseOffHelpBuffers);
        }
    }

}
