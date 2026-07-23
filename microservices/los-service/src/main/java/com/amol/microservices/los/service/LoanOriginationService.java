package com.amol.microservices.los.service;

import com.amol.microservices.los.domain.LoanApplication;
import com.amol.microservices.los.domain.LoanStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loan origination workflow. Applications are submitted (RECEIVED), underwritten to APPROVED or
 * REJECTED against a credit policy, then originated (booked) once approved. Illegal transitions are
 * rejected. State is in-memory (a real LOS uses a durable store).
 */
@Service
public class LoanOriginationService {

    private static final Logger log = LoggerFactory.getLogger(LoanOriginationService.class);
    private static final int MIN_CREDIT_SCORE = 640;
    private static final long MAX_AUTO_APPROVE_MINOR = 5_000_000; // 50,000.00

    private final ConcurrentHashMap<String, LoanApplication> applications = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public LoanOriginationService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public LoanApplication submit(String applicantId, long amountMinor, int termMonths, int creditScore) {
        if (amountMinor <= 0 || termMonths <= 0) {
            throw new IllegalArgumentException("amountMinor and termMonths must be positive");
        }
        LoanApplication app = new LoanApplication(
                UUID.randomUUID().toString(), applicantId, amountMinor, termMonths, creditScore);
        applications.put(app.getId(), app);
        log.info("application_submitted id={} applicantId={}", app.getId(), applicantId);
        return app;
    }

    public LoanApplication get(String id) {
        LoanApplication app = applications.get(id);
        if (app == null) {
            throw new IllegalArgumentException("Unknown application: " + id);
        }
        return app;
    }

    /** Underwrites a RECEIVED application, transitioning it to APPROVED or REJECTED. */
    public LoanApplication underwrite(String id) {
        LoanApplication app = get(id);
        if (app.getStatus() != LoanStatus.RECEIVED) {
            throw new IllegalStateException("Only RECEIVED applications can be underwritten (was "
                    + app.getStatus() + ")");
        }
        boolean approved = app.getCreditScore() >= MIN_CREDIT_SCORE
                && app.getAmountMinor() <= MAX_AUTO_APPROVE_MINOR;
        if (approved) {
            app.setStatus(LoanStatus.APPROVED);
            app.setDecisionReason("Meets credit policy");
        } else {
            app.setStatus(LoanStatus.REJECTED);
            app.setDecisionReason(app.getCreditScore() < MIN_CREDIT_SCORE
                    ? "Credit score below " + MIN_CREDIT_SCORE
                    : "Amount exceeds auto-approval limit");
        }
        counter(app.getStatus().name()).increment();
        log.info("application_underwritten id={} status={}", id, app.getStatus());
        return app;
    }

    /** Originates (books) an APPROVED application. */
    public LoanApplication originate(String id) {
        LoanApplication app = get(id);
        if (app.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED applications can be originated (was "
                    + app.getStatus() + ")");
        }
        app.setStatus(LoanStatus.ORIGINATED);
        counter("ORIGINATED").increment();
        log.info("application_originated id={}", id);
        return app;
    }

    private Counter counter(String outcome) {
        return Counter.builder("los.applications")
                .tag("outcome", outcome)
                .description("Loan application outcomes")
                .register(meterRegistry);
    }
}
