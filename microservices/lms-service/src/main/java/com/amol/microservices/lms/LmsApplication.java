package com.amol.microservices.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Loan Management System (LMS) — services loans post-disbursement: books a loan, computes its
 * amortization schedule, records repayments, and tracks the outstanding balance to payoff.
 */
@SpringBootApplication
public class LmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }
}
