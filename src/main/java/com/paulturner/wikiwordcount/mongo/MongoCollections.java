package com.paulturner.wikiwordcount.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.paulturner.wikiwordcount.cli.RuntimeOptions;
import com.paulturner.wikiwordcount.domain.ChunkDigest;
import com.paulturner.wikiwordcount.domain.ProcessingChunk;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Component
public class MongoCollections {

    MongoClient mongoClient;
    private RuntimeOptions runtimeOptions;
    private MongoCollection<ProcessingChunk> chunkPositionCollection;
    private MongoCollection<ChunkDigest> chunkDigestCollection;

    @Autowired
    public MongoCollections(RuntimeOptions runtimeOptions) {
        this.runtimeOptions = runtimeOptions;
    }

    @PostConstruct
    void initialise() {

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));


        MongoClientSettings settings = MongoClientSettings.builder()
                .codecRegistry(pojoCodecRegistry)
                .applyToClusterSettings(builder ->
                        builder.hosts(Arrays.asList(new ServerAddress(runtimeOptions.getMongoClientUri()))))
                .build();
        com.mongodb.client.MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("wikiwordcount");
        chunkPositionCollection = database.getCollection("chunkPosition", ProcessingChunk.class);
        chunkDigestCollection = database.getCollection("chunkDigest", ChunkDigest.class);

    }


    public List<ProcessingChunk> readAllChunkPositions() {

        try {

            List<ProcessingChunk> results = new ArrayList<>();
            chunkPositionCollection.find().iterator().forEachRemaining(cp -> results.add(cp));
            System.out.println(results);
            return results;
        } catch (MongoCommandException e) {
            throw e;
        } finally {
            System.out.println("####################################\n");
        }
    }

    public void addReservedChunk(final ProcessingChunk processingChunk) {
        try {
            chunkPositionCollection.insertOne(processingChunk);
        } catch (MongoCommandException e) {
            System.out.println("####### ROLLBACK TRANSACTION #######");
        } finally {
            System.out.println("####################################\n");
        }
    }


    public void addChunkDigest(ChunkDigest chunkDigest) {
        chunkDigestCollection.insertOne(chunkDigest);
    }

    public void completeChunk(ProcessingChunk processingChunk) {

    }
}
