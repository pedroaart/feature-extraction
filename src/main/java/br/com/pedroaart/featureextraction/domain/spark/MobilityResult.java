package br.com.pedroaart.featureextraction.domain.spark;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class MobilityResult implements Serializable {
    private String userId;
    private java.sql.Timestamp analysisDate;
    private Double totalHomeTimeHours;
    private Double totalTravelTimeHours;
    private Integer uniqueLocationsVisited;
    private String predominantTransportMode;
    private Double averageTravelDistance;
    private Integer totalTrips;

}
