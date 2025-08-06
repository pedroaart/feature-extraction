package br.com.pedroaart.featureextraction.services.spark;

import br.com.pedroaart.featureextraction.domain.spark.MobilityResult;
import br.com.pedroaart.featureextraction.domain.spark.ValidationResult;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.apache.spark.sql.functions.col;

@Service
public class SparkValidationService {

    @Autowired
    private SparkSession spark;

    public ValidationResult validateMobilityAnalysis(String userId, LocalDateTime startDate, LocalDateTime endDate) {

        // 1. Carregar dados automáticos (COLLECTED_BY_TRACK)
        Dataset<Row> automaticData = loadMeasurements(userId, startDate, endDate, "COLLECTED_BY_TRACK");

        // 2. Carregar dados manuais (CREATED_BY_USER) - Ground Truth
        Dataset<Row> manualData = loadMeasurements(userId, startDate, endDate, "CREATED_BY_USER");

        // 3. Processar dados automáticos com Spark
        MobilityResult automaticResults = processAutomaticData(automaticData);

        // 4. Processar dados manuais (verdade)
        MobilityResult manualResults = processManualData(manualData);

        // 5. Comparar e calcular métricas
        return calculateValidationMetrics(automaticResults, manualResults);
    }

    private Dataset<Row> loadMeasurements(String userId, LocalDateTime start, LocalDateTime end, String measurementType) {
        Dataset<Row> rawData = spark.read()
                .format("mongodb")
                .option("database", "feature-extraction")
                .option("collection", "measurements")
                .load();

        return rawData
                .select(
                        col("_id").alias("id"),
                        col("timestamp"),
                        col("metadata.userId").alias("userId"),
                        col("metadata.measurementTypeEnum").alias("measurementType"),
                        col("metadata.tag").alias("tag"),
                        col("accuracy"),
                        col("location.coordinates").getItem(1).alias("latitude"),
                        col("location.coordinates").getItem(0).alias("longitude")
                )
                .filter(col("userId").equalTo(userId))
                .filter(col("measurementType").equalTo(measurementType))
                .filter(col("timestamp").between(Timestamp.valueOf(start), Timestamp.valueOf(end)))
                .orderBy("timestamp");
    }

    private MobilityResult processManualData(Dataset<Row> manualData) {
        manualData.createOrReplaceTempView("manual_data");

        // Analisar tags manuais para extrair ground truth
        Dataset<Row> manualStayTimes = spark.sql("""
            SELECT userId, tag,
                   MIN(timestamp) as start_time,
                   MAX(timestamp) as end_time,
                   (UNIX_TIMESTAMP(MAX(timestamp)) - UNIX_TIMESTAMP(MIN(timestamp))) / 3600.0 as duration_hours,
                   AVG(latitude) as avg_lat,
                   AVG(longitude) as avg_lon,
                   COUNT(*) as point_count
            FROM manual_data
            WHERE tag IS NOT NULL
            GROUP BY userId, tag
            ORDER BY start_time
            """);

        manualStayTimes.createOrReplaceTempView("manual_stays");

        // Calcular métricas baseadas nas tags manuais
        Dataset<Row> homeTime = spark.sql("""
            SELECT userId, SUM(duration_hours) as total_home_hours
            FROM manual_stays
            WHERE LOWER(tag) LIKE '%casa%' OR LOWER(tag) LIKE '%home%'
            GROUP BY userId
            """);

        Dataset<Row> uniqueLocations = spark.sql("""
            SELECT userId, COUNT(DISTINCT tag) as unique_locations
            FROM manual_stays
            GROUP BY userId
            """);

        Dataset<Row> travelTime = spark.sql("""
            WITH travel_periods AS (
                SELECT userId,
                       LEAD(start_time) OVER (ORDER BY start_time) - end_time as travel_duration_seconds
                FROM manual_stays
                ORDER BY start_time
            )
            SELECT userId, 
                   SUM(travel_duration_seconds) / 3600.0 as total_travel_hours
            FROM travel_periods
            WHERE travel_duration_seconds > 120 -- Mais que 2 minutos
            GROUP BY userId
            """);

        // Converter para MobilityResult
        MobilityResult result = new MobilityResult();
        result.setUserId(manualData.first().getAs("userId"));

        if (homeTime.count() > 0) {
            result.setTotalHomeTimeHours(homeTime.first().getAs("total_home_hours"));
        }

        if (uniqueLocations.count() > 0) {
            result.setUniqueLocationsVisited(uniqueLocations.first().getAs("unique_locations"));
        }

        if (travelTime.count() > 0) {
            result.setTotalTravelTimeHours(travelTime.first().getAs("total_travel_hours"));
        }

        System.out.println("=== DADOS MANUAIS (GROUND TRUTH) ===");
        manualStayTimes.show();
        homeTime.show();
        uniqueLocations.show();
        travelTime.show();

        return result;
    }

    private MobilityResult processAutomaticData(Dataset<Row> automaticData) {
        // Usar sua análise automática existente
        automaticData.createOrReplaceTempView("measurements");

        // Detectar stay points automaticamente
        Dataset<Row> stayPoints = detectStayPoints();
        stayPoints.createOrReplaceTempView("stay_points");

        // Calcular métricas automáticas
        Dataset<Row> autoHomeTime = spark.sql("""
            SELECT userId,
                   SUM(duration_minutes) / 60.0 as total_home_hours
            FROM stay_points
            WHERE HOUR(start_time) >= 20 OR HOUR(end_time) <= 7
            GROUP BY userId
            """);

        Dataset<Row> autoUniqueLocations = spark.sql("""
            SELECT userId, COUNT(*) as unique_locations
            FROM stay_points
            GROUP BY userId
            """);

        Dataset<Row> autoTravelTime = spark.sql("""
            WITH journey_pairs AS (
                SELECT userId,
                       end_time as departure_time,
                       LEAD(start_time) OVER (PARTITION BY userId ORDER BY start_time) as arrival_time
                FROM stay_points
            )
            SELECT userId,
                   SUM((UNIX_TIMESTAMP(arrival_time) - UNIX_TIMESTAMP(departure_time)) / 3600.0) as total_travel_hours
            FROM journey_pairs
            WHERE arrival_time IS NOT NULL 
            AND (UNIX_TIMESTAMP(arrival_time) - UNIX_TIMESTAMP(departure_time)) BETWEEN 120 AND 18000 -- 2min a 5h
            GROUP BY userId
            """);

        // Converter para resultado
        MobilityResult result = new MobilityResult();
        result.setUserId(automaticData.first().getAs("userId"));

        if (autoHomeTime.count() > 0) {
            result.setTotalHomeTimeHours(autoHomeTime.first().getAs("total_home_hours"));
        }

        if (autoUniqueLocations.count() > 0) {
            result.setUniqueLocationsVisited(autoUniqueLocations.first().getAs("unique_locations"));
        }

        if (autoTravelTime.count() > 0) {
            result.setTotalTravelTimeHours(autoTravelTime.first().getAs("total_travel_hours"));
        }

        System.out.println("=== DADOS AUTOMÁTICOS (PREDIÇÕES) ===");
        autoHomeTime.show();
        autoUniqueLocations.show();
        autoTravelTime.show();

        return result;
    }

    private ValidationResult calculateValidationMetrics(MobilityResult automatic, MobilityResult manual) {
        ValidationResult validation = new ValidationResult();
        validation.setUserId(automatic.getUserId());
        validation.setValidationDate(new java.sql.Timestamp(System.currentTimeMillis()));

        // Calcular erro absoluto médio (MAE) para tempo em casa
        if (automatic.getTotalHomeTimeHours() != null && manual.getTotalHomeTimeHours() != null) {
            double homeTimeError = Math.abs(automatic.getTotalHomeTimeHours() - manual.getTotalHomeTimeHours());
            validation.setHomeTimeMAE(homeTimeError);
            validation.setHomeTimeAccuracy(1.0 - (homeTimeError / Math.max(manual.getTotalHomeTimeHours(), 0.1)));
        }

        // Calcular erro para tempo de viagem
        if (automatic.getTotalTravelTimeHours() != null && manual.getTotalTravelTimeHours() != null) {
            double travelTimeError = Math.abs(automatic.getTotalTravelTimeHours() - manual.getTotalTravelTimeHours());
            validation.setTravelTimeMAE(travelTimeError);
            validation.setTravelTimeAccuracy(1.0 - (travelTimeError / Math.max(manual.getTotalTravelTimeHours(), 0.1)));
        }

        // Calcular erro para locais únicos
        if (automatic.getUniqueLocationsVisited() != null && manual.getUniqueLocationsVisited() != null) {
            int locationError = Math.abs(automatic.getUniqueLocationsVisited() - manual.getUniqueLocationsVisited());
            validation.setLocationCountMAE((double) locationError);
            validation.setLocationCountAccuracy(1.0 - (locationError / (double) Math.max(manual.getUniqueLocationsVisited(), 1)));
        }

        // Calcular F1-Score geral
        double avgAccuracy = (validation.getHomeTimeAccuracy() + validation.getTravelTimeAccuracy() + validation.getLocationCountAccuracy()) / 3.0;
        validation.setOverallF1Score(avgAccuracy);

        return validation;
    }

    // Método auxiliar que você já tem
    private Dataset<Row> detectStayPoints() {
        // Sua implementação existente de detecção de stay points
        return spark.sql("""
            WITH ordered_measurements AS (
                SELECT userId, latitude, longitude, timestamp,
                       LAG(latitude) OVER (PARTITION BY userId ORDER BY timestamp) as prev_lat,
                       LAG(longitude) OVER (PARTITION BY userId ORDER BY timestamp) as prev_lon
                FROM measurements
            ),
            with_distance AS (
                SELECT *,
                       CASE 
                           WHEN prev_lat IS NOT NULL THEN
                               6371000 * 2 * ASIN(SQRT(
                                   POWER(SIN(RADIANS(latitude - prev_lat) / 2), 2) +
                                   COS(RADIANS(prev_lat)) * COS(RADIANS(latitude)) *
                                   POWER(SIN(RADIANS(longitude - prev_lon) / 2), 2)
                               ))
                           ELSE 0.0
                       END as distance_meters
                FROM ordered_measurements
            ),
            clustered_points AS (
                SELECT *,
                       SUM(CASE WHEN distance_meters > 100 THEN 1 ELSE 0 END) 
                           OVER (PARTITION BY userId ORDER BY timestamp ROWS UNBOUNDED PRECEDING) as location_group
                FROM with_distance
            )
            SELECT userId, location_group,
                   AVG(latitude) as stay_lat, AVG(longitude) as stay_lon,
                   MIN(timestamp) as start_time, MAX(timestamp) as end_time,
                   COUNT(*) as point_count,
                   (UNIX_TIMESTAMP(MAX(timestamp)) - UNIX_TIMESTAMP(MIN(timestamp))) / 60.0 as duration_minutes
            FROM clustered_points
            GROUP BY userId, location_group
            HAVING duration_minutes >= 10 AND point_count >= 3
            """);
    }
}