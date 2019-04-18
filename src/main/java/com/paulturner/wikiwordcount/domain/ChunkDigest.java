package com.paulturner.wikiwordcount.domain;


import java.util.*;

public class ChunkDigest {


    private Map<String, Integer> wordCountMap;

    public ChunkDigest() {
        wordCountMap = new HashMap<>();
    }

    public ChunkDigest(Map<String, Integer> wordCountMap) {
        this();
        this.wordCountMap.putAll(wordCountMap);
    }

    public int wordCount(String word) {
        Integer count = wordCountMap.get(word);
        return Objects.isNull(count) ? 0 : count;
    }

    public ChunkDigest addChunkDigest(ChunkDigest chunkDigest) {

        chunkDigest.wordCountMap.entrySet().stream().forEach(
                entry -> {
                    this.wordCountMap.computeIfAbsent(entry.getKey(), k -> entry.getValue());
                    this.wordCountMap.computeIfPresent(entry.getKey(), (k, v) -> (v + entry.getValue()));
                }
        );
        return this;
    }

    public List<WordCount> getWordCounts() {
        final List<WordCount> wordCounts = new ArrayList<>(wordCountMap.size());
        wordCountMap.entrySet().stream().forEach(
                entry -> wordCounts.add(new WordCount(entry.getKey(), entry.getValue()))
        );
        return wordCounts;
    }

    public Map<String, Integer> getWordCountMap() {
        return wordCountMap;
    }
}
