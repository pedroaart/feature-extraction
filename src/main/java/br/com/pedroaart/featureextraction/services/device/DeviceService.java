package br.com.pedroaart.featureextraction.services.device;

import br.com.pedroaart.featureextraction.controllers.device.DeviceDTO;
import br.com.pedroaart.featureextraction.domain.device.Device;
import br.com.pedroaart.featureextraction.repositories.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {
    private DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Device create(DeviceDTO deviceDTO) {
        Device device = new Device(deviceDTO);
        return deviceRepository.save(device);
    }

    public List<Device> getAll() {
        return deviceRepository.findAll();
    }

    public Optional<Device> findById(String id) {
        return deviceRepository.findById(id);
    }
}
