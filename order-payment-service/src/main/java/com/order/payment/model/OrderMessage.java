package com.order.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {
    private String orderId; private String customerName; private String product;
    private int quantity; private double totalAmount; private LocalDateTime createdAt;
    private String status; private String priority;
}
