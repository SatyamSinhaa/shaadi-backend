package com.shaadi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShaadiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShaadiBackendApplication.class, args);
	}
}