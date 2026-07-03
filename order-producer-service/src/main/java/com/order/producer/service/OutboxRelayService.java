package com.order.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.producer.entity.OutboxEvent;
import com.order.producer.model.OrderMessage;
import com.order.producer.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class OutboxRelayService {

    @Autowired
    private OutboxEventRepository outboxRepo;

    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    private ObjectMapper objectMapper;



    // Runs every 5 seconds — picks up PENDING events and publishes to MQ

    @Scheduled(fixedDelay = 10000)
    public void relay() {
        List<OutboxEvent> pending = outboxRepo.findByStatus("PENDING");
        if (pending.isEmpty()) return;

        log.info("Outbox relay: found {} PENDING events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                OrderMessage order = objectMapper.readValue(event.getPayload(), OrderMessage.class);
                orderProducer.sendOrder(order);

                event.setStatus("SENT");
                event.setSentAt(LocalDateTime.now());
                outboxRepo.save(event);

                log.info("Outbox relay: published orderId={}", event.getOrderId());

            }

            catch (Exception e) {
                log.error("Outbox relay FAILED for orderId={}: {}", event.getOrderId(), e.getMessage());
                event.setStatus("FAILED");
                outboxRepo.save(event);
            }
        }
    }
}
