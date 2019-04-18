package com.paulturner.wikiwordcount.io;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.domain.FileChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DumpFileChunksTest {

    private static final int KILO = 1 << 10;
    private static final int CHUNK_SIZE = 512 * KILO;

    private DumpFileChunks dumpFileChunks;
    CalculateOptions calculateOptions;

    @Before
    public void before() throws Exception {

        File projectRootDir = new File(".");
        String testFilePath = projectRootDir.getAbsolutePath() + "<SEP>src<SEP>test<SEP>resources<SEP>quite-a-few-pages.xml".replace("<SEP>", File.separator);
        calculateOptions = new CalculateOptions.Builder()
                .withChunkSize(CHUNK_SIZE)
                .withFile(new File(testFilePath))
                .build();
        dumpFileChunks = new DumpFileChunks(calculateOptions);

    }


    @Test
    public void shouldFindUnreservedChunkAtStart() throws Exception {

        int start = 1024 * KILO;
        int end = (int) calculateOptions.getFileSize() - 1;

        ProcessingChunk processingChunk = new ProcessingChunk(start, end, System.currentTimeMillis(), calculateOptions.getUniqueDumpFileName());

        FileChunk anyAvailableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(Arrays.asList(processingChunk));
        boolean testedRandomChunkInMiddleOfFreeSpace = false;
        boolean testedRandomChunkAtEndOfFreeSpace = false;

        while (!testedRandomChunkAtEndOfFreeSpace && !testedRandomChunkInMiddleOfFreeSpace) {
            // we got a random chunk bounded by the start of the already reserved chunk
            if (anyAvailableFileChunk.getEnd() == start - 1) {
                assertThat(anyAvailableFileChunk.getStart()).isGreaterThan(0);
                assertThat(anyAvailableFileChunk.getStart()).isLessThan(start - 1);
                assertThat(anyAvailableFileChunk.getStart()).isLessThan(anyAvailableFileChunk.getEnd());
                assertThat(anyAvailableFileChunk.getEnd() - anyAvailableFileChunk.getStart()).isLessThanOrEqualTo(CHUNK_SIZE);
                assertThat(anyAvailableFileChunk.isEndBound()).isTrue();
                assertThat(anyAvailableFileChunk.isStartBound()).isFalse();
                testedRandomChunkAtEndOfFreeSpace = true;

            } else {
                assertThat(anyAvailableFileChunk.getEnd() - anyAvailableFileChunk.getStart()).isEqualTo(CHUNK_SIZE);
                assertThat(anyAvailableFileChunk.getStart()).isLessThan(anyAvailableFileChunk.getEnd());
                testedRandomChunkInMiddleOfFreeSpace = true;

            }
        }

    }


    @Test
    public void shouldFindUnreservedChunkAtEnd() throws Exception {

        int start = 0;
        int end = 2048 * KILO;

        ProcessingChunk processingChunk = new ProcessingChunk(start, end, System.currentTimeMillis(), calculateOptions.getUniqueDumpFileName());

        FileChunk anyAvailableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(Arrays.asList(processingChunk));
        boolean testedRandomChunkInMiddleOfFreeSpace = false;
        boolean testedRandomChunkAtEndOfFreeSpace = false;

        while (!testedRandomChunkAtEndOfFreeSpace && !testedRandomChunkInMiddleOfFreeSpace) {
            // we got a random chunk bounded by the end of the file
            if (anyAvailableFileChunk.getEnd() == calculateOptions.getFileSize() - 1) {
                assertThat(anyAvailableFileChunk.getStart()).isLessThan(calculateOptions.getFileSize() - 1);
                assertThat(anyAvailableFileChunk.getStart()).isGreaterThan(calculateOptions.getFileSize() - 1 - CHUNK_SIZE);
                assertThat(anyAvailableFileChunk.getEnd() - anyAvailableFileChunk.getStart()).isLessThanOrEqualTo(CHUNK_SIZE);
                assertThat(anyAvailableFileChunk.isEndBound()).isTrue();
                assertThat(anyAvailableFileChunk.isStartBound()).isFalse();
                testedRandomChunkAtEndOfFreeSpace = true;

            } else {
                assertThat(anyAvailableFileChunk.getEnd() - anyAvailableFileChunk.getStart()).isEqualTo(CHUNK_SIZE);
                assertThat(anyAvailableFileChunk.getStart()).isLessThan(anyAvailableFileChunk.getEnd());
                testedRandomChunkInMiddleOfFreeSpace = true;

            }
        }

    }


    @Test
    public void shouldFindUnreservedChunkWhenMiddleReserved() throws Exception {

        int start = 2048 * KILO;
        int end = 3072 * KILO;

        ProcessingChunk reserved1 = new ProcessingChunk(start, end - 1024, System.currentTimeMillis(), calculateOptions.getUniqueDumpFileName());
        ProcessingChunk reserved2 = new ProcessingChunk(end - 1023, end, System.currentTimeMillis(), calculateOptions.getUniqueDumpFileName());

        FileChunk anyAvailableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(Arrays.asList(reserved1, reserved2));
        boolean testedRandomChunkAtEndOfFreeSpace = false;
        boolean testedRandomChunkAtStartOfFreeSpace = false;

        while (!testedRandomChunkAtEndOfFreeSpace && testedRandomChunkAtStartOfFreeSpace) {

            if (anyAvailableFileChunk.getEnd() == calculateOptions.getFileSize() - 1) {
                assertThat(anyAvailableFileChunk.getStart()).isLessThan(calculateOptions.getFileSize() - 1);
                assertThat(anyAvailableFileChunk.getStart()).isGreaterThan(calculateOptions.getFileSize() - 1 - CHUNK_SIZE);
                assertThat(anyAvailableFileChunk.getEnd() - anyAvailableFileChunk.getStart()).isLessThanOrEqualTo(CHUNK_SIZE);
                assertThat(anyAvailableFileChunk.getStart()).isGreaterThan(reserved2.getEnd());
                testedRandomChunkAtEndOfFreeSpace = true;

            }
            if (anyAvailableFileChunk.getStart() == 0) {
                assertThat(anyAvailableFileChunk.getEnd()).isLessThan(CHUNK_SIZE + 1);
                assertThat(anyAvailableFileChunk.getStart()).isLessThan(anyAvailableFileChunk.getEnd());
                assertThat(anyAvailableFileChunk.getEnd() - anyAvailableFileChunk.getStart()).isLessThanOrEqualTo(CHUNK_SIZE);
                testedRandomChunkAtStartOfFreeSpace = true;

            }

        }
    }

    @Test
    public void noMoreChunksWhenAllDoneSimple() {
        ProcessingChunk pc1 = new ProcessingChunk(0, 3330000, System.currentTimeMillis(), "");
        ProcessingChunk pc2 = new ProcessingChunk(3330001, 7916025, System.currentTimeMillis(), "");

        List<ProcessingChunk> processingChunks = Arrays.asList(pc1, pc2);

        FileChunk anyAvailableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(processingChunks);

    }

    @Test
    public void chunkWhenOnlySpaceAtStart() {
        ProcessingChunk pc1 = new ProcessingChunk(1000, 3330000, System.currentTimeMillis(), "");
        ProcessingChunk pc2 = new ProcessingChunk(3330001, 7916025, System.currentTimeMillis(), "");


        List<ProcessingChunk> processingChunks = Arrays.asList(pc1, pc2);

        FileChunk anyAvailableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(processingChunks);

        assertThat(anyAvailableFileChunk.isStartBound()).isTrue();
        assertThat(anyAvailableFileChunk.isEndBound()).isTrue();
        assertThat(anyAvailableFileChunk.getStart()).isEqualTo(0);
        assertThat(anyAvailableFileChunk.getEnd()).isEqualTo(999);

    }


    @Test
    public void chunkWhenOnlySpaceAtEnd() {
        ProcessingChunk pc1 = new ProcessingChunk(0, 3330000, System.currentTimeMillis(), "");
        ProcessingChunk pc2 = new ProcessingChunk(3330001, 7910000, System.currentTimeMillis(), "");

        List<ProcessingChunk> processingChunks = new ArrayList(Arrays.asList(pc1, pc2));

        FileChunk anyAvailableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(processingChunks);

        assertThat(anyAvailableFileChunk.isStartBound()).isTrue();
        assertThat(anyAvailableFileChunk.isEndBound()).isTrue();
        assertThat(anyAvailableFileChunk.getStart()).isEqualTo(7910001);
        assertThat(anyAvailableFileChunk.getEnd()).isEqualTo(7916025);

        ProcessingChunk processingChunk = dumpFileChunks.trimToWholePages(anyAvailableFileChunk);

        processingChunks.add(processingChunk);

        anyAvailableFileChunk = dumpFileChunks.getAnyAvailableFileChunk(processingChunks);

        assertThat(anyAvailableFileChunk).isNull();
    }



}