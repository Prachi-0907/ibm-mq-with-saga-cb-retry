package com.order.payment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="payment_events")
public class PaymentEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String eventType;
    private LocalDateTime createdAt;
}