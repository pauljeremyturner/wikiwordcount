package com.paulturner.wikiwordcount.command;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import com.paulturner.wikiwordcount.io.DumpFileChunks;
import com.paulturner.wikiwordcount.mongo.ChunkDigestRepository;
import com.paulturner.wikiwordcount.mongo.DumpFileDescriptorRepository;
import com.paulturner.wikiwordcount.mongoentity.DumpFileDescriptor;
import org.springframework.stereotype.Component;

@Component
public class FileChunkWorkerFactory {

    private final CalculateOptions calculateOptions;
    private final ChunkDigestRepository chunkDigestRepository;
    private final DumpFileDescriptorRepository dumpFileDescriptorRepository;
    private final DumpFileChunks dumpFileChunks;

    public FileChunkWorkerFactory(CalculateOptions calculateOptions, ChunkDigestRepository chunkDigestRepository,
                                  DumpFileDescriptorRepository dumpFileDescriptorRepository, DumpFileChunks dumpFileChunks
    ) {
        this.calculateOptions = calculateOptions;
        this.chunkDigestRepository = chunkDigestRepository;
        this.dumpFileDescriptorRepository = dumpFileDescriptorRepository;
        this.dumpFileChunks = dumpFileChunks;
    }

    public FileChunkWorker newInstance(final ProcessingChunk reservedChunk, final DumpFileDescriptor dumpFileDescriptor) {
        return new FileChunkWorker(calculateOptions, dumpFileChunks, chunkDigestRepository, reservedChunk, dumpFileDescriptorRepository, dumpFileDescriptor);
    }


}
