package br.com.pedroaart.featureextraction.controllers.user;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record UserDTO(
        String fullName,
        LocalDate birthDate,
        Map<String, GeoJsonPoint> locations,
        List<String> devices
) {
}
