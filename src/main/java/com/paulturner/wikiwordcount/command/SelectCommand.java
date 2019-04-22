package com.paulturner.wikiwordcount.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulturner.wikiwordcount.cli.SelectOptions;
import com.paulturner.wikiwordcount.domain.ChunkDigestAccumulator;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.mongo.ChunkDigestRepository;
import com.paulturner.wikiwordcount.mongo.DumpFileDescriptorRepository;
import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;
import com.paulturner.wikiwordcount.mongoentity.DumpFileDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.paulturner.wikiwordcount.cli.SelectOptions.Direction.DESC;

@Component
public class SelectCommand {

    private static final Logger logger = LoggerFactory.getLogger(SelectCommand.class);

    private DumpFileDescriptorRepository dumpFileDescriptorRepository;
    private ChunkDigestRepository chunkDigestRepository;
    private SelectOptions selectOptions;

    @Autowired
    public SelectCommand(
        DumpFileDescriptorRepository dumpFileDescriptorRepository,
        ChunkDigestRepository chunkDigestRepository, SelectOptions selectOptions
    ) {
        this.dumpFileDescriptorRepository = dumpFileDescriptorRepository;
        this.chunkDigestRepository = chunkDigestRepository;
        this.selectOptions = selectOptions;
    }

    public void processChunks() {

        final ChunkDigestAccumulator digestAccumulator = new ChunkDigestAccumulator();

        Optional<DumpFileDescriptor> dumpFileDescriptorOpt = dumpFileDescriptorRepository
                .findById(selectOptions.getUniqueDumpFileName());

        dumpFileDescriptorOpt.ifPresent(dfd -> selectAndMerge(dfd, digestAccumulator));


        dumpFileDescriptorOpt
                .orElseThrow(() -> new IllegalArgumentException(String.format("Could not find word counts for that file [file={%s]", selectOptions.getUniqueDumpFileName())));

        Map<String, Integer> resultsMap = sortAndFilterTrimToSize(digestAccumulator);
        try {
            String wordCountJson = wordCountJson = new ObjectMapper().writeValueAsString(resultsMap);
            logger.info(wordCountJson);
        } catch (final JsonProcessingException e) {
            logger.error("Couldn't convert results to json", e);
            logger.info(resultsMap.toString());
        }


    }

    private void selectAndMerge(DumpFileDescriptor dumpFileDescriptor, ChunkDigestAccumulator digestAccumulator ) {

        final List<ProcessingChunk> processingChunks = dumpFileDescriptor.getProcessingChunks();
        List<CompletableFuture> futures = new ArrayList<>(dumpFileDescriptor.getProcessingChunks().size());
        Set<Integer> missingChunkDigests = IntStream.range(0, processingChunks.size()).mapToObj(Integer::valueOf).collect(Collectors.toSet());
        for (ProcessingChunk processingChunk : processingChunks) {
            int index = processingChunk.getIndex();
            String chunkKey = ChunkDigest.generatePrimaryKey(index, selectOptions.getUniqueDumpFileName());

            futures.add(CompletableFuture.runAsync(() -> {
                Optional<ChunkDigest> chunkDigestOptional =  chunkDigestRepository.findById(chunkKey);
                chunkDigestOptional.ifPresent(cd -> {
                    missingChunkDigests.remove(index);
                    digestAccumulator.addChunkDigest(chunkDigestOptional.get());
                });
            }));

        }

        if (!missingChunkDigests.isEmpty()) {
            logger.warn(
                    "The word count extraction for this file has not completed yet, showing word counts done uptill now [file={}], [missing chunks={}]",
                    selectOptions.getUniqueDumpFileName(),
                    missingChunkDigests
            );
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
    }

    private Map<String, Integer> sortAndFilterTrimToSize(ChunkDigestAccumulator chunkDigestAccumulator) {

        final LinkedHashMap<String, Integer> sortedAndFiltered = chunkDigestAccumulator.getAccumulated(selectOptions.getUniqueDumpFileName()).getWordCountMap()
            .entrySet()
            .stream()
            .filter(getWordLengthPredicate(selectOptions))
            .sorted(getComparator(selectOptions))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        return sortedAndFiltered
                .entrySet()
                .stream()
                .limit(selectOptions.getCount())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

    }

    private Comparator<Map.Entry<String, Integer>> getComparator(SelectOptions selectOptions) {
        if (DESC == selectOptions.getDirection()) {
            return Collections.reverseOrder(Map.Entry.comparingByValue());
        } else {
            return Map.Entry.comparingByValue();
        }
    }

    private Predicate<Map.Entry<String, Integer>> getWordLengthPredicate(SelectOptions selectOptions) {
        if (selectOptions.getWordLength() == -1) {
            return e -> true;
        } else {
            return e -> e.getKey().length() == selectOptions.getWordLength();
        }
    }


}
