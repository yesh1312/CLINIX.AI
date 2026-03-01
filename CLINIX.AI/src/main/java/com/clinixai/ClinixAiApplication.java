package com.clinixai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.clinixai", "com.medai.braintumor" })
public class ClinixAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClinixAiApplication.class, args);
	}

}
