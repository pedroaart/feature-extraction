package br.com.pedroaart.featureextraction.repositories;

import br.com.pedroaart.featureextraction.domain.measurement.Measurement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeasurementRepository extends MongoRepository<Measurement, String> {
}
