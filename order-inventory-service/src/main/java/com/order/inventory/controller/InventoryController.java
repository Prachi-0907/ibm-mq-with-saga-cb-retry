package com.order.inventory.controller;
import com.order.inventory.entity.InventoryEntity;
import com.order.inventory.repository.InventoryRepository;
import com.order.inventory.service.InventoryConsumer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap; import java.util.List; import java.util.Map;
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:3000")

public class InventoryController {
    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryConsumer inventoryConsumer;

//    @Autowired
//    private CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/records")
    public List<InventoryEntity> getAll() {
        return inventoryRepository.findAll();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("processed", inventoryConsumer.getProcessedCount(),
                      "failed", inventoryConsumer.getFailedCount(), "status", "Inventory running");
    }

    @GetMapping("/health") public Map<String, Object> health() {
        return Map.of("service", "order-inventory-service", "status", "UP", "port", 8081);
    }

//    @GetMapping("/circuit-status")
//    public Map<String, Object> circuitStatus() {
//
//        var cb = circuitBreakerRegistry
//                .circuitBreaker("paymentService");
//
//        return Map.of(
//                "service", "paymentService",
//                "state", cb.getState().name()
//        );
//    }
}
