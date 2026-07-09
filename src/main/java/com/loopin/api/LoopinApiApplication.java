package com.loopin.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.loopin.api.moderation.ContentModerationProperties;
import com.loopin.api.moderation.ai.AiModerationProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ContentModerationProperties.class, AiModerationProperties.class})
public class LoopinApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoopinApiApplication.class, args);
		System.out.println("API Started Without Any Problem...");
	}

}
