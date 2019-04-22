package com.paulturner.wikiwordcount.io;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.test.TestFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DumpFileChunksTest {

    private static final int KILO = 1 << 10;
    private static final int CHUNK_SIZE = 512 * KILO;

    private CalculateOptions calculateOptions;
    private DumpFileChunks dumpFileChunks;

    @Before
    public void before() throws Exception {

        String testFilePath = TestFile.testDumpFilePath();
        calculateOptions = CalculateOptions.builder()
                .withChunkSize(CHUNK_SIZE)
                .withFile(new File(testFilePath))
                .build();
        dumpFileChunks = new DumpFileChunks(calculateOptions);

    }

    @Test
    public void shouldSeparateIntoChunks() throws Exception {
        assertThat(dumpFileChunks.divideToProcessingChunks().size());
    }

    @Test
    public void allProcessingChunksShouldStartAndEndWithNewPage() throws Exception {
        List<ProcessingChunk> processingChunks = dumpFileChunks.divideToProcessingChunks();
        RandomAccessFile randomAccessFile = new RandomAccessFile(calculateOptions.getFile(), "r");
        boolean first = true;
        for (ProcessingChunk pc : processingChunks) {
            try {
                randomAccessFile.seek(pc.getStart());
                int charsRead = 0;
                byte[] bytes = new byte[6];
                if (first) {
                    first = false;
                    continue;
                }
                while (charsRead < 6) {

                    byte b = randomAccessFile.readByte();
                    if (Character.isWhitespace((char) b)) {
                        continue;
                    }
                    bytes[charsRead] = b;
                    charsRead++;

                }
                assertThat(Arrays.equals(bytes, "<page>".getBytes(StandardCharsets.US_ASCII))).isTrue();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Test
    public void processingChunksShouldBeContiguousBytesOfFile() throws Exception {

        List<ProcessingChunk> processingChunks = dumpFileChunks.divideToProcessingChunks();


        Iterator<ProcessingChunk> iterator = processingChunks.iterator();

        ProcessingChunk cp1;
        ProcessingChunk cp2 = iterator.next();
        while (iterator.hasNext()) {
            cp1 = cp2;
            cp2 = iterator.next();
            boolean contiguous = (cp1.getEnd() + 1) == cp2.getStart();
            if (!contiguous) {
                Assert.fail();
            }
        }

        ProcessingChunk first = processingChunks.get(0);
        if (first.getStart() > 0) {
            Assert.fail();
        }

        ProcessingChunk last = processingChunks.get(processingChunks.size() - 1);
        if (last.getEnd() < calculateOptions.getFileSize() - 1) {
            Assert.fail();
        }

    }


}