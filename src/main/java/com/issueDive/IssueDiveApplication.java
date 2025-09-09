package com.issueDive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IssueDiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(IssueDiveApplication.class, args);
	}

}
