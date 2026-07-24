package com.amol.microservices.ai.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Offline, deterministic assistant model — intent detection over keywords returning safe, templated
 * guidance. No external calls, so it is free and test-safe. A real LLM-backed {@link AssistantModel}
 * replaces this in production without changing callers.
 */
@Component
public class DeterministicAssistantModel implements AssistantModel {

    @Override
    public String answer(String question, String context) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        String grounding = (context == null || context.isBlank()) ? "" : " Based on the context you provided, ";

        String body;
        if (q.contains("loan") || q.contains("emi")) {
            body = "loan applications are originated in the LOS and serviced in the LMS; "
                    + "check the amortization schedule for EMI and payoff details.";
        } else if (q.contains("fraud") || q.contains("suspicious")) {
            body = "flag the transaction for review — the fraud service scores amount, velocity, "
                    + "cross-border and rail signals and can BLOCK or REVIEW it.";
        } else if (q.contains("balance") || q.contains("account")) {
            body = "account balances and postings are held in the core banking system (CBS); "
                    + "every movement is double-entry in the general ledger.";
        } else if (q.contains("payment") || q.contains("transfer")) {
            body = "payments are routed by the payments switch to INTERNAL, ACH or WIRE based on "
                    + "amount and destination, and are processed idempotently.";
        } else {
            body = "I can help with loans, payments, accounts, and fraud questions for this platform.";
        }
        return grounding + capitalize(body);
    }

    @Override
    public String name() {
        return "deterministic-stub";
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
