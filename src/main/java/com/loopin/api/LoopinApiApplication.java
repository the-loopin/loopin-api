package com.loopin.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoopinApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoopinApiApplication.class, args);
		System.out.println("API Started Without Any Problem...");
	}

}
