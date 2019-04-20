package com.paulturner.wikiwordcount.command;

import com.paulturner.wikiwordcount.cli.SelectOptions;
import com.paulturner.wikiwordcount.domain.ChunkDigestAccumulator;
import com.paulturner.wikiwordcount.mongo.ChunkDigestRepository;
import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.paulturner.wikiwordcount.cli.SelectOptions.Direction.DESC;

@Component
public class SelectProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SelectProcessor.class);

    private ChunkDigestRepository chunkDigestRepository;
    private SelectOptions selectOptions;

    @Autowired
    public SelectProcessor(ChunkDigestRepository chunkDigestRepository, SelectOptions selectOptions) {
        this.chunkDigestRepository = chunkDigestRepository;
        this.selectOptions = selectOptions;
    }

    public void processChunks() {

        List<ChunkDigest> chunkDigestList = chunkDigestRepository.findAll();

        final ChunkDigestAccumulator digestAccumulator = new ChunkDigestAccumulator();
        chunkDigestList.stream().parallel().forEach(
                cd -> digestAccumulator.addChunkDigest(cd)
        );

        final StringBuilder stringBuilder = new StringBuilder().append(System.lineSeparator()).append(selectOptions.toString()).append(System.lineSeparator());


        LinkedHashMap<String, Integer> sortedAndFiltered = digestAccumulator.getAccumulated(selectOptions.getUniqueDumpFileName()).getWordCountMap()
                .entrySet()
                .stream()
                .filter(getWordLengthPredicate(selectOptions))
                .sorted(getComparator(selectOptions))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));

        int limit = selectOptions.getCount();
        Iterator<Map.Entry<String, Integer>> iterator = sortedAndFiltered.entrySet().iterator();
        IntStream.range(0, limit).forEach(
                i -> {
                    Map.Entry<String, Integer> entry = iterator.next();
                    stringBuilder.append("word=").append(entry.getKey()).append(", count=").append(entry.getValue()).append(System.lineSeparator());
                }
        );

        logger.info(stringBuilder.toString());

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
