package br.com.pedroaart.featureextraction.controllers.measurement;

import br.com.pedroaart.featureextraction.domain.measurement.Measurement;
import br.com.pedroaart.featureextraction.services.measurement.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Measurement>> findByUserId(@PathVariable String userId) {
        List<Measurement> measurements = measurementService.findByUserId(userId);
        return ResponseEntity.ok(measurements);
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<Measurement>> findAll() {
        List<Measurement> measurements = measurementService.findAll();
        return ResponseEntity.ok(measurements);
    }
}
