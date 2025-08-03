package br.com.pedroaart.featureextraction.controllers.measurement;

import br.com.pedroaart.featureextraction.domain.measurement.MeasurementTypeEnum;
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
            String userId,
            MeasurementTypeEnum type,
            String tag
    ) {
    }
}
