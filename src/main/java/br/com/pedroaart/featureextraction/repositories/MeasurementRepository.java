package br.com.pedroaart.featureextraction.repositories;

import br.com.pedroaart.featureextraction.domain.measurement.Measurement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeasurementRepository extends MongoRepository<Measurement, String> {
    @Query("{ 'metadata.userId': ?0 }")
    List<Measurement> findByUserId(String userId);

    @Query(value = "{ 'metadata.userId': ?0 }", delete = true)
    void deleteByUserId(String userId);
}
