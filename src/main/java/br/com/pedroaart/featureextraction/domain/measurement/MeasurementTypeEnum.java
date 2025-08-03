package br.com.pedroaart.featureextraction.domain.measurement;

import lombok.Getter;

@Getter
public enum MeasurementTypeEnum {
        CREATED_BY_USER("created_by_user"),
    COLLECTED_BY_TRACK("collected_by_track");

    private final String value;

    MeasurementTypeEnum(String value) {
        this.value = value;
    }

    public static MeasurementTypeEnum fromValue(String value) {
        for (MeasurementTypeEnum measurementTypeEnum : values()) {
            if (measurementTypeEnum.value.equalsIgnoreCase(value)) {
                return measurementTypeEnum;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + value);
    }
}