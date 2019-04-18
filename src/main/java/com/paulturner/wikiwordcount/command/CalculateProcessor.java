package com.paulturner.wikiwordcount.command;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.domain.FileChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.io.DumpFileChunks;
import com.paulturner.wikiwordcount.mongo.ChunkDigestRepository;
import com.paulturner.wikiwordcount.mongo.ProcessingChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CalculateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CalculateProcessor.class);

    private CalculateOptions calculateOptions;
    private DumpFileChunks dumpFileChunks;
    private ProcessingChunkRepository processingChunkRepository;
    private ChunkDigestRepository chunkDigestRepository;

    private final ExecutorService executorService;

    @Autowired
    public CalculateProcessor(
            CalculateOptions calculateOptions,
            DumpFileChunks dumpFileChunks,
            ProcessingChunkRepository processingChunkRepository,
            ChunkDigestRepository chunkDigestRepository
    ) {
        this.dumpFileChunks = dumpFileChunks;
        this.calculateOptions = calculateOptions;
        this.processingChunkRepository = processingChunkRepository;
        this.chunkDigestRepository = chunkDigestRepository;

        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void process() {

        Optional<ProcessingChunk> unreservedChunkOpt;

        while ((unreservedChunkOpt = reserveUnreserved()).isPresent()) {
            FileChunkWorker fileChunkWorker = new FileChunkWorker(dumpFileChunks, processingChunkRepository, chunkDigestRepository, unreservedChunkOpt.get());
            fileChunkWorker.run();

        }

        while (!dumpFileChunks.fileComplete(processingChunkRepository.findByProcessed(true))) {


            final List<ProcessingChunk> stalereservedChunks = processingChunkRepository
                    .findAll()
                    .stream()
                    .filter(pc -> (pc.getTimestamp() - System.currentTimeMillis()) > TimeUnit.SECONDS.toMillis(30))
                    .collect(Collectors.toList());

            Optional<ProcessingChunk> reservedChunk = processReserved();

            if (reservedChunk.isPresent()) {
                final FileChunkWorker fileChunkWorker = new FileChunkWorker(dumpFileChunks, processingChunkRepository, chunkDigestRepository, reservedChunk.get());
                fileChunkWorker.run();
            }

            //In case file is not complete and another JVM has reserved a chunk but died before completing it
            if (dumpFileChunks.fileComplete(processingChunkRepository.findByProcessed(true))) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    logger.warn("Interruped while waiting for reserved chunks to become stale", e);
                }
            }


        }


    }


    private Optional<ProcessingChunk> reserveUnreserved() {
        List<ProcessingChunk> reservedChunks = processingChunkRepository.findByProcessed(true);

        FileChunk availableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(reservedChunks);

        if (null != availableFileChunk) {
            ProcessingChunk trimmedChunk = dumpFileChunks.trimToWholePages(availableFileChunk);
            processingChunkRepository.insert(trimmedChunk);
            logger.info("Reserved a chunk to be processed [chunk={}]", trimmedChunk);

            return Optional.of(trimmedChunk);
        } else {
            return Optional.empty();
        }

    }


    private Optional<ProcessingChunk> processReserved() {
        List<ProcessingChunk> reservedChunks = processingChunkRepository.findAll().stream().filter(pc -> !pc.isProcessed()).collect(Collectors.toList());

        FileChunk availableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(reservedChunks);

        if (null != availableFileChunk) {
            ProcessingChunk trimmedChunk = dumpFileChunks.trimToWholePages(availableFileChunk);

            processingChunkRepository.insert(trimmedChunk);

            logger.info("Hijacked a chunk to be processed [chunk={}]", trimmedChunk);

            return Optional.of(trimmedChunk);
        } else {
            return Optional.empty();
        }
    }


}
