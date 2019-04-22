package com.paulturner.wikiwordcount.io;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.collections.CircularByteArrayQueue;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.domain.Subchunk;
import com.paulturner.wikiwordcount.domain.Subchunks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DumpFileChunks {

    private static final byte[] BYTES_CLOSE_PAGE = "</page>".getBytes(StandardCharsets.US_ASCII);
    private static final Logger logger = LoggerFactory.getLogger(DumpFileChunks.class);

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private final CalculateOptions calculateOptions;
    private final ByteBufferPool byteBufferPool;
    private final Path path;

    @Autowired
    public DumpFileChunks(CalculateOptions calculateOptions) {
        this.calculateOptions = calculateOptions;
        this.path = calculateOptions.getFile().toPath();
        byteBufferPool = new ByteBufferPool(calculateOptions.getChunkSize(), calculateOptions.isUseOffHelpBuffers());
    }


    public List<ProcessingChunk> divideToProcessingChunks() {

        int index = 0;

        long position = 0;
        List<ProcessingChunk> processingChunks = new ArrayList<>();
        while (position < calculateOptions.getFileSize() - 1) {
            long start = position;
            position = position + calculateOptions.getChunkSize();
            ByteBuffer probeBuffer = readProbeChunk(position);
            probeBuffer.flip();
            int delta = advanceToNewPage(probeBuffer);
            position = position + delta;
            final ProcessingChunk processingChunk = new ProcessingChunk(start, position, index);

            logger.info("Divided file into chunk [ProcessingChunk={}]", processingChunk);

            processingChunks.add(processingChunk);

            position++;
            index++;

        }
        return processingChunks;

    }


    public Subchunks splitToSubChunks(final ByteBuffer byteBuffer, final ProcessingChunk processingChunk) {

        final List<Subchunk> subchunkList = new ArrayList<>();

        readProcessingChunk(byteBuffer, processingChunk.getStart());
        byteBuffer.flip();
        while (byteBuffer.hasRemaining()) {
            int actualSubchunkSize;
            int approximateSubchunkSize = Math.min(calculateOptions.getSubchunkSize(), byteBuffer.remaining());
            if (approximateSubchunkSize == calculateOptions.getSubchunkSize()) {
                byteBuffer.mark();
                byteBuffer.position(byteBuffer.position() + approximateSubchunkSize);
                actualSubchunkSize = approximateSubchunkSize + advanceToNewPage(byteBuffer);
                byteBuffer.reset();
            } else {
                actualSubchunkSize = approximateSubchunkSize;
            }

            ByteBuffer slice = byteBuffer.slice();
            slice.limit(actualSubchunkSize);

            slice.rewind();

            subchunkList.add(new Subchunk(byteBuffer.position(), byteBuffer.position() + actualSubchunkSize, slice));
            byteBuffer.position(byteBuffer.position() + Math.min(actualSubchunkSize + 1, byteBuffer.remaining()));

        }

        return new Subchunks(subchunkList);
    }


    public int advanceToNewPage(final ByteBuffer byteBuffer) {
        int startPosition = byteBuffer.position();
        CircularByteArrayQueue queue = new CircularByteArrayQueue(BYTES_CLOSE_PAGE.length);
        while (byteBuffer.hasRemaining()) {
            queue.offer(byteBuffer.get());
            if (queue.contains(BYTES_CLOSE_PAGE)) {
                break;
            }
        }
        return byteBuffer.position() - startPosition;
    }


    private ByteBuffer readProbeChunk(long position) {

        logger.info("Find next processing chunk via probe chunk [start position={}]", position);

        SeekableByteChannel seekableByteChannel = null;
        try {
            seekableByteChannel = Files
                    .newByteChannel(this.path, EnumSet.of(StandardOpenOption.READ))
                    .position(position);

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
            seekableByteChannel.read(byteBuffer);
            return byteBuffer;
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        } finally {
            try {
                if (Objects.nonNull(seekableByteChannel)) {
                    seekableByteChannel.close();
                }
            } catch (final IOException ioe) {
                //it's not autoclosable...
            }
        }
    }


    public ByteBuffer readProcessingChunk(final ByteBuffer byteBuffer, long position) {
        SeekableByteChannel seekableByteChannel = null;
        try {
            seekableByteChannel = Files
                    .newByteChannel(calculateOptions.getFile().toPath(), EnumSet.of(StandardOpenOption.READ))
                    .position(position);

            seekableByteChannel.read(byteBuffer);
            return byteBuffer;
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        } finally {
            try {
                if (Objects.nonNull(seekableByteChannel)) {
                    seekableByteChannel.close();
                }
            } catch (final IOException ioe) {
                //it's not autoclosable...
            }
        }
    }


    public ByteBuffer acquireProcessingByteBuffer() {
        return byteBufferPool.acquire();
    }

    public void releaseProcessingByteBuffer(final ByteBuffer byteBuffer) {
        byteBufferPool.release(byteBuffer);
    }
}
