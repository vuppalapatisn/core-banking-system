package com.amol.microservices.lms.service;

import com.amol.microservices.lms.domain.Installment;
import com.amol.microservices.lms.domain.Loan;
import com.amol.microservices.lms.domain.LoanState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loan servicing. Books loans, computes their amortization schedule (fixed EMI, reducing balance),
 * records repayments against the outstanding balance, and marks the loan PAID_OFF at zero.
 */
@Service
public class LoanManagementService {

    private static final Logger log = LoggerFactory.getLogger(LoanManagementService.class);

    private final ConcurrentHashMap<String, Loan> loans = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public LoanManagementService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Loan book(long principalMinor, double annualRatePct, int termMonths) {
        if (principalMinor <= 0 || termMonths <= 0 || annualRatePct < 0) {
            throw new IllegalArgumentException("principalMinor/termMonths must be positive, rate >= 0");
        }
        Loan loan = new Loan(UUID.randomUUID().toString(), principalMinor, annualRatePct, termMonths);
        loans.put(loan.getId(), loan);
        Counter.builder("lms.loans").tag("event", "booked").register(meterRegistry).increment();
        log.info("loan_booked id={} principalMinor={} term={}", loan.getId(), principalMinor, termMonths);
        return loan;
    }

    public Loan get(String id) {
        Loan loan = loans.get(id);
        if (loan == null) {
            throw new IllegalArgumentException("Unknown loan: " + id);
        }
        return loan;
    }

    /** Fixed monthly instalment (EMI) for a loan, in minor units. */
    public long monthlyInstalment(Loan loan) {
        double monthlyRate = loan.getAnnualRatePct() / 100.0 / 12.0;
        int n = loan.getTermMonths();
        double principal = loan.getPrincipalMinor();
        if (monthlyRate == 0.0) {
            return Math.round(principal / n);
        }
        double factor = Math.pow(1 + monthlyRate, n);
        return Math.round(principal * monthlyRate * factor / (factor - 1));
    }

    /** Full amortization schedule; the final instalment absorbs rounding so the balance ends at 0. */
    public List<Installment> schedule(String id) {
        Loan loan = get(id);
        long emi = monthlyInstalment(loan);
        double monthlyRate = loan.getAnnualRatePct() / 100.0 / 12.0;
        long balance = loan.getPrincipalMinor();
        List<Installment> schedule = new ArrayList<>();
        for (int month = 1; month <= loan.getTermMonths(); month++) {
            long interest = Math.round(balance * monthlyRate);
            long principalPart = emi - interest;
            boolean last = month == loan.getTermMonths();
            if (last || principalPart >= balance) {
                principalPart = balance;
            }
            balance -= principalPart;
            long instalment = principalPart + interest;
            schedule.add(new Installment(month, instalment, principalPart, interest, balance));
            if (balance <= 0) {
                break;
            }
        }
        return schedule;
    }

    /** Records a repayment, reducing the outstanding balance; marks PAID_OFF at zero. */
    public synchronized Loan recordPayment(String id, long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amountMinor must be positive");
        }
        Loan loan = get(id);
        if (loan.getState() == LoanState.PAID_OFF) {
            throw new IllegalStateException("Loan " + id + " is already paid off");
        }
        long remaining = loan.getOutstandingMinor() - amountMinor;
        loan.setOutstandingMinor(Math.max(0, remaining));
        if (loan.getOutstandingMinor() == 0) {
            loan.setState(LoanState.PAID_OFF);
        }
        Counter.builder("lms.repayments").register(meterRegistry).increment();
        log.info("loan_repayment id={} outstandingMinor={} state={}",
                id, loan.getOutstandingMinor(), loan.getState());
        return loan;
    }
}
