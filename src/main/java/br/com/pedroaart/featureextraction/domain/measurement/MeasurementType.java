package br.com.pedroaart.featureextraction.domain.measurement;

import lombok.Getter;

@Getter
public enum MeasurementType {
    CREATED_BY_USER("created_by_user"),
    COLLECTED_BY_TRACK("collected_by_track");

    private final String value;

    MeasurementType(String value) {
        this.value = value;
    }

    public static MeasurementType fromValue(String value) {
        for (MeasurementType measurementType : values()) {
            if (measurementType.value.equalsIgnoreCase(value)) {
                return measurementType;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + value);
    }
}