package com.order.inventory.service;

import com.order.inventory.entity.InventoryEntity;
import com.order.inventory.model.OrderMessage;
import com.order.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SagaCompensationConsumer {

    @Autowired
    private InventoryRepository repository;

    @JmsListener(
            destination = "DEV.ORDER.FAILED"
    )
    public void compensate(
            OrderMessage order) {

        repository
                .findByOrderId(
                        order.getOrderId()
                )
                .ifPresent(inv -> {

                    inv.setStatus(
                            "RELEASED"
                    );

                    repository.save(inv);

                    log.info(
                            "Inventory Released {}",
                            order.getOrderId()
                    );
                });
    }
}