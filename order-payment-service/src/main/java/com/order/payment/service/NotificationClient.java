package com.order.payment.service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
@Service @Slf4j
public class NotificationClient {
    @Autowired
    private RestTemplate restTemplate;
    @Value("${app.notification.url:http://localhost:8083}")
    private String notificationUrl;

    @Retry(
            name = "notificationRetry"
    )
    @CircuitBreaker(
            name = "notificationService",
            fallbackMethod = "notifyFallback"
    )
    public String notify(
            String orderId,
            String status,
            double amount) {

        log.info("Calling Notification service for orderId={}", orderId);

        Map<String, Object> req =
                Map.of(
                        "orderId", orderId,
                        "status", status,
                        "amount", amount
                );

        return restTemplate.postForObject(
                notificationUrl + "/api/notification/send",
                req,
                String.class
        );

    }

    public String notifyFallback(
            String orderId,
            String status,
            double amount,
            Throwable t) {

        log.error(
                "Notification fallback triggered for orderId={} reason={}",
                orderId,
                t.getMessage());

        return "Notification service unavailable";
    }

}