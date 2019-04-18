package com.paulturner.wikiwordcount.cli;

import java.io.File;

public class CalculateOptions {

    private static final String TO_STRING_MASK = "CalculateOptions:: [file=%s] [mongo uri=%s] [chunk size=%d]";
    private static final String NAME_MASK = "dump-file-%s-%d";
    private File file;
    private String mongoClientUri;
    private long chunkSize;
    private long fileSize;
    private boolean useOffHelpBuffers;

    private CalculateOptions() {
    }

    private CalculateOptions(
            File file, String mongoClientUri, int chunkSize, boolean useOffHelpBuffers
    ) {
        this.file = file;
        this.mongoClientUri = mongoClientUri;
        this.chunkSize = chunkSize;
        this.fileSize = file.length();
        this.useOffHelpBuffers = useOffHelpBuffers;
    }

    public File getFile() {
        return file;
    }

    public String getMongoClientUri() {
        return mongoClientUri;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public int getSubchunkSize() {
        return 1024 * 1024 * 128;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isUseOffHelpBuffers() {
        return useOffHelpBuffers;
    }

    public String getUniqueDumpFileName() {
        return String.format(NAME_MASK, file.getName(), file.length());
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_MASK, file.getAbsolutePath(), mongoClientUri.toString(), chunkSize);
    }

    public static class Builder {
        private File bFile;
        private String bMongoClientUri;
        private int bChunkSize;
        private long bFileSize;
        private boolean bUseOffHelpBuffers;

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
