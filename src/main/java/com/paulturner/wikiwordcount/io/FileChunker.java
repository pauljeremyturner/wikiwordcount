package com.paulturner.wikiwordcount.io;

import com.paulturner.wikiwordcount.cli.RuntimeOptions;
import com.paulturner.wikiwordcount.domain.FileChunk;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FileChunker {

    private static final Random RANDOM = new Random();
    private long chunkSize;

    /*
    implementation note:
    The FileChunker works with bytes not characters.  This is because xml tags use US-ASCII character set so 1
    character of an xml tag is always 1 byte.  Because the file is spliced according to xml tags with content of
    interest for word counting, the file will never be spliced so that it splits a UTF-8 character in 2 pieces.
     */
    private long fileSize;

    @Autowired
    public FileChunker(RuntimeOptions runtimeOptions) {
        this.fileSize = runtimeOptions.getFileSize();
        this.chunkSize = runtimeOptions.getChunkSize();

    }


    public FileChunk getAvailableFileChunk(final List<ProcessingChunk> reservedProcessingChunks) {

        final List<ProcessingChunk> unreservedProcessingChunks = reservedToUnreserved(reservedProcessingChunks);

        FileChunk availableFileChunk;
        final ProcessingChunk availableSpace = unreservedProcessingChunks.get(RANDOM.nextInt(unreservedProcessingChunks.size()));
        if (availableSpace.getLength() > chunkSize) {
            final int chunksAvailable = (int) ((availableSpace.getEnd() - availableSpace.getStart()) / chunkSize);
            final int randomchunkIndex = RANDOM.nextInt(chunksAvailable);
            final long myChunkStart = randomchunkIndex * chunkSize;
            final long myChunkEnd = Math.min(myChunkStart + chunkSize, availableSpace.getEnd());
            availableFileChunk = new FileChunk(myChunkStart, myChunkEnd, randomchunkIndex == 0, myChunkEnd == availableSpace.getEnd());

        } else {
            availableFileChunk = new FileChunk(
                    availableSpace.getStart(), availableSpace.getEnd(), true, true
            );
        }

        return availableFileChunk;
    }

    private List<ProcessingChunk> reservedToUnreserved(List<ProcessingChunk> reserved) {

        List<ProcessingChunk> unreserved = new ArrayList<>();

        Collections.sort(reserved);

        Iterator<ProcessingChunk> iterator = reserved.iterator();

        if (reserved.isEmpty()) {
            return Arrays.asList(new ProcessingChunk(0, fileSize - 1, 0));
        }


        ProcessingChunk cp1;
        ProcessingChunk cp2 = iterator.next();
        while (iterator.hasNext()) {
            cp1 = cp2;
            cp2 = iterator.next();

            boolean contiguous = (cp1.getEnd() + 1) == cp2.getStart();
            if (!contiguous) {
                ProcessingChunk freeSpace = new ProcessingChunk((cp1.getEnd()), (cp2.getStart() - 1), 0);
                unreserved.add(freeSpace);
            }


        }

        long lastReserved = cp2.getEnd();

        if (lastReserved < fileSize - 1) {
            unreserved.add(new ProcessingChunk(lastReserved + 1, fileSize - 1, 0));
        }

        return unreserved;

    }

}
