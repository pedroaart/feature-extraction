package br.com.pedroaart.featureextraction.domain.spark;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SparkMeasurement implements Serializable {
    private String id;
    private java.sql.Timestamp timestamp;
    private String userId;
    private String deviceId;
    private String measurementType;
    private String tag;
    private Integer accuracy;
    private Double latitude;
    private Double longitude;


}

