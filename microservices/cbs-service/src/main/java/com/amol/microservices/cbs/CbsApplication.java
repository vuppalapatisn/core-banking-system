package com.amol.microservices.cbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Core Banking System (CBS) — the system of record for customers, CASA accounts
 * (current/savings), and general-ledger postings with double-entry bookkeeping.
 */
@SpringBootApplication
public class CbsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CbsApplication.class, args);
    }
}
