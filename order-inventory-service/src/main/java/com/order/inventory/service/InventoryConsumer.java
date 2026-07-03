package com.order.inventory.service;

import com.order.inventory.entity.InventoryEntity;
import com.order.inventory.model.OrderMessage;
import com.order.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class InventoryConsumer {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${app.mq.dead-letter-queue:DEV.DEAD.LETTER.QUEUE}")
    private String dlq;

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);

    @JmsListener(destination = "${app.mq.order-queue}", containerFactory = "jmsListenerContainerFactory")
    public void receiveOrder(OrderMessage order) {
        log.info("INVENTORY received: orderId={}, product={}, qty={}", order.getOrderId(), order.getProduct(), order.getQuantity());
        try {
            // Step 1: Validate
            if (order.getQuantity() <= 0) throw new RuntimeException("Invalid quantity: " + order.getQuantity());

            // Step 2: Reserve inventory
            InventoryEntity inv = new InventoryEntity();
            inv.setOrderId(order.getOrderId());
            inv.setProduct(order.getProduct());
            inv.setQuantity(order.getQuantity());
            inv.setStatus("RESERVED");
            inventoryRepository.save(inv);
            log.info("Inventory reserved for orderId={}", order.getOrderId());

            // Step 3: Call Payment service (Circuit Breaker here)
            // Step 3: Call Payment service (Circuit Breaker here)
            paymentClient.callPayment(order);

            // Step 4: Forward order to Payment Queue
//            jmsTemplate.convertAndSend(
//                    "DEV.PAYMENT.Q",
//                    order
//            );

            processedCount.incrementAndGet();

//            log.info(
//                    "Inventory forwarded order {} to DEV.PAYMENT.Q",
//                    order.getOrderId()
//            );

        } catch (Exception e) {

            log.error(
                    "Inventory failed {}",
                    order.getOrderId()
            );

            failedCount.incrementAndGet();

            jmsTemplate.convertAndSend(
                    "DEV.ORDER.FAILED",
                    order
            );

            jmsTemplate.convertAndSend(
                    dlq,
                    order
            );

            log.info(
                    "Sent to compensation queue {}",
                    order.getOrderId()
            );
        }
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public int getFailedCount() {
        return failedCount.get();
    }
}
