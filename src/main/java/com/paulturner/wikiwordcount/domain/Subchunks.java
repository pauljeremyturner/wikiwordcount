package com.paulturner.wikiwordcount.domain;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class Subchunks {

    final ByteBuffer byteBuffer;
    final List<Subchunk> subchunkList;

    public Subchunks(final ByteBuffer byteBuffer, final List<Subchunk> subchunkList) {
        this.byteBuffer = byteBuffer;
        this.subchunkList = Collections.unmodifiableList(subchunkList);
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public List<Subchunk> getSubchunkList() {
        return subchunkList;
    }
}
