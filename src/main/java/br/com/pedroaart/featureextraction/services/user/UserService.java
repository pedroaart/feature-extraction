package br.com.pedroaart.featureextraction.services.user;

import br.com.pedroaart.featureextraction.controllers.user.UserDTO;
import br.com.pedroaart.featureextraction.domain.user.User;
import br.com.pedroaart.featureextraction.domain.user.exeptions.UserNotFoundException;
import br.com.pedroaart.featureextraction.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        user.setBirthDate(userDTO.birthDate());
        user.setEmail(userDTO.email());
        user.setFullName(userDTO.fullName());

        return userRepository.save(user);
    }

    public User find(String id,
                     String fullName,
                     String email,
                     String birthDate) {
        return null;
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
}
