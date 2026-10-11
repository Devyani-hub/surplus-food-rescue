
package SurplusFoodRescue.service;

import SurplusFoodRescue.model.User;
import SurplusFoodRescue.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserService(
            UserRepository userRepository,
            PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Transactional
    public void register(
            String name,
            String email,
            String password,
            String role) {

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Enter a name, valid email, and password of at least 8 characters.");
        }

        if (!"DONOR".equals(role) && !"NGO".equals(role)) {
            throw new IllegalArgumentException(
                    "Role must be DONOR or NGO.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "An account with this email already exists.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(
                passwordService.hashPassword(password));
        user.setRole(role);

        userRepository.createUser(user);
    }

    public boolean login(String email, String password) {
        if (email == null || password == null) {
            return false;
        }

        return userRepository.findByEmail(
                email.trim().toLowerCase())
                .map(user -> passwordService.verifyPassword(
                        password, user.getPasswordHash()))
                .orElse(false);
    }
}
