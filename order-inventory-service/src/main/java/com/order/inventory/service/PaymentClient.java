package com.order.inventory.service;

import com.order.inventory.model.OrderMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap; import java.util.Map;

@Service
@Slf4j
public class PaymentClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.payment.url:http://localhost:8082}")
    private String paymentUrl;


    @CircuitBreaker(
            name = "paymentService",
            fallbackMethod = "paymentFallback"
    )
    @Retry(
            name = "paymentRetry"
    )

    public String callPayment(OrderMessage order) {
        log.info("Calling Payment service for orderId={}", order.getOrderId());
        Map<String, Object> req = new HashMap<>();
        req.put("orderId", order.getOrderId());
        req.put("amount", order.getTotalAmount());
        req.put("product", order.getProduct());

        String response = restTemplate.postForObject(paymentUrl + "/api/payment/process", req, String.class);
        log.info("Payment response: {}", response);
        log.info("Calling Payment Service for {}", order.getOrderId());
//        restTemplate.postForObject(
//                paymentUrl,
//                order,
//                String.class);
        return response;
    }

    // Fallback runs when Payment service is down or CB is OPEN
    public String paymentFallback(OrderMessage order, Throwable t) {
        log.error("Payment service unavailable for orderId={}. CB fallback. Reason: {}", order.getOrderId(), t.getMessage());
        // Throw so JMS does NOT acknowledge — message goes back to MQ → eventually DLQ
        throw new RuntimeException("Payment service unavailable — requeue message", t);
    }
}
