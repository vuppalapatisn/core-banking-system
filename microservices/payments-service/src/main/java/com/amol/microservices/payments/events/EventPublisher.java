package com.amol.microservices.payments.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes domain / CDC events to the event-streaming-service. Fire-and-forget: a publish failure
 * is logged and swallowed so it never fails the business operation (a production system would use an
 * outbox / async dispatch). Propagates {@code X-Correlation-Id} so events correlate with the request.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RestClient client;
    private final String baseUrl;
    private final boolean enabled;

    public EventPublisher(
            @Value("${events.base-url:http://event-streaming-service:8101/events}") String baseUrl,
            @Value("${events.enabled:true}") boolean enabled,
            RestClient.Builder builder) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(2_000);
        this.client = builder.requestFactory(factory).build();
    }

    public void publish(String topic, String key, Object payload) {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("key", key);
            body.put("payload", payload);
            client.post()
                    .uri(baseUrl + "/topics/" + topic + "/publish")
                    .headers(h -> {
                        String correlationId = MDC.get("correlationId");
                        if (correlationId != null) {
                            h.set("X-Correlation-Id", correlationId);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("event_publish_failed topic={} reason={}", topic, ex.getMessage());
        }
    }
}
