package com.paulturner.wikiwordcount;

import com.paulturner.wikiwordcount.cli.RuntimeOptions;
import com.paulturner.wikiwordcount.domain.FileChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunkWithBuffer;
import com.paulturner.wikiwordcount.io.FileChunkSizer;
import com.paulturner.wikiwordcount.io.FileChunker;
import com.paulturner.wikiwordcount.mongo.MongoCollections;
import com.paulturner.wikiwordcount.text.WordExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class FileProcessor {

    private final ExecutorService wordCountDigestPool = Executors.newFixedThreadPool(8);
    private RuntimeOptions runtimeOptions;
    private FileChunker fileChunker;
    private FileChunkSizer fileChunkSizer;
    private MongoCollections mongoCollections;


    @Autowired
    public FileProcessor(
            RuntimeOptions runtimeOptions,
            FileChunkSizer fileChunkSizer,
            MongoCollections mongoCollections,
            FileChunker fileChunker
    ) {


        this.fileChunkSizer = fileChunkSizer;
        this.runtimeOptions = runtimeOptions;
        this.mongoCollections = mongoCollections;
        this.fileChunker = fileChunker;
    }


    public void process() {

        ConcurrentMap<String, Integer> wordCountMap = new ConcurrentHashMap<>();

        ProcessingChunk reservedChunk = reserveChunk();


        List<ProcessingChunkWithBuffer> subchunks = fileChunkSizer.splitToSubChunks(reservedChunk);


        WordExtractor wordExtractor = new WordExtractor(subchunks.get(0).getByteBuffer());

        wordCountDigestPool.submit(wordExtractor);


    }

    private ProcessingChunk reserveChunk() {
        List<ProcessingChunk> reservedChunks = mongoCollections.readAllChunkPositions();

        FileChunk availableFileChunk = fileChunker.getAvailableFileChunk(reservedChunks);

        ProcessingChunk trimmedChunk = fileChunkSizer.trimToWholeLines(availableFileChunk);

        mongoCollections.addReservedChunk(trimmedChunk);

        return trimmedChunk;

    }


}
