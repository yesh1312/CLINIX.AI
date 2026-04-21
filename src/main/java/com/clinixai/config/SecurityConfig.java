package com.clinixai.config;

import com.clinixai.model.User;
import com.clinixai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/api/auth/**", "/api/v1/**", "/api/history/**",
                                "/css/**", "/js/**",
                                "/error", "/favicon.ico", "/h2-console/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oauth2SuccessHandler()))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/login"));

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            String fullName = oauthUser.getAttribute("name");
            String providerId = oauthUser.getName(); // This is the unique subject ID for Google

            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;
            if (userOpt.isPresent()) {
                user = userOpt.get();
                // Update provider info if needed
                if (user.getoAuthProvider() == null) {
                    user.setoAuthProvider("GOOGLE");
                    user.setoAuthProviderId(providerId);
                    userRepository.save(user);
                }
            } else {
                // Create new user for Google login
                user = new User(email, "OAUTH_USER", fullName, "CLINICIAN", email);
                user.setoAuthProvider("GOOGLE");
                user.setoAuthProviderId(providerId);
                userRepository.save(user);
            }

            // Link to existing session logic
            HttpSession session = request.getSession();
            session.setAttribute("user", user);

            response.sendRedirect("/");
        };
    }
}
