package com.doomscroll.wik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class WikApplication {
	public static void main(String[] args) {
		try {
			java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
			if (java.nio.file.Files.exists(envPath)) {
				java.nio.file.Files.lines(envPath)
						.map(String::trim)
						.filter(line -> !line.isEmpty() && !line.startsWith("#"))
						.forEach(line -> {
							int eqIndex = line.indexOf('=');
							if (eqIndex > 0) {
								String key = line.substring(0, eqIndex).trim();
								String value = line.substring(eqIndex + 1).trim();
								if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
									value = value.substring(1, value.length() - 1);
								} else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
									value = value.substring(1, value.length() - 1);
								}
								System.setProperty(key, value);
							}
						});
			}
		} catch (Exception e) {
			System.err.println("Could not load .env file: " + e.getMessage());
		}
		SpringApplication.run(WikApplication.class, args);
	}
}
