package br.com.pedroaart.featureextraction.services.spark;

import br.com.pedroaart.featureextraction.domain.spark.MobilityResult;
import br.com.pedroaart.featureextraction.domain.user.User;
import br.com.pedroaart.featureextraction.services.user.UserService;
import org.apache.spark.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.spark.sql.functions.col;

@Service
public class SparkMobilityAnalysisService {

    @Autowired
    private SparkSession spark;

    @Autowired
    private UserService userService;

    public MobilityResult analyzeMobilityWithSpark(String userId, LocalDateTime startDate, LocalDateTime endDate) {

        // 1. Validar se usuário existe
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("Usuário não encontrado: " + userId);
        }

        // 2. Carregar dados do MongoDB
        Dataset<Row> measurements = loadMeasurementsFromMongo(userId, startDate, endDate);

        if (measurements.count() == 0) {
            throw new IllegalArgumentException("Nenhuma medição encontrada para o usuário no período especificado");
        }

        // 3. Processar com Spark SQL
        measurements.createOrReplaceTempView("measurements");

        // 4. Detectar stay points
        Dataset<Row> stayPoints = detectStayPoints();
        stayPoints.createOrReplaceTempView("stay_points");

        // 5. Identificar jornadas
        Dataset<Row> journeys = identifyJourneys();
        journeys.createOrReplaceTempView("journeys");

        // 6. Calcular métricas finais
        MobilityResult result = calculateFinalMetrics(userId, user.get());

        // 7. Salvar resultado
        saveMobilityResult(result);

        return result;
    }

    private Dataset<Row> loadMeasurementsFromMongo(String userId, LocalDateTime start, LocalDateTime end) {

        // Carregar dados brutos do MongoDB
        Dataset<Row> rawData = spark.read()
                .format("mongodb")
                .option("database", "feature-extraction")
                .option("collection", "measurements")
                .load();

        // Transformar para formato flat e filtrar
        Dataset<Row> flattenedData = rawData
                .select(
                        col("_id").alias("id"),
                        col("timestamp"),
                        col("metadata.userId").alias("userId"),
                        col("metadata.deviceId").alias("deviceId"),
                        col("metadata.measurementTypeEnum").alias("measurementType"),
                        col("metadata.tag").alias("tag"),
                        col("accuracy"),
                        col("location.coordinates").getItem(1).alias("latitude"),  // GeoJSON: [lng, lat]
                        col("location.coordinates").getItem(0).alias("longitude")
                )
                .filter(col("userId").equalTo(userId))
                .filter(col("timestamp").between(
                        Timestamp.valueOf(start),
                        Timestamp.valueOf(end)))
                .filter(col("latitude").isNotNull().and(col("longitude").isNotNull()))
                .orderBy("timestamp");

        System.out.println("=== DADOS CARREGADOS ===");
        flattenedData.show(10);
        System.out.println("Total de medições: " + flattenedData.count());

        return flattenedData;
    }

    private Dataset<Row> detectStayPoints() {
        String stayPointQuery = """
            WITH ordered_measurements AS (
                SELECT userId, latitude, longitude, timestamp,
                       LAG(latitude) OVER (PARTITION BY userId ORDER BY timestamp) as prev_lat,
                       LAG(longitude) OVER (PARTITION BY userId ORDER BY timestamp) as prev_lon,
                       ROW_NUMBER() OVER (PARTITION BY userId ORDER BY timestamp) as row_num
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
                       -- Criar grupos baseados em distância (< 100m = mesmo local)
                       SUM(CASE WHEN distance_meters > 100 THEN 1 ELSE 0 END) 
                           OVER (PARTITION BY userId ORDER BY timestamp 
                                 ROWS UNBOUNDED PRECEDING) as location_group
                FROM with_distance
            )
            SELECT userId,
                   location_group,
                   AVG(latitude) as stay_lat,
                   AVG(longitude) as stay_lon,
                   MIN(timestamp) as start_time,
                   MAX(timestamp) as end_time,
                   COUNT(*) as point_count,
                   (UNIX_TIMESTAMP(MAX(timestamp)) - UNIX_TIMESTAMP(MIN(timestamp))) / 60.0 as duration_minutes
            FROM clustered_points
            GROUP BY userId, location_group
            HAVING duration_minutes >= 10 AND point_count >= 3
            ORDER BY start_time
            """;

        Dataset<Row> stayPoints = spark.sql(stayPointQuery);

        System.out.println("=== STAY POINTS DETECTADOS ===");
        stayPoints.show();

        return stayPoints;
    }

    private Dataset<Row> identifyJourneys() {
        String journeyQuery = """
            WITH journey_pairs AS (
                SELECT userId,
                       stay_lat as origin_lat, stay_lon as origin_lon, 
                       end_time as departure_time,
                       LEAD(stay_lat) OVER (PARTITION BY userId ORDER BY start_time) as dest_lat,
                       LEAD(stay_lon) OVER (PARTITION BY userId ORDER BY start_time) as dest_lon,
                       LEAD(start_time) OVER (PARTITION BY userId ORDER BY start_time) as arrival_time
                FROM stay_points
            ),
            valid_journeys AS (
                SELECT *,
                       (UNIX_TIMESTAMP(arrival_time) - UNIX_TIMESTAMP(departure_time)) / 60.0 as travel_time_minutes,
                       6371000 * 2 * ASIN(SQRT(
                           POWER(SIN(RADIANS(dest_lat - origin_lat) / 2), 2) +
                           COS(RADIANS(origin_lat)) * COS(RADIANS(dest_lat)) *
                           POWER(SIN(RADIANS(dest_lon - origin_lon) / 2), 2)
                       )) as distance_meters
                FROM journey_pairs
                WHERE dest_lat IS NOT NULL AND arrival_time IS NOT NULL
            )
            SELECT *,
                   CASE 
                       WHEN travel_time_minutes > 0 THEN
                           (distance_meters / (travel_time_minutes * 60)) * 3.6
                       ELSE 0.0
                   END as average_speed_kmh,
                   CASE 
                       WHEN (distance_meters / (travel_time_minutes * 60)) * 3.6 < 5 THEN 'walking'
                       WHEN (distance_meters / (travel_time_minutes * 60)) * 3.6 < 20 THEN 'bicycle'  
                       WHEN (distance_meters / (travel_time_minutes * 60)) * 3.6 < 60 THEN 'car'
                       ELSE 'public_transport'
                   END as transport_mode
            FROM valid_journeys
            WHERE travel_time_minutes BETWEEN 2 AND 300 -- Entre 2min and 5h
            AND distance_meters > 50 -- Mínimo 50m de distância
            """;

        Dataset<Row> journeys = spark.sql(journeyQuery);

        System.out.println("=== JORNADAS IDENTIFICADAS ===");
        journeys.show();

        return journeys;
    }

    private MobilityResult calculateFinalMetrics(String userId, User user) {

        // 1. Tempo em casa (baseado em locations do usuário se disponível)
        Dataset<Row> homeTime = calculateHomeTime(user);

        // 2. Estatísticas de viagem
        Dataset<Row> travelStats = spark.sql("""
        SELECT userId,
               COUNT(*) as total_trips,
               AVG(travel_time_minutes) as avg_travel_time_minutes,
               AVG(distance_meters) as avg_distance_meters,
               SUM(travel_time_minutes) as total_travel_time_minutes
        FROM journeys
        GROUP BY userId
        """);

        // 3. Meio de transporte predominante
        Dataset<Row> transportMode = spark.sql("""
        SELECT userId, transport_mode, COUNT(*) as trip_count
        FROM journeys
        GROUP BY userId, transport_mode
        ORDER BY trip_count DESC
        LIMIT 1
        """);

        // 4. Locais únicos visitados
        Dataset<Row> uniqueLocations = spark.sql("""
        SELECT userId, COUNT(*) as unique_locations
        FROM stay_points
        GROUP BY userId
        """);

        // Converter para objeto resultado
        MobilityResult result = new MobilityResult();
        result.setUserId(userId);
        result.setAnalysisDate(new java.sql.Timestamp(System.currentTimeMillis()));

        // Coletar resultados com getAs() e cast
        try {
            if (homeTime.count() > 0) {
                Row homeRow = homeTime.first();
                Double homeHours = homeRow.<Double>getAs("total_home_hours");
                result.setTotalHomeTimeHours(homeHours != null ? homeHours : 0.0);
            } else {
                result.setTotalHomeTimeHours(0.0);
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar tempo em casa: " + e.getMessage());
            result.setTotalHomeTimeHours(0.0);
        }

        try {
            if (travelStats.count() > 0) {
                Row travelRow = travelStats.first();

                Double totalTravelMinutes = travelRow.<Double>getAs("total_travel_time_minutes");
                Double avgDistance = travelRow.<Double>getAs("avg_distance_meters");
                Long totalTrips = travelRow.<Long>getAs("total_trips");

                result.setTotalTravelTimeHours(totalTravelMinutes != null ? totalTravelMinutes / 60.0 : 0.0);
                result.setAverageTravelDistance(avgDistance != null ? avgDistance : 0.0);
                result.setTotalTrips(totalTrips != null ? totalTrips.intValue() : 0);
            } else {
                result.setTotalTravelTimeHours(0.0);
                result.setAverageTravelDistance(0.0);
                result.setTotalTrips(0);
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar estatísticas de viagem: " + e.getMessage());
            result.setTotalTravelTimeHours(0.0);
            result.setAverageTravelDistance(0.0);
            result.setTotalTrips(0);
        }

        try {
            if (transportMode.count() > 0) {
                Row transportRow = transportMode.first();
                String transportModeStr = transportRow.<String>getAs("transport_mode");
                result.setPredominantTransportMode(transportModeStr != null ? transportModeStr : "unknown");
            } else {
                result.setPredominantTransportMode("unknown");
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar modo de transporte: " + e.getMessage());
            result.setPredominantTransportMode("unknown");
        }

        try {
            if (uniqueLocations.count() > 0) {
                Row locationRow = uniqueLocations.first();
                Long uniqueLocationCount = locationRow.<Long>getAs("unique_locations");
                result.setUniqueLocationsVisited(uniqueLocationCount != null ? uniqueLocationCount.intValue() : 0);
            } else {
                result.setUniqueLocationsVisited(0);
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar locais únicos: " + e.getMessage());
            result.setUniqueLocationsVisited(0);
        }

        return result;
    }

    private Dataset<Row> calculateHomeTime(User user) {
        // Se o usuário tem location "home" definida, usar ela
        if (user.getLocations() != null && user.getLocations().containsKey("home")) {
            GeoJsonPoint homeLocation = user.getLocations().get("home");
            double homeLat = homeLocation.getCoordinates().get(1); // lat
            double homeLon = homeLocation.getCoordinates().get(0); // lng

            return spark.sql(String.format("""
                SELECT userId,
                       SUM(duration_minutes) / 60.0 as total_home_hours
                FROM stay_points
                WHERE 6371000 * 2 * ASIN(SQRT(
                    POWER(SIN(RADIANS(stay_lat - %f) / 2), 2) +
                    COS(RADIANS(%f)) * COS(RADIANS(stay_lat)) *
                    POWER(SIN(RADIANS(stay_lon - %f) / 2), 2)
                )) < 200 -- 200m do home
                GROUP BY userId
                """, homeLat, homeLat, homeLon));
        } else {
            // Fallback: considerar período noturno como "casa"
            return spark.sql("""
                SELECT userId,
                       SUM(duration_minutes) / 60.0 as total_home_hours
                FROM stay_points
                WHERE HOUR(start_time) >= 20 OR HOUR(end_time) <= 7
                GROUP BY userId
                """);
        }
    }

    private void saveMobilityResult(MobilityResult result) {
        // Criar Dataset com o resultado
        List<MobilityResult> resultList = Arrays.asList(result);
        Encoder<MobilityResult> encoder = Encoders.bean(MobilityResult.class);
        Dataset<MobilityResult> resultDS = spark.createDataset(resultList, encoder);

        // Salvar no MongoDB
        resultDS.write()
                .format("mongodb")
                .option("database", "feature-extraction")
                .option("collection", "mobility_analysis")
                .mode("append")
                .save();

        System.out.println("=== RESULTADO SALVO ===");
        System.out.println("Análise de mobilidade salva para usuário: " + result.getUserId());
    }
}
