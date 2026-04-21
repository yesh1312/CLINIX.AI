package com.clinixai.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    private String fullName;
    private String role;

    // OAuth2 support
    private String oAuthProvider; // e.g., GOOGLE
    private String oAuthProviderId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisHistory> history = new ArrayList<>();

    public User() {}

    public User(String username, String password, String fullName, String role, String email) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getoAuthProvider() { return oAuthProvider; }
    public void setoAuthProvider(String oAuthProvider) { this.oAuthProvider = oAuthProvider; }

    public String getoAuthProviderId() { return oAuthProviderId; }
    public void setoAuthProviderId(String oAuthProviderId) { this.oAuthProviderId = oAuthProviderId; }

    public List<AnalysisHistory> getHistory() { return history; }
    public void setHistory(List<AnalysisHistory> history) { this.history = history; }
}
