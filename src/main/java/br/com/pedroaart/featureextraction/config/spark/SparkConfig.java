package br.com.pedroaart.featureextraction.config.spark;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SparkConfig {

    @Bean
    @Primary
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("MobilityAnalysisApp")
                .config("spark.mongodb.read.connection.uri",
                        "mongodb+srv://pedroaart:Pimentas1%23@feature-extraction.x2phx.mongodb.net/feature-extraction.measurements?retryWrites=true&w=majority&appName=feature-extraction")
                .config("spark.mongodb.write.connection.uri",
                        "mongodb+srv://pedroaart:Pimentas1%23@feature-extraction.x2phx.mongodb.net/feature-extraction.mobility_analysis?retryWrites=true&w=majority&appName=feature-extraction")
                .config("spark.sql.adaptive.enabled", "true")
                .master("local[*]")
                .getOrCreate();
    }
}