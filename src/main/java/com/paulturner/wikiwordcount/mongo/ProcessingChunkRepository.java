package com.paulturner.wikiwordcount.mongo;

import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProcessingChunkRepository extends MongoRepository<ProcessingChunk, String> {

    List<ProcessingChunk> findByProcessed(boolean processed);

}