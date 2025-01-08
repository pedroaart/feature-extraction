package br.com.pedroaart.featureextraction.controllers.measurement;

import br.com.pedroaart.featureextraction.services.measurement.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/measurement")
public class MeasurementController {
    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody List<MeasurementDTO> measurement) {
        this.measurementService.create(measurement);
        return ResponseEntity.ok().build();
    }
}
