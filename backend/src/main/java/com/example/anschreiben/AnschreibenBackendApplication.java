package com.example.anschreiben;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Anschreiben Generator backend.
 *
 * <p>This Spring Boot application exposes a single REST endpoint for
 * generating cover letters. The controller delegates to a service which
 * invokes a Python script to build the letter text and then converts
 * that text into a PDF using Apache PDFBox. The finished PDF is
 * streamed back to the client.</p>
 */
@SpringBootApplication
public class AnschreibenBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnschreibenBackendApplication.class, args);
    }
}