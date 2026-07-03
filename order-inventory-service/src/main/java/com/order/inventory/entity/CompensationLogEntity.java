package com.order.inventory.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="compensation_log")
public class CompensationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String action;
    private LocalDateTime createdAt;
}
