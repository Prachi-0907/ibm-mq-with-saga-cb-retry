package com.order.notification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="notification_events")
public class NotificationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String eventType;
    private LocalDateTime createdAt;
}