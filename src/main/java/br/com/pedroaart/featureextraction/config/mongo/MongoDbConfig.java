package br.com.pedroaart.featureextraction.config.mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoDbConfig {
    @Bean
    public MongoDatabaseFactory mongoConfigure() {
        return new SimpleMongoClientDatabaseFactory("");
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoConfigure());
    }
}
