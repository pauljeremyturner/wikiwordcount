package com.paulturner.wikiwordcount.cli;

import java.io.File;

public class AbstractCommandOptions {

    private File file;
    private String mongoClientUri;
    private int chunkSize;

    public AbstractCommandOptions(File file, String mongoClientUri, int chunkSize) {
        this.file = file;
        this.mongoClientUri = mongoClientUri;
        this.chunkSize = chunkSize;
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

    public long getFileSize() {
        return file.length();
    }

}
