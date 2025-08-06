package br.com.pedroaart.featureextraction.controllers.spark;

import br.com.pedroaart.featureextraction.domain.spark.MobilityResult;
import br.com.pedroaart.featureextraction.domain.spark.ValidationResult;
import br.com.pedroaart.featureextraction.domain.user.User;
import br.com.pedroaart.featureextraction.services.spark.SparkMobilityAnalysisService;
import br.com.pedroaart.featureextraction.services.spark.SparkValidationService;
import br.com.pedroaart.featureextraction.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spark-mobility")
public class SparkMobilityController {

    @Autowired
    private SparkValidationService sparkValidationService;

    @Autowired
    private SparkMobilityAnalysisService sparkAnalysisService;

    @Autowired
    private UserService userService;

    @PostMapping("/analyze/{userId}")
    public ResponseEntity<?> analyzeUserMobility(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            MobilityResult result = sparkAnalysisService.analyzeMobilityWithSpark(
                    userId,
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59)
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Análise de mobilidade concluída com Spark",
                    "userId", userId,
                    "period", startDate + " to " + endDate,
                    "results", Map.of(
                            "totalHomeTimeHours", result.getTotalHomeTimeHours(),
                            "totalTravelTimeHours", result.getTotalTravelTimeHours(),
                            "uniqueLocationsVisited", result.getUniqueLocationsVisited(),
                            "predominantTransportMode", result.getPredominantTransportMode(),
                            "totalTrips", result.getTotalTrips()
                    ),
                    "status", "completed"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro na análise Spark: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze-all")
    public ResponseEntity<?> analyzeAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Buscar todos os usuários
            List<User> users = userService.getAll();
            List<String> processedUsers = new ArrayList<>();
            List<String> errorUsers = new ArrayList<>();

            for (User user : users) {
                try {
                    sparkAnalysisService.analyzeMobilityWithSpark(
                            user.getId(),
                            startDate.atStartOfDay(),
                            endDate.atTime(23, 59, 59)
                    );
                    processedUsers.add(user.getId());
                } catch (Exception e) {
                    errorUsers.add(user.getId() + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Análise em lote concluída",
                    "processedUsers", processedUsers,
                    "errors", errorUsers,
                    "totalProcessed", processedUsers.size(),
                    "totalErrors", errorUsers.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro na análise em lote: " + e.getMessage()));
        }
    }

    @PostMapping("/validate/{userId}")
    public ResponseEntity<?> validateAnalysis(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            ValidationResult validation = sparkValidationService.validateMobilityAnalysis(
                    userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
            );

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "validation", Map.of(
                            "homeTimeAccuracy", String.format("%.2f%%", validation.getHomeTimeAccuracy() * 100),
                            "travelTimeAccuracy", String.format("%.2f%%", validation.getTravelTimeAccuracy() * 100),
                            "locationCountAccuracy", String.format("%.2f%%", validation.getLocationCountAccuracy() * 100),
                            "overallF1Score", String.format("%.2f%%", validation.getOverallF1Score() * 100)
                    ),
                    "errors", Map.of(
                            "homeTimeMAE", validation.getHomeTimeMAE() + " hours",
                            "travelTimeMAE", validation.getTravelTimeMAE() + " hours",
                            "locationCountMAE", validation.getLocationCountMAE() + " locations"
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro na validação: " + e.getMessage()));
        }
    }
}