package com.order.producer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="orders")
public class OrderEntity {

    @Id
    private String orderId;

    private String product;
    private Integer quantity;
    private String status; // PENDING, CONFIRMED, FAILED
}