package com.paulturner.wikiwordcount.command;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.io.DumpFileChunks;
import com.paulturner.wikiwordcount.mongo.DumpFileDescriptorRepository;
import com.paulturner.wikiwordcount.mongoentity.DumpFileDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CalculateCommand {

    private static final Logger logger = LoggerFactory.getLogger(CalculateCommand.class);

    private final CalculateOptions calculateOptions;
    private final FileChunkWorkerFactory fileChunkWorkerFactory;
    private final DumpFileDescriptorRepository dumpFileDescriptorRepository;
    private final DumpFileChunks dumpFileChunks;

    @Autowired
    public CalculateCommand(CalculateOptions calculateOptions, FileChunkWorkerFactory fileChunkWorkerFactory, DumpFileDescriptorRepository dumpFileDescriptorRepository, DumpFileChunks dumpFileChunks) {
        this.calculateOptions = calculateOptions;
        this.fileChunkWorkerFactory = fileChunkWorkerFactory;
        this.dumpFileDescriptorRepository = dumpFileDescriptorRepository;
        this.dumpFileChunks = dumpFileChunks;
    }


    public void process() {

        DumpFileDescriptor dumpFileDescriptor;
        if (!dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).isPresent()) {

            dumpFileDescriptor = new DumpFileDescriptor(dumpFileChunks.divideToProcessingChunks());
            try {
                dumpFileDescriptor = dumpFileDescriptorRepository.save(dumpFileDescriptor);
            } catch (final DuplicateKeyException dke) {
                logger.info("Another process completed the divide to chunks before this one [descriptor={}]", calculateOptions.getUniqueDumpFileName());
            }
        } else {
            dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
        }


        boolean saved = false;
        List<ProcessingChunk> availableProcessingChunks;
        while (!(availableProcessingChunks = availableProcessingChunks(dumpFileDescriptor)).isEmpty()) {
            int randomProcessingChunkIndex = ThreadLocalRandom.current().nextInt(availableProcessingChunks.size());
            ProcessingChunk availableProcessingChunk = availableProcessingChunks.get(randomProcessingChunkIndex);
            logger.info("Processing a chunk of dump file [chunk #={}]", randomProcessingChunkIndex);
            availableProcessingChunk.setProcessing(true);

            while (!saved) {
                try {
                    dumpFileDescriptor = dumpFileDescriptorRepository.save(dumpFileDescriptor);
                    saved = true;
                } catch (OptimisticLockingFailureException olfe) {
                    dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
                    availableProcessingChunks.get(randomProcessingChunkIndex).setProcessing(true);
                }
            }

            FileChunkWorker fileChunkWorker = fileChunkWorkerFactory.newInstance(availableProcessingChunk, dumpFileDescriptor);
            fileChunkWorker.run();

            saved = false;
            while (!saved) {
                try {
                    dumpFileDescriptor = dumpFileDescriptorRepository.save(dumpFileDescriptor);
                    saved = true;
                } catch (OptimisticLockingFailureException olfe) {
                    dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
                    availableProcessingChunks.get(randomProcessingChunkIndex).setProcessed(true);
                }
            }

        }

        waitForUnfinishedProcessingChunks();

        //we've already seen this so OK to assume it's there
        dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();

        while (!(availableProcessingChunks = staleUnfinishedProcessingChunks(dumpFileDescriptor)).isEmpty()) {
            int randomProcessingChunkIndex = ThreadLocalRandom.current().nextInt(availableProcessingChunks.size());
            ProcessingChunk availableProcessingChunk = availableProcessingChunks.get(randomProcessingChunkIndex);

            logger.info("Hijacking a chunk of dump file [chunk #={}]", randomProcessingChunkIndex);

            FileChunkWorker fileChunkWorker = fileChunkWorkerFactory.newInstance(availableProcessingChunk, dumpFileDescriptor);
            fileChunkWorker.run();


            logger.info("Waiting before hijacking a chunk started by another process but unfinished.");
            waitForUnfinishedProcessingChunks();

        }


    }

    private List<ProcessingChunk> availableProcessingChunks(DumpFileDescriptor dumpFileDescriptor) {
        return dumpFileDescriptor.getProcessingChunks()
                .stream()
                .filter(pc -> (!pc.isProcessed()) && (!pc.isProcessing()))
                .collect(Collectors.toList());

    }

    private List<ProcessingChunk> staleUnfinishedProcessingChunks(DumpFileDescriptor dumpFileDescriptor) {

        return dumpFileDescriptor.getProcessingChunks()
                .stream()
                .filter(pc -> (!pc.isProcessed()) && (pc.isProcessing()) && canHijack(pc))
                .collect(Collectors.toList());
    }


    private void waitForUnfinishedProcessingChunks() {
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException ie) {
            logger.warn("Interruped while waiting for chunk started but not finished", ie);
        }
    }

    private boolean canHijack(final ProcessingChunk pc) {
        return 30L < (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - pc.getTimestamp()));
    }

}
