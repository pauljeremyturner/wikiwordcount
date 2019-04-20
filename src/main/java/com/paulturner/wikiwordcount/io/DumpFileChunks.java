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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DumpFileChunks {

    private static final byte[] BYTES_CLOSE_PAGE = "</page>".getBytes(StandardCharsets.US_ASCII);
    private static final Logger logger = LoggerFactory.getLogger(DumpFileChunks.class);

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private final CalculateOptions calculateOptions;
    private final ByteBufferPool byteBufferPool;
    private final long fileSize;
    private final String uniqueFilename;
    private final Path path;

    @Autowired
    public DumpFileChunks(CalculateOptions calculateOptions) {
        this.calculateOptions = calculateOptions;
        this.uniqueFilename = calculateOptions.getUniqueDumpFileName();
        this.fileSize = calculateOptions.getFileSize();
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
            processingChunks.add(new ProcessingChunk(start, position, -1, index));

            position++;
            index++;

        }
        return processingChunks;

    }

    /*

        public FileChunk getAnyAvailableFileChunk(final List<ProcessingChunk> reservedProcessingChunks) {

            final List<ProcessingChunk> unreservedProcessingChunks = reservedToUnreserved(reservedProcessingChunks);

            if (unreservedProcessingChunks.isEmpty()) {
                return null;
            }


            ProcessingChunk processingChunk = unreservedProcessingChunks.get(0);
            long start = processingChunk.getStart();
            long end;
            boolean endBound;
            if ((processingChunk.getEnd() - processingChunk.getStart()) > calculateOptions.getChunkSize()) {
                end = start + calculateOptions.getChunkSize();
                endBound = false;

            } else {
                end = processingChunk.getEnd();
                endBound = true;
            }

            return new FileChunk(start, end, true, endBound);

        }
    */
    public boolean fileComplete(final List<ProcessingChunk> reservedProcessingChunks) {


        Collections.sort(reservedProcessingChunks);
        List<ProcessingChunk> reservedChunks = reservedProcessingChunks;

        if (reservedChunks.isEmpty()) {
            return false;
        }

        if (reservedChunks.size() == 1) {
            ProcessingChunk singleChunk = reservedProcessingChunks.get(0);
            return (singleChunk.getStart() == 0) && (singleChunk.getEnd() == calculateOptions.getFileSize() - 1);
        }

        Iterator<ProcessingChunk> chunkIterator = reservedProcessingChunks.iterator();
        ProcessingChunk previous = chunkIterator.next();
        if (previous.getStart() != 0) {
            return false;
        } else {
            while (chunkIterator.hasNext()) {
                ProcessingChunk next = chunkIterator.next();
                if ((previous.getEnd() + 1) != next.getStart()) {
                    return false;
                }
                previous = next;
            }
            return (previous.getStart() == 0) && (previous.getEnd() == calculateOptions.getFileSize() - 1);
        }


    }

/*
    private List<ProcessingChunk> reservedToUnreserved(List<ProcessingChunk> reserved) {

        List<ProcessingChunk> unreserved = new ArrayList<>();

        Collections.sort(reserved);

        Iterator<ProcessingChunk> iterator = reserved.iterator();

        if (reserved.isEmpty()) {
            return Arrays.asList(new ProcessingChunk(0, fileSize - 1, System.currentTimeMillis(), uniqueFilename));
        }


        ProcessingChunk cp1;
        ProcessingChunk cp2 = iterator.next();
        while (iterator.hasNext()) {
            cp1 = cp2;
            cp2 = iterator.next();
            cp2.getStart();
            boolean contiguous = (cp1.getEnd() + 1) == cp2.getStart();
            if (!contiguous) {
                ProcessingChunk freeSpace = new ProcessingChunk((cp1.getEnd() + 1), (cp2.getStart() - 1), System.currentTimeMillis(), uniqueFilename);
                unreserved.add(freeSpace);
            }
        }

        ProcessingChunk first = reserved.get(0);
        if (first.getStart() > 0) {
            unreserved.add(new ProcessingChunk(0, first.getStart() - 1, System.currentTimeMillis(), calculateOptions.getUniqueDumpFileName()));
        }

        ProcessingChunk last = reserved.get(reserved.size() - 1);
        if (last.getEnd() < fileSize - 1) {
            unreserved.add(new ProcessingChunk(last.getEnd() + 1, fileSize - 1, System.currentTimeMillis(), calculateOptions.getUniqueDumpFileName()));
        }


        return unreserved;

    }
*/


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


    public int advanceToNewPage(ByteBuffer byteBuffer) {
        int startPosition = byteBuffer.position();
        CircularByteArrayQueue queue = new CircularByteArrayQueue(BYTES_CLOSE_PAGE.length);
        while (byteBuffer.hasRemaining()) {
            queue.offer(byteBuffer.get());
            if (queue.containsArray(BYTES_CLOSE_PAGE)) {
                break;
            }
        }
        return byteBuffer.position() - startPosition;
    }


    private ByteBuffer readProbeChunk(long position) {

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


    public ByteBuffer readProcessingChunk(ByteBuffer byteBuffer, long position) {
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


    public ByteBuffer acquireByteBuffer() {
        return byteBufferPool.acquire();
    }

    public void releaseByteBuffer(final ByteBuffer byteBuffer) {
        byteBufferPool.release(byteBuffer);
    }
}
