package com.paulturner.wikiwordcount.io;

import com.paulturner.wikiwordcount.cli.RuntimeOptions;
import com.paulturner.wikiwordcount.collections.CircularByteArrayQueue;
import com.paulturner.wikiwordcount.domain.FileChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunkWithBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Component
public class FileChunkSizer {

    static final byte[] BYTES_CLOSE_PAGE = "</page>" .getBytes(StandardCharsets.US_ASCII);

    private final File file;
    private final int chunkSize;
    private final int subchunkSize;
    private final long fileSizeBytes;

    @Autowired
    public FileChunkSizer(RuntimeOptions runtimeOptions) {
        this.file = runtimeOptions.getFile();
        this.fileSizeBytes = runtimeOptions.getFileSize();
        this.chunkSize = runtimeOptions.getChunkSize();
        this.subchunkSize = runtimeOptions.getSubchunkSize();
    }

    /*
    Implementation note: no need to read the whole 2GB file chunk to determine where to chop to provide whole lines.
    Use a byte buffer here for start and end of the chunk and get the nearest chunk with whole lines.
     */
    public ProcessingChunk trimToWholePages(FileChunk fileChunk) {

        long startPosition, endPosition;


        if (!fileChunk.isEndBound()) {
            ByteBuffer byteBuffer = readProbeChunk(fileChunk.getEnd());
            byteBuffer.flip();
            endPosition = fileChunk.getEnd() + advanceToNewPage(byteBuffer);
        } else {
            endPosition = fileChunk.getEnd();
        }

        if (!fileChunk.isStartBound()) {

            ByteBuffer byteBuffer = readProbeChunk(fileChunk.getStart());
            byteBuffer.flip();
            startPosition = fileChunk.getStart() + advanceToNewPage(byteBuffer);
        } else {
            startPosition = fileChunk.getStart();
        }


        ProcessingChunk processingChunk = new ProcessingChunk(startPosition, endPosition, System.currentTimeMillis());

        System.out.println(processingChunk);
        return processingChunk;

    }

    public List<ProcessingChunkWithBuffer> splitToSubChunks(ProcessingChunk processingChunk) {

        List<ProcessingChunkWithBuffer> subchunks = new ArrayList<>();

        ByteBuffer byteBuffer = readProcessingChunk(processingChunk.getStart());
        byteBuffer.flip();
        int start;
        while (byteBuffer.hasRemaining()) {
            start = byteBuffer.position();
            int actualSubchunkSize;
            int approximateSubchunkSize = Math.min(subchunkSize, byteBuffer.remaining());
            if (approximateSubchunkSize == subchunkSize) {
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

            subchunks.add(new ProcessingChunkWithBuffer(byteBuffer.position(), byteBuffer.position() + actualSubchunkSize, slice));
            byteBuffer.position(byteBuffer.position() + Math.min(actualSubchunkSize + 1, byteBuffer.remaining()));

        }

        return subchunks;


    }


    private int advanceToNewPage(ByteBuffer byteBuffer) {
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
                    .newByteChannel(file.toPath(), EnumSet.of(StandardOpenOption.READ))
                    .position(position);

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 4);
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


    private ByteBuffer readProcessingChunk(long position) {
        SeekableByteChannel seekableByteChannel = null;
        try {
            seekableByteChannel = Files
                    .newByteChannel(file.toPath(), EnumSet.of(StandardOpenOption.READ))
                    .position(position);

            ByteBuffer byteBuffer = ByteBuffer.allocate(chunkSize);
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


    private ByteBuffer readChunk(long position) {
        SeekableByteChannel seekableByteChannel = null;
        try {
            seekableByteChannel = Files
                    .newByteChannel(file.toPath(), EnumSet.of(StandardOpenOption.READ))
                    .position(position);

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 8);
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

}
