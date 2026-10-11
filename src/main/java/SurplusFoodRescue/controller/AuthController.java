
package SurplusFoodRescue.controller;

import SurplusFoodRescue.model.User;
import SurplusFoodRescue.repository.UserRepository;
import SurplusFoodRescue.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DuplicateKeyException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(
            UserService userService,
            UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // Register a new Donor or NGO
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody Map<String, String> data) {

        try {
            userService.register(
                    data.get("name"),
                    data.get("email"),
                    data.get("password"),
                    data.get("role")
            );

            return ResponseEntity.ok(
                    Map.of("message", "Registration successful.")
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );

        } catch (DuplicateKeyException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "This email is already registered.")
            );
        }
    }

    // Log in and create a session
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> data,
            HttpSession session) {

        String email = data.get("email");
        String password = data.get("password");

        if (!userService.login(email, password)) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Invalid email or password.")
            );
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> user =
                userRepository.findByEmail(normalizedEmail);

        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Account not found.")
            );
        }

        session.setAttribute("userId", user.get().getId());
        session.setAttribute("userEmail", user.get().getEmail());
        session.setAttribute("userRole", user.get().getRole());

        return ResponseEntity.ok(Map.of(
                "message", "Login successful.",
                "name", user.get().getName(),
                "role", user.get().getRole()
        ));
    }

    // Get the current logged-in user's details
    @GetMapping("/me")
    public ResponseEntity<?> currentUser(HttpSession session) {

        Object userId = session.getAttribute("userId");
        Object email = session.getAttribute("userEmail");
        Object role = session.getAttribute("userRole");

        if (userId == null || email == null || role == null) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Not logged in.")
            );
        }

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "email", email,
                "role", role
        ));
    }

    // Log out and invalidate the session
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();

        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully.")
        );
    }
}
