package com.paulturner.wikiwordcount.command;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.domain.ChunkDigestAccumulator;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.domain.Subchunks;
import com.paulturner.wikiwordcount.io.DumpFileChunks;
import com.paulturner.wikiwordcount.mongo.ChunkDigestRepository;
import com.paulturner.wikiwordcount.mongo.DumpFileDescriptorRepository;
import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;
import com.paulturner.wikiwordcount.mongoentity.DumpFileDescriptor;
import com.paulturner.wikiwordcount.text.WordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FileChunkWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FileChunkWorker.class);

    private final ChunkDigestRepository chunkDigestRepository;
    private final DumpFileDescriptorRepository dumpFileDescriptorRepository;
    private final ProcessingChunk reservedChunk;
    private final DumpFileDescriptor dumpFileDescriptor;
    private final DumpFileChunks dumpFileChunks;
    private final CalculateOptions calculateOptions;

    public FileChunkWorker(
            final CalculateOptions calculateOptions, final DumpFileChunks dumpFileChunks, final ChunkDigestRepository chunkDigestRepository, final ProcessingChunk reservedChunk,
            final DumpFileDescriptorRepository dumpFileDescriptorRepository, final DumpFileDescriptor dumpFileDescriptor
    ) {
        this.chunkDigestRepository = chunkDigestRepository;
        this.reservedChunk = reservedChunk;
        this.dumpFileDescriptor = dumpFileDescriptor;
        this.dumpFileDescriptorRepository = dumpFileDescriptorRepository;
        this.dumpFileChunks = dumpFileChunks;
        this.calculateOptions = calculateOptions;
    }

    @Override
    public void run() {

        final ByteBuffer byteBuffer = dumpFileChunks.acquireProcessingByteBuffer();
        final Subchunks subchunks = dumpFileChunks.splitToSubChunks(byteBuffer, reservedChunk);

        logger.info("Split a chunk in to subchunks and started extracting words [chunk={}] [subchunk count={}]", reservedChunk, subchunks.getSubchunkList().size());

        final ChunkDigestAccumulator chunkDigestAccumulator = new ChunkDigestAccumulator(reservedChunk.getIndex());

        final List<CompletableFuture<ChunkDigestAccumulator>> chunkDigestFutures = subchunks.getSubchunkList()
                .stream()
                .map(subchunk -> {
                    WordExtractor wordExtractor = new WordExtractor(subchunk.getByteBuffer(), calculateOptions.getUniqueDumpFileName(), reservedChunk.getIndex());
                    return CompletableFuture
                            .supplyAsync(() -> wordExtractor.extract())
                            .thenApply(cd -> chunkDigestAccumulator.addChunkDigest(cd));
                })
                .collect(Collectors.toList());

        CompletableFuture
                .allOf(chunkDigestFutures.toArray(new CompletableFuture[chunkDigestFutures.size()]))
                .thenAccept(cf ->
                    completeChunk(chunkDigestAccumulator, reservedChunk.getIndex())
                ).join();

        dumpFileChunks.releaseProcessingByteBuffer(byteBuffer);

        logger.info("Completed word count of all subchunks [chunk={}]", reservedChunk);
    }

    private void completeChunk(ChunkDigestAccumulator chunkDigestAccumulator, int index) {
        ChunkDigest accumulated = chunkDigestAccumulator.getAccumulated(calculateOptions.getUniqueDumpFileName());
        try {
            logger.info("Save ChunkDigest [index={}]", index);
            chunkDigestRepository.insert(accumulated);
        } catch (final DuplicateKeyException dke) {
            logger.info("Another process inserted a chunk digest before this process could, nevermind.  [processing chunk index={}]", index);
        }

        boolean saved = false;

        DumpFileDescriptor dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
        while (!saved) {
            try {
                ProcessingChunk processingChunk = dumpFileDescriptor.getProcessingChunks().get(index);
                processingChunk.setProcessed(true);
                processingChunk.setProcessing(true);
                dumpFileDescriptor = dumpFileDescriptorRepository.save(dumpFileDescriptor);
                saved = true;
            } catch (final OptimisticLockingFailureException olfe) {
                logger.info("Optimistic lock fail when trying to mark processing chunk as processed, retrying... [processingchunk={}]");
                dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
            }
        }
    }


}
