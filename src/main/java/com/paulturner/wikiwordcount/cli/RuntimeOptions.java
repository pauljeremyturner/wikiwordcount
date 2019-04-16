package com.paulturner.wikiwordcount.cli;

import java.io.File;

public class RuntimeOptions {

    private static final String TO_STRING_MASK = "RuntimeOptions:: [file=%s] [mongo uri=%s] [chunk size=%d]";
    private File file;
    private String mongoClientUri;
    private int chunkSize;
    private long fileSize;
    private boolean useOffHelpBuffers;

    private RuntimeOptions(
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

    public int getChunkSize() {
        return chunkSize;
    }

    public int getSubchunkSize() {
        return 1024 * 1024 * 1024;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isUseOffHelpBuffers() {
        return useOffHelpBuffers;
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

        public Builder withFile(File file ){
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

        public RuntimeOptions build() {
            return new RuntimeOptions(this.bFile, this.bMongoClientUri, this.bChunkSize, this.bUseOffHelpBuffers);
        }



    }
}
