package br.com.pedroaart.featureextraction.services.measurement;

import br.com.pedroaart.featureextraction.controllers.measurement.MeasurementDTO;
import br.com.pedroaart.featureextraction.domain.device.Device;
import br.com.pedroaart.featureextraction.domain.measurement.Measurement;
import br.com.pedroaart.featureextraction.domain.measurement.exceptions.MeasurementWithoutOwnerException;
import br.com.pedroaart.featureextraction.domain.user.User;
import br.com.pedroaart.featureextraction.repositories.MeasurementRepository;
import br.com.pedroaart.featureextraction.services.device.DeviceService;
import br.com.pedroaart.featureextraction.services.user.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final DeviceService deviceService;
    private final UserService userService;

    public MeasurementService(MeasurementRepository measurementRepository, DeviceService deviceService, UserService userService) {
        this.measurementRepository = measurementRepository;
        this.deviceService = deviceService;
        this.userService = userService;
    }

    @Async
    public void create(List<MeasurementDTO> measurementData) {
        List<Measurement> measurements = new ArrayList<>();
        for (MeasurementDTO measurementDTO : measurementData) {
            String deviceId = measurementDTO.metadata().deviceId();
            String userId = measurementDTO.metadata().userId();

            if (deviceId == null && userId == null) {
                throw new MeasurementWithoutOwnerException();
            }
            //Optional<Device> device = deviceService.findById(measurementDTO.metadata().deviceId());
            Optional<User> user = userService.findById(measurementDTO.metadata().userId());

            if (user.isEmpty()) throw new MeasurementWithoutOwnerException();

            measurements.add(new Measurement(measurementDTO));
        }
        measurementRepository.saveAll(measurements);

    }

    public List<Measurement> findByUserId(String userId) {
        return measurementRepository.findByUserId(userId);
    }

    public List<Measurement> findAll() {
        return measurementRepository.findAll();
    }

    public void deleteAllByUserId(String userId) {
        measurementRepository.deleteByUserId(userId);
    }
}
