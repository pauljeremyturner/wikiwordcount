package com.paulturner.wikiwordcount.domain;

import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChunkDigestAccumulator {

    private final int index;

    public ChunkDigestAccumulator(int index) {
        this.index = index;
    }

    public ChunkDigestAccumulator() {
        this.index = -1;
    }

    private ConcurrentMap<String, Integer> wordCountMap = new ConcurrentHashMap<>();

    public ChunkDigestAccumulator addChunkDigest(ChunkDigest chunkDigest) {

        chunkDigest.getWordCountMap().entrySet().stream().forEach(
                entry -> {
                    this.wordCountMap.computeIfAbsent(entry.getKey(), k -> entry.getValue());
                    this.wordCountMap.computeIfPresent(entry.getKey(), (k, v) -> (v + entry.getValue()));
                }
        );
        return this;
    }

    public ChunkDigest getAccumulated(String filename) {
        return new ChunkDigest(filename, index, wordCountMap);
    }

}
