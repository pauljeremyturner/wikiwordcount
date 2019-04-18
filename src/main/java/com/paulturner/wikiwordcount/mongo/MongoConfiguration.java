package com.paulturner.wikiwordcount.mongo;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"com.paulturner.wikiwordcount.mongo"})
public class MongoConfiguration extends AbstractMongoConfiguration {

    @Autowired
    private ServerAddress serverAddress;

    @Override
    protected String getDatabaseName() {
        return "wikiwordcount";
    }


    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    @Override
    @Bean
    public MongoClient mongoClient() {

        MongoClient client = new MongoClient(serverAddress);
        client.setWriteConcern(WriteConcern.ACKNOWLEDGED);

        return client;
    }
}