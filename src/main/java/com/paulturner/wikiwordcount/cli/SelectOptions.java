package com.paulturner.wikiwordcount.cli;

public class SelectOptions {

    private static final String TO_STRING_MASK = "SelectOptions:: [mongo uri=%s] [sort-direction=%s] [word-count number=%d] [word-length=%s]";


    private Integer count;
    private String mongoClientUri;
    private Direction direction;
    private Integer wordLength;
    private String filePath;

    public SelectOptions(Integer count, String mongoClientUri, Direction direction, Integer wordLength, String filePath) {
        this.count = count;
        this.mongoClientUri = mongoClientUri;
        this.direction = direction;
        this.wordLength = wordLength;
        this.filePath = filePath;
    }

    public enum Direction {ASC, DESC}

    @Override
    public String toString() {
        return String.format(TO_STRING_MASK, mongoClientUri, direction, count, wordLength);
    }

    public static String getToStringMask() {
        return TO_STRING_MASK;
    }

    public Integer getCount() {
        return count;
    }

    public String getMongoClientUri() {
        return mongoClientUri;
    }

    public Direction getDirection() {
        return direction;
    }

    public Integer getWordLength() {
        return wordLength;
    }

    public String getFilePath() {
        return filePath;
    }
}
