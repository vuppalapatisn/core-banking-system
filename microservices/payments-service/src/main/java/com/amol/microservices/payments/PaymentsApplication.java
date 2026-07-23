package com.amol.microservices.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Payments switch / gateway — accepts payment instructions, routes each to the appropriate network
 * (internal book transfer, ACH, or wire), and processes them idempotently.
 */
@SpringBootApplication
public class PaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
    }
}
