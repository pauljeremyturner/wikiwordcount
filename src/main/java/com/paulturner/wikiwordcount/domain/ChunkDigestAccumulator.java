package com.paulturner.wikiwordcount.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChunkDigestAccumulator {

    private ConcurrentMap<String, Integer> countMap = new ConcurrentHashMap<>();

    public ChunkDigestAccumulator addChunkDigest(ChunkDigest chunkDigest) {
        countMap.putAll(chunkDigest.getWordCountMap());
        return this;
    }

    public ChunkDigest getAccumulated() {
        return new ChunkDigest(countMap);
    }

}
