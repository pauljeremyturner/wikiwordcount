package com.paulturner.wikiwordcount;

import com.paulturner.wikiwordcount.cli.RuntimeOptions;
import com.paulturner.wikiwordcount.io.FileChunkSizer;
import com.paulturner.wikiwordcount.io.FileChunker;
import com.paulturner.wikiwordcount.mongo.MongoCollections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FileProcessor {

    private RuntimeOptions runtimeOptions;
    private FileChunker fileChunker;
    private FileChunkSizer fileChunkSizer;
    private MongoCollections mongoCollections;




    @Autowired
    public FileProcessor(
            RuntimeOptions runtimeOptions,
            FileChunkSizer fileChunkSizer,
            MongoCollections mongoCollections,
            FileChunker fileChunker
    ) {

        this.fileChunkSizer = fileChunkSizer;
        this.runtimeOptions = runtimeOptions;
        this.mongoCollections = mongoCollections;
        this.fileChunker = fileChunker;
    }

    public void process() {

        new FileChunkProcessor(fileChunkSizer, mongoCollections, fileChunker).run();
    }


}
