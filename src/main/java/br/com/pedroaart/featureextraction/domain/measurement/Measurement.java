package br.com.pedroaart.featureextraction.domain.measurement;

import br.com.pedroaart.featureextraction.controllers.measurement.MeasurementDTO;
import br.com.pedroaart.featureextraction.domain.device.Device;
import br.com.pedroaart.featureextraction.domain.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "measurements")
@Getter
@Setter
@NoArgsConstructor
public class Measurement {
    @Id
    private String id;
    private Instant timestamp;
    private Metadata metadata;
    private Integer accuracy;
    private GeoJsonPoint location;

    public Measurement(MeasurementDTO measurementDTO) {
        this.timestamp = measurementDTO.timestamp();
        this.accuracy = measurementDTO.accuracy();
        this.location = new GeoJsonPoint(measurementDTO.location());
        this.metadata = new Metadata(measurementDTO.metadata().deviceId(), measurementDTO.metadata().userId());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Metadata {
        private String deviceId;
        private String userId;
        @Transient
        private Device device;
        @Transient
        private User user;

        public Metadata(String deviceId, String userId) {
            this.deviceId = deviceId;
            this.userId = userId;
        }
    }
}
