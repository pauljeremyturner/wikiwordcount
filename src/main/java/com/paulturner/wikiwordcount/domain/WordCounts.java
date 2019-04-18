package com.paulturner.wikiwordcount.domain;

import java.util.ArrayList;
import java.util.List;

public class WordCounts {

    private List<WordCount> wordCountList;

    public WordCounts() {
        this.wordCountList = new ArrayList<>();
    }

    public void addAll(ChunkDigest chunkDigest) {
        this.wordCountList.addAll(chunkDigest.getWordCounts());
    }

    public List<WordCount> getWordCountList() {
        return wordCountList;
    }
}
