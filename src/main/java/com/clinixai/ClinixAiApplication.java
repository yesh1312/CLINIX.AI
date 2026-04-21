package com.clinixai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.clinixai.model.User;
import com.clinixai.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.clinixai", "com.medai.braintumor" })
public class ClinixAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClinixAiApplication.class, args);
	}

	@Bean
	CommandLineRunner init(UserRepository userRepository) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User("admin", "admin123", "Clinical Administrator", "ADMIN", "admin@clinix.ai");
				userRepository.save(admin);
				System.out.println("DEBUG: Default admin user created (admin/admin123)");
			}
		};
	}
}
