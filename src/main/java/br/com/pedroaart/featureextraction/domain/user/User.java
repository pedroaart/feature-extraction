package br.com.pedroaart.featureextraction.domain.user;

import br.com.pedroaart.featureextraction.controllers.user.UserDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    private String id;
    private String fullName;
    private String email;
    private LocalDate birthDate;

    public User(UserDTO userDTO) {
        this.fullName = userDTO.fullName();
        this.email = userDTO.email();
        this.birthDate = userDTO.birthDate();
    }
}
