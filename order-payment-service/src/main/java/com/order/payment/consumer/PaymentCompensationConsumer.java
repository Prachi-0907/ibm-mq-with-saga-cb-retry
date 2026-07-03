package com.order.payment.consumer;

import com.order.payment.model.OrderMessage;
import com.order.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentCompensationConsumer {

    @Autowired
    private PaymentRepository paymentRepository;

    @JmsListener(destination = "DEV.ORDER.FAILED")
    public void compensate(OrderMessage order) {

        paymentRepository
                .findByOrderId(order.getOrderId())
                .ifPresent(payment -> {

                    payment.setStatus("CANCELLED");

                    paymentRepository.save(payment);

                    log.info(
                            "Payment cancelled {}",
                            order.getOrderId()
                    );
                });
    }
}