package com.momo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class MomoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MomoBackendApplication.class, args);
	}

}