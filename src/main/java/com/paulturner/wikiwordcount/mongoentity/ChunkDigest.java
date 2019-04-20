package com.paulturner.wikiwordcount.mongoentity;


import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChunkDigest implements Persistable<String> {


    @Id
    private String id = null;

    private Map<String, Integer> wordCountMap;
    private int index;
    private String fileName;

    public ChunkDigest() {
        wordCountMap = new HashMap<>();
    }

    public ChunkDigest(String fileName, int index, Map<String, Integer> wordCountMap) {
        this();
        this.wordCountMap.putAll(wordCountMap);
        this.index = index;
        this.fileName = fileName;
    }

    public Map<String, Integer> getWordCountMap() {
        return wordCountMap;
    }

    @Override
    public boolean isNew() {
        return Objects.isNull(id);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
