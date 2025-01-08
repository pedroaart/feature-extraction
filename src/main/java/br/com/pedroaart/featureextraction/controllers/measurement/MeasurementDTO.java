package br.com.pedroaart.featureextraction.controllers.measurement;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.Instant;


public record MeasurementDTO(
        Instant timestamp,
        MetadataDTO metadata,
        Integer accuracy,
        GeoJsonPoint location
) {
    public record MetadataDTO(
            String deviceId,
            String userId
    ) {
    }
}
