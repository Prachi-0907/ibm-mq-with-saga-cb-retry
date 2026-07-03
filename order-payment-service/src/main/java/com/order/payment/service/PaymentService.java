package com.order.payment.service;
import com.order.payment.entity.PaymentEntity;
import com.order.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service @Slf4j
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationClient notificationClient;

    public String processPayment(String orderId, double amount, String product) {
        log.info("Processing payment for orderId={}, amount={}", orderId, amount);
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setProduct(product);
        payment.setStatus("SUCCESS");

        if(paymentRepository.existsByOrderId(orderId))
        {
            log.warn("Duplicate payment ignored for {}", orderId);
            return "Duplicate payment ignored";
        }

        paymentRepository.save(payment);
        log.info("Payment saved for orderId={}", orderId);

        // Call Notification service (CB protects this)
        notificationClient.notify(orderId, "PAYMENT_SUCCESS", amount);
        return "Payment processed for " + orderId;
    }
}