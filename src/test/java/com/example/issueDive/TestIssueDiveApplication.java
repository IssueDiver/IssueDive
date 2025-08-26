package com.example.issueDive;

import org.springframework.boot.SpringApplication;

public class TestIssueDiveApplication {

	public static void main(String[] args) {
		SpringApplication.from(IssueDiveApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
