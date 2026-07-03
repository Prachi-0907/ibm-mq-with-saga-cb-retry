package com.order.inventory.entity;
import jakarta.persistence.*;
import lombok.Data; import java.time.LocalDateTime;
@Entity
@Table(name = "inventory_records")
@Data

public class InventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id") private String orderId;
    @Column(name = "product") private String product;
    @Column(name = "quantity") private int quantity;
    @Column(name = "status") private String status;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
}
