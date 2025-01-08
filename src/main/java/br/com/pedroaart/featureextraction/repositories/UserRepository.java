package br.com.pedroaart.featureextraction.repositories;

import br.com.pedroaart.featureextraction.domain.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
}
