package com.paulturner.wikiwordcount;

import com.paulturner.wikiwordcount.domain.ChunkDigest;
import com.paulturner.wikiwordcount.domain.FileChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunkWithBuffer;
import com.paulturner.wikiwordcount.io.FileChunkSizer;
import com.paulturner.wikiwordcount.io.FileChunker;
import com.paulturner.wikiwordcount.mongo.MongoCollections;
import com.paulturner.wikiwordcount.text.WordExtractor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class FileChunkProcessor implements Runnable {

    private FileChunkSizer fileChunkSizer;
    private MongoCollections mongoCollections;
    private FileChunker fileChunker;

    public FileChunkProcessor(FileChunkSizer fileChunkSizer, MongoCollections mongoCollections, FileChunker fileChunker) {
        this.fileChunkSizer = fileChunkSizer;
        this.mongoCollections = mongoCollections;
        this.fileChunker = fileChunker;
    }

    @Override
    public void run() {


        final ProcessingChunk reservedChunk = reserveChunk();


        final List<ProcessingChunkWithBuffer> subchunks = fileChunkSizer.splitToSubChunks(reservedChunk);

        final ChunkDigest chunkDigest = new ChunkDigest();



        List<CompletableFuture<ChunkDigest>> completableFutures = subchunks
                .stream()
                .map(pcwb -> {
                    WordExtractor wordExtractor = new WordExtractor(pcwb.getByteBuffer());
                    return CompletableFuture
                            .supplyAsync(() -> wordExtractor.call())
                            .thenApply(cd -> chunkDigest.addChunkDigest(cd));
                })
                .collect(Collectors.toList());


        CompletableFuture
                .allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
                .thenAccept(cf -> mongoCollections.addChunkDigest(chunkDigest));

        try {
            TimeUnit.HOURS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private ProcessingChunk reserveChunk() {
        List<ProcessingChunk> reservedChunks = mongoCollections.readAllChunkPositions();

        FileChunk availableFileChunk = fileChunker.getAvailableFileChunk(reservedChunks);

        ProcessingChunk trimmedChunk = fileChunkSizer.trimToWholePages(availableFileChunk);

        mongoCollections.addReservedChunk(trimmedChunk);

        return trimmedChunk;
    }

    private void completeChunk(ProcessingChunk processingChunk) {
        mongoCollections.completeChunk(processingChunk);
    }
}
