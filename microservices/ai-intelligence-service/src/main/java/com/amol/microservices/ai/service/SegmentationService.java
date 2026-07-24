package com.amol.microservices.ai.service;

import com.amol.microservices.ai.model.SegmentRequest;
import org.springframework.stereotype.Service;

/**
 * Customer segmentation and next-best-offer. Assigns a segment from balance/tenure/activity and maps
 * it to a recommended product. Reference rules; a clustering model + recommender swap in here.
 */
@Service
public class SegmentationService {

    private static final long HIGH_VALUE_BALANCE_MINOR = 10_000_000; // 100,000.00
    private static final long AFFLUENT_BALANCE_MINOR = 2_000_000;    // 20,000.00

    public record Result(String segment, String nextBestOffer) {
    }

    public Result segment(SegmentRequest r) {
        String segment;
        if (r.monthlyTxns() == 0 && r.tenureMonths() > 6) {
            segment = "DORMANT";
        } else if (r.balanceMinor() >= HIGH_VALUE_BALANCE_MINOR) {
            segment = "HIGH_VALUE";
        } else if (r.balanceMinor() >= AFFLUENT_BALANCE_MINOR) {
            segment = "MASS_AFFLUENT";
        } else {
            segment = "STANDARD";
        }
        return new Result(segment, nextBestOffer(segment, r));
    }

    private String nextBestOffer(String segment, SegmentRequest r) {
        return switch (segment) {
            case "HIGH_VALUE" -> "Wealth management advisory";
            case "MASS_AFFLUENT" -> r.productCount() < 2 ? "Premium credit card" : "Investment account";
            case "DORMANT" -> "Re-engagement cashback offer";
            default -> r.productCount() < 2 ? "Savings account bundle" : "Personal loan pre-approval";
        };
    }
}
