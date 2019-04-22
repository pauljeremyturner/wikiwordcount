package com.paulturner.wikiwordcount.cli;

import java.io.File;

public class SelectOptions extends AbstractCommandOptions {

    private static final String TO_STRING_MASK = "SelectOptions:: [mongo uri=%s] [sort-direction=%s] [word-count number=%d] [word-length=%s]";
    private static final String NAME_MASK = "dump-file-%s-%d-%d";


    private Integer count;
    private Direction direction;
    private Integer wordLength;

    private SelectOptions(
            File file, String mongoClientUri, int chunkSize,
            Integer count, Direction direction, Integer wordLength
    ) {
        super(file, mongoClientUri, chunkSize);
        this.count = count;
        this.direction = direction;
        this.wordLength = wordLength;
    }

    public static SelectOptions.Builder builder() {
        return new SelectOptions.Builder();
    }

    public String getUniqueDumpFileName() {
        return String.format(NAME_MASK, getFile().getName(), getFile().length(), getChunkSize());
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_MASK, getMongoClientUri(), direction, count, wordLength);
    }

    public Integer getCount() {
        return count;
    }

    public Direction getDirection() {
        return direction;
    }

    public Integer getWordLength() {
        return wordLength;
    }


    public enum Direction {ASC, DESC}

    public static class Builder {
        private File bFile;
        private String bMongoClientUri;
        private int bChunkSize;
        private Direction bDirection;
        private int bWordLength;
        private int bCount;

        private Builder() {

        }

        public SelectOptions.Builder withFile(File file) {
            this.bFile = file;
            return this;
        }

        public SelectOptions.Builder withMongoClientUri(String mongoClientUri) {
            this.bMongoClientUri = mongoClientUri;
            return this;
        }

        public SelectOptions.Builder withChunkSize(int chunkSize) {
            this.bChunkSize = chunkSize;
            return this;
        }

        public SelectOptions.Builder withCount(int count) {
            this.bCount = count;
            return this;
        }

        public SelectOptions.Builder withDirection(Direction direction) {
            this.bDirection = direction;
            return this;
        }


        public SelectOptions.Builder withWordLength(int wordLength) {
            this.bWordLength = wordLength;
            return this;
        }

        public SelectOptions build() {
            return new SelectOptions(this.bFile, this.bMongoClientUri, this.bChunkSize, this.bCount, this.bDirection, this.bWordLength);
        }
    }

}
