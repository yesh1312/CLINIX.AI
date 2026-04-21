package com.clinixai.controller;

import com.clinixai.model.User;
import com.clinixai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            User user = userOpt.get();
            session.setAttribute("user", user);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("user", Map.of(
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "role", user.getRole()
            ));
            return ResponseEntity.ok(resp);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "message", "Invalid credentials"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String email = data.get("email");
        String password = data.get("password");
        String fullName = data.get("fullName");
        String role = data.get("role");

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Username already exists"));
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Email already exists"));
        }

        User user = new User(username, password, fullName, role, email);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("status", "success", "message", "User registered successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", user.getUsername(),
                "fullName", user.getFullName()
            ));
        }
        return ResponseEntity.ok(Map.of("authenticated", false));
    }
}
