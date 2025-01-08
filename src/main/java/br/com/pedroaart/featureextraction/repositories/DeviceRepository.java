package br.com.pedroaart.featureextraction.repositories;

import br.com.pedroaart.featureextraction.domain.device.Device;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends MongoRepository<Device, String> {
}
