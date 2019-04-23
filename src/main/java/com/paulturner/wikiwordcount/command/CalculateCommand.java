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

        DumpFileDescriptor dumpFileDescriptor = divideToProcessingChunks();

        processUnstartedChunks(dumpFileDescriptor);

        hijackStartedChunks();

        markAsComplete();

    }

    private DumpFileDescriptor divideToProcessingChunks() {
        DumpFileDescriptor dumpFileDescriptor;
        if (!dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).isPresent()) {

            dumpFileDescriptor = new DumpFileDescriptor(dumpFileChunks.divideToProcessingChunks());
            try {
                dumpFileDescriptor = dumpFileDescriptorRepository.save(dumpFileDescriptor);
            } catch (final DuplicateKeyException dke) {
                logger.info("Another process completed the divide to chunks before this one [descriptor={}]", calculateOptions.getUniqueDumpFileName());
            }
        } else {
            logger.info("Another process completed the divide to chunks before this one [descriptor={}]", calculateOptions.getUniqueDumpFileName());
            dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
        }
        return dumpFileDescriptor;
    }

    private void markAsComplete() {
        DumpFileDescriptor dumpFileDescriptor;
        dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
        boolean saved = false;
        while (!saved && !dumpFileDescriptor.isComplete()) {
            try {
                dumpFileDescriptor.setComplete(true);
                dumpFileDescriptor = dumpFileDescriptorRepository.save(dumpFileDescriptor);
                logger.info("Marked dumpFileDescriptor as complete");
                saved = true;
            } catch (final OptimisticLockingFailureException olfe) {
                logger.info("Tried to mark dumpFileDescriptor as complete but is stale, retrying");
                dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
                dumpFileDescriptor.setComplete(true);
            }
        }

        logger.info("Completed calculation stage");
    }

    private void hijackStartedChunks() {
        DumpFileDescriptor dumpFileDescriptor;
        List<ProcessingChunk> availableProcessingChunks;//we've already seen this so OK to assume it's there
        dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();

        while (!(availableProcessingChunks = staleUnfinishedProcessingChunks(dumpFileDescriptor)).isEmpty()) {

            logger.info(
                    "Remaining processing chunks to be hijacked [count={}] [indexes={}]",
                    availableProcessingChunks.size(),
                    availableProcessingChunks.stream().map(pc -> pc.getIndex()).collect(Collectors.toList())
            );


            int randomInt = ThreadLocalRandom.current().nextInt(availableProcessingChunks.size());
            ProcessingChunk availableProcessingChunk = availableProcessingChunks.get(randomInt);
            int availableChunkIndex = availableProcessingChunk.getIndex();

            logger.info("Hijacking a chunk of dump file [chunk #={}]", availableChunkIndex);

            fileChunkWorkerFactory.newInstance(availableProcessingChunk, dumpFileDescriptor).run();


            logger.info("Waiting before hijacking a chunk started by another process but unfinished.");
            waitForUnfinishedProcessingChunks();

            dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();

        }
    }

    private void processUnstartedChunks(DumpFileDescriptor dumpFileDescriptor) {
        boolean saved = false;
        List<ProcessingChunk> availableProcessingChunks;
        while (!(availableProcessingChunks = availableProcessingChunks(dumpFileDescriptor)).isEmpty()) {

            logger.info(
                    "Remaining processing chunks to be processed [count={}] [indexes={}]",
                    availableProcessingChunks.size(),
                    availableProcessingChunks.stream().map(pc -> pc.getIndex()).collect(Collectors.toList())
            );

            int randomInt = ThreadLocalRandom.current().nextInt(availableProcessingChunks.size());
            ProcessingChunk availableProcessingChunk = availableProcessingChunks.get(randomInt);
            int availableChunkIndex = availableProcessingChunk.getIndex();
            logger.info("Processing a chunk of dump file [chunk #={}]", availableChunkIndex);
            availableProcessingChunk.setProcessing(true);
            saved = false;
            while (!saved) {
                try {
                    dumpFileDescriptor = dumpFileDescriptorRepository.save(dumpFileDescriptor);
                    logger.info("Marked a chunk of dump file as processing [chunk #={}]", availableChunkIndex);
                    saved = true;
                } catch (final OptimisticLockingFailureException olfe) {
                    logger.info("Tried to mark a chunk of dump file as processing but DumpFileDescriptor is stale, retrying [chunk #={}]", availableChunkIndex);
                    dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
                    availableProcessingChunks.get(availableChunkIndex).setProcessing(true);
                }
            }

            fileChunkWorkerFactory.newInstance(availableProcessingChunk, dumpFileDescriptor).run();
            dumpFileDescriptor = dumpFileDescriptorRepository.findById(calculateOptions.getUniqueDumpFileName()).get();
        }

        logger.info("Finished processing new chunks");

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
