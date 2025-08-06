package br.com.pedroaart.featureextraction.domain.spark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationResult {
    private String userId;
    private java.sql.Timestamp validationDate;
    private Double homeTimeMAE;          // Erro absoluto médio - tempo em casa
    private Double travelTimeMAE;        // Erro absoluto médio - tempo de viagem
    private Double locationCountMAE;     // Erro absoluto médio - contagem de locais
    private Double homeTimeAccuracy;     // Acurácia tempo em casa
    private Double travelTimeAccuracy;   // Acurácia tempo de viagem
    private Double locationCountAccuracy;// Acurácia contagem de locais
    private Double overallF1Score;       // F1-Score geral

}
