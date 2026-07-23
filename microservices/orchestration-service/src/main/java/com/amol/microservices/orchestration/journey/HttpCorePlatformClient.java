package com.amol.microservices.orchestration.journey;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP implementation of {@link CorePlatformClient} using {@link RestClient}. Calls the LOS, LMS,
 * and Payments services at their configured base URLs and propagates {@code X-Correlation-Id} from
 * the MDC on every hop, so the whole journey is traceable end-to-end.
 */
@Component
public class HttpCorePlatformClient implements CorePlatformClient {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final RestClient restClient;
    private final OrchestrationProperties.Services services;

    public HttpCorePlatformClient(OrchestrationProperties properties, RestClient.Builder builder) {
        this.services = properties.getIntegration().getServices();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.restClient = builder.requestFactory(factory).build();
    }

    @Override
    public Origination originateLoan(String applicantId, long amountMinor, int termMonths, int creditScore) {
        String base = services.getLos();
        JsonNode submitted = post(base + "/applications", Map.of(
                "applicantId", applicantId,
                "amountMinor", amountMinor,
                "termMonths", termMonths,
                "creditScore", creditScore));
        String applicationId = submitted.get("id").asText();

        JsonNode underwritten = post(base + "/applications/" + applicationId + "/underwrite", null);
        String status = underwritten.get("status").asText();

        if ("APPROVED".equals(status)) {
            JsonNode originated = post(base + "/applications/" + applicationId + "/originate", null);
            status = originated.get("status").asText();
        }
        return new Origination(applicationId, status);
    }

    @Override
    public Booking bookLoan(long principalMinor, double annualRatePct, int termMonths) {
        String base = services.getLms();
        JsonNode booked = post(base + "/loans", Map.of(
                "principalMinor", principalMinor,
                "annualRatePct", annualRatePct,
                "termMonths", termMonths));
        String loanId = booked.get("id").asText();
        JsonNode schedule = get(base + "/loans/" + loanId + "/schedule");
        return new Booking(loanId, schedule.size());
    }

    @Override
    public Disbursement disburse(String fromAccount, String toAccount, long amountMinor, String idempotencyKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("fromAccount", fromAccount);
        body.put("toAccount", toAccount);
        body.put("amountMinor", amountMinor);
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        JsonNode paid = post(services.getPayments() + "/payments", body);
        return new Disbursement(paid.get("id").asText(), paid.get("network").asText(), paid.get("status").asText());
    }

    private JsonNode post(String url, Object body) {
        RestClient.RequestBodySpec spec = restClient.post().uri(url).headers(this::correlation);
        if (body != null) {
            spec.body(body);
        }
        return spec.retrieve().body(JsonNode.class);
    }

    private JsonNode get(String url) {
        return restClient.get().uri(url).headers(this::correlation).retrieve().body(JsonNode.class);
    }

    private void correlation(HttpHeaders headers) {
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            headers.set(CORRELATION_HEADER, correlationId);
        }
    }
}
