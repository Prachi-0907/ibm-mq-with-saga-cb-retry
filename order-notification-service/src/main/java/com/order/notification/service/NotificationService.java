package com.order.notification.service;
import com.order.notification.entity.NotificationEntity;
import com.order.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @CircuitBreaker(
            name = "notificationDb",
            fallbackMethod = "saveFallback"
    )
    @Retry(
            name = "notificationDbRetry"
    )
    public String sendNotification(
            String orderId,
            String status,
            double amount) {

        log.info("Sending notification for orderId={}, status={}", orderId, status);

        NotificationEntity n = new NotificationEntity();
        n.setOrderId(orderId);
        n.setMessage("Order " + orderId + " status: " + status + ". Amount: " + amount);
        n.setStatus("SENT");

        notificationRepository.save(n);

        log.info("Notification saved for orderId={}", orderId);

        return "Notification sent for " + orderId;
    }

    public String saveFallback(
            String orderId,
            String status,
            double amount,
            Throwable t) {

        log.error(
                "Notification DB unavailable for orderId={} reason={}",
                orderId,
                t.getMessage()
        );

        return "Notification DB unavailable";
    }

}