package com.gg.turnlook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


// Punto de entrada de la aplicación.
@SpringBootApplication
@EnableScheduling
public class TurnlookApplication {

	public static void main(String[] args) {
		SpringApplication.run(TurnlookApplication.class, args);
	}

}
