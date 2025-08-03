package br.com.pedroaart.featureextraction.services.user;

import br.com.pedroaart.featureextraction.controllers.user.UserDTO;
import br.com.pedroaart.featureextraction.domain.user.User;
import br.com.pedroaart.featureextraction.domain.user.exeptions.UserNotFoundException;
import br.com.pedroaart.featureextraction.repositories.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public UserService(UserRepository userRepository, MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public User createUser(UserDTO userDTO) {
        User user = new User(userDTO);
        return userRepository.save(user);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User update(String id, UserDTO userDTO) {
        User user = this.userRepository.findById(id).orElseThrow(UserNotFoundException::new);

        if (userDTO.birthDate() != null) user.setBirthDate(userDTO.birthDate());
        if (userDTO.fullName() != null && !user.getFullName().isEmpty()) user.setFullName(userDTO.fullName());
        if (userDTO.locations() != null) {
            Map<String, GeoJsonPoint> mergedLocations = new HashMap<>(
                    user.getLocations() != null ? user.getLocations() : Collections.emptyMap()
            );
            mergedLocations.putAll(userDTO.locations());
            user.setLocations(mergedLocations);
        }
        if (userDTO.devices() != null) user.setDevices(userDTO.devices());

        return userRepository.save(user);
    }


    public User find(String id, String fullName, String email, String birthDate) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (id != null) {
            criteriaList.add(Criteria.where("id").is(id));
        }
        if (fullName != null) {
            criteriaList.add(Criteria.where("fullName").regex(fullName, "i"));
        }
        if (email != null) {
            criteriaList.add(Criteria.where("email").is(email));
        }
        if (birthDate != null) {
            try {
                LocalDate parsedDate = LocalDate.parse(birthDate);
                criteriaList.add(Criteria.where("birthDate").is(parsedDate));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Formato de data inválido. Use yyyy-MM-dd.");
            }
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.findOne(query, User.class);
    }


    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
}
