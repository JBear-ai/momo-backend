package com.momo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class MomoBackendApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(MomoBackendApplication.class);

		// 서비스 계층에서 필요한 데이터를 조회하도록 OSIV를 비활성화한다.
		application.setDefaultProperties(Map.of(
				"spring.jpa.open-in-view", "false"
		));

		application.run(args);
	}

}