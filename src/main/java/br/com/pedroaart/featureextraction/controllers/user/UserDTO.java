package br.com.pedroaart.featureextraction.controllers.user;

import java.time.LocalDate;

public record UserDTO(String fullName, String email, LocalDate birthDate) {
}
