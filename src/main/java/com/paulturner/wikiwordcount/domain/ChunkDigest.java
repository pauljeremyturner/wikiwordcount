package com.paulturner.wikiwordcount.domain;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChunkDigest {

    private Map<String, Integer> wordCountMap;

    public ChunkDigest(Map<String, Integer> wordCountMap) {
        this.wordCountMap = wordCountMap;
    }

    public int wordCount(String word) {
        Integer count = wordCountMap.get(word);
        return Objects.isNull(count) ? 0 : count;
    }


    public List<WordCount> getWordCounts() {
        final List<WordCount> wordCounts = new ArrayList<>(wordCountMap.size());
        wordCountMap.entrySet().stream().forEach(
                entry -> wordCounts.add(new WordCount(entry.getKey(), entry.getValue()))
        );
        return wordCounts;
    }
}
