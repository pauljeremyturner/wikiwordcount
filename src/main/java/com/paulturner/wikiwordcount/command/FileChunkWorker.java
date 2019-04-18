package com.paulturner.wikiwordcount.command;

import com.paulturner.wikiwordcount.domain.ChunkDigestAccumulator;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.domain.Subchunks;
import com.paulturner.wikiwordcount.io.DumpFileChunks;
import com.paulturner.wikiwordcount.mongo.ChunkDigestRepository;
import com.paulturner.wikiwordcount.mongo.ProcessingChunkRepository;
import com.paulturner.wikiwordcount.text.WordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FileChunkWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FileChunkWorker.class);

    private final DumpFileChunks dumpFileChunks;
    private final ProcessingChunkRepository processingChunkRepository;
    private final ChunkDigestRepository chunkDigestRepository;
    private final ProcessingChunk reservedChunk;

    public FileChunkWorker(
            final DumpFileChunks dumpFileChunks, final ProcessingChunkRepository processingChunkRepository,
            final ChunkDigestRepository chunkDigestRepository, final ProcessingChunk reservedChunk) {
        this.dumpFileChunks = dumpFileChunks;
        this.processingChunkRepository = processingChunkRepository;
        this.chunkDigestRepository = chunkDigestRepository;
        this.reservedChunk = reservedChunk;
    }

    @Override
    public void run() {

        final Subchunks subchunks = dumpFileChunks.splitToSubChunks(reservedChunk);

        logger.info("Split a chunk in to subchunks and started extracting words [chunk={}] [subchunk count={}]", reservedChunk, subchunks.getSubchunkList());


        final ChunkDigestAccumulator chunkDigestAccumulator = new ChunkDigestAccumulator();

        final List<CompletableFuture<ChunkDigestAccumulator>> completableFutures = subchunks.getSubchunkList()
                .stream()
                .map(subchunk -> {
                    WordExtractor wordExtractor = new WordExtractor(subchunk.getByteBuffer());
                    return CompletableFuture
                            .supplyAsync(() -> wordExtractor.extract())
                            .thenApply(cd -> chunkDigestAccumulator.addChunkDigest(cd));
                })
                .collect(Collectors.toList());

        CompletableFuture
                .allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
                .thenAccept(cf -> {
                    chunkDigestRepository.insert(chunkDigestAccumulator.getAccumulated());
                    completeChunk(reservedChunk);
                }).join();

        dumpFileChunks.releaseByteBuffer(subchunks.getByteBuffer());

        logger.info("Completed word count of all subchunks [chunk={}]", reservedChunk);
    }


    private void completeChunk(final ProcessingChunk processingChunk) {
        processingChunk.setProcessed(true);
        processingChunkRepository.save(processingChunk);
    }
}
