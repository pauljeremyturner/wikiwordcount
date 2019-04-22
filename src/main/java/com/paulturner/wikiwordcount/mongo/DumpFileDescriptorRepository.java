package com.paulturner.wikiwordcount.mongo;

import com.paulturner.wikiwordcount.mongoentity.DumpFileDescriptor;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DumpFileDescriptorRepository extends MongoRepository<DumpFileDescriptor, String> { }