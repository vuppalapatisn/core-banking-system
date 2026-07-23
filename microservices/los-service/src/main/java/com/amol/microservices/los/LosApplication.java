package com.amol.microservices.los;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Loan Origination System (LOS) — manages the loan application lifecycle from intake through
 * underwriting to origination (booking a loan for servicing by the LMS).
 */
@SpringBootApplication
public class LosApplication {

    public static void main(String[] args) {
        SpringApplication.run(LosApplication.class, args);
    }
}
