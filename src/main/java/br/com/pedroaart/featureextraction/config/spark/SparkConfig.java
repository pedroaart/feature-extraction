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
              .config("spark.sql.adaptive.enabled", "true")
                .master("local[*]")
                .getOrCreate();
    }
}
