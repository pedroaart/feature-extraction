package br.com.pedroaart.featureextraction.controllers.user;

import br.com.pedroaart.featureextraction.domain.user.User;
import br.com.pedroaart.featureextraction.services.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok().body(userService.createUser(userDTO));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok().body(userService.getAll());
    }

    @GetMapping
    public ResponseEntity<User> find(@RequestParam(required = false) String id,
                                     @RequestParam(required = false) String fullName,
                                     @RequestParam(required = false) String email,
                                     @RequestParam(required = false) String birthDate) {
        return ResponseEntity.ok().body(userService.find(id, fullName, email, birthDate));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable("id") String id,
                                       @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok().body(userService.update(id, userDTO));
    }
}
