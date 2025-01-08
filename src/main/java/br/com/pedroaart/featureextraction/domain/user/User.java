package br.com.pedroaart.featureextraction.domain.user;

import br.com.pedroaart.featureextraction.controllers.user.UserDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    private Map<String, GeoJsonPoint> locations;
    private List<String> devices;

    public User(UserDTO userDTO) {
        this.fullName = userDTO.fullName();
        this.email = userDTO.email();
        this.birthDate = userDTO.birthDate();
        this.locations = userDTO.locations();
        this.devices = userDTO.devices();
    }
}
