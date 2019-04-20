package com.paulturner.wikiwordcount.domain;


import java.util.Collections;
import java.util.List;

public class Subchunks {

    private final List<Subchunk> subchunkList;

    public Subchunks(final List<Subchunk> subchunkList) {
        this.subchunkList = Collections.unmodifiableList(subchunkList);
    }


    public List<Subchunk> getSubchunkList() {
        return subchunkList;
    }
}
