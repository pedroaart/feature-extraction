package br.com.pedroaart.featureextraction.controllers.device;

import br.com.pedroaart.featureextraction.domain.device.Device;
import br.com.pedroaart.featureextraction.services.device.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<Device> create(@RequestBody DeviceDTO deviceDTO) {
        return ResponseEntity.ok().body(deviceService.create(deviceDTO));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Device>> getAll() {
        return ResponseEntity.ok().body(deviceService.getAll());
    }

}
