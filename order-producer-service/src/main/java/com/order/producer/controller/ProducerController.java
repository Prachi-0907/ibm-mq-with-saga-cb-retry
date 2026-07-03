package com.order.producer.controller;

import com.order.producer.model.OrderMessage;
import com.order.producer.service.OutboxOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/producer")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class ProducerController {

    @Autowired
    private OutboxOrderService outboxOrderService;

    @PostMapping("/send")
    public Map<String, Object> sendOrder(
            @RequestParam(defaultValue = "Laptop") String product,
            @RequestParam(defaultValue = "2") int quantity) {

        // Outbox pattern: saves to outbox_events table only, NO direct MQ call
        OrderMessage order = outboxOrderService.placeOrder(product, quantity, "MEDIUM");

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Order queued via Outbox pattern!");
        res.put("orderId", order.getOrderId());
        res.put("product", product);
        res.put("quantity", quantity);
        res.put("note", "Relay will publish to MQ within 5 seconds");
        return res;
    }

    @PostMapping("/send-bad")
    public Map<String, Object> sendBadOrder() {
        OrderMessage order = outboxOrderService.placeOrder("BadProduct", -1, "HIGH");
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Bad order queued (quantity=-1, will go to DLQ)");
        res.put("orderId", order.getOrderId());
        return res;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service", "order-producer-service", "status", "UP", "port", 8080);
    }
}
