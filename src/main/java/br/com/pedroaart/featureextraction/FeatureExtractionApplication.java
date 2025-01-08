package br.com.pedroaart.featureextraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoRepositories
@EnableAsync
public class FeatureExtractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeatureExtractionApplication.class, args);
    }

}
