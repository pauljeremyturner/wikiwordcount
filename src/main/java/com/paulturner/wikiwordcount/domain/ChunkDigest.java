package com.paulturner.wikiwordcount.domain;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChunkDigest {

    private ConcurrentMap<String, Integer> wordCountMap;

    public ChunkDigest() {
        wordCountMap = new ConcurrentHashMap<>();
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
}
