package com.paulturner.wikiwordcount.mongo;

import com.paulturner.wikiwordcount.domain.ChunkDigest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChunkDigestRepository extends MongoRepository<ChunkDigest, String> {


}