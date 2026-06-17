package com.smartretailx.inventory.service;

import com.smartretailx.inventory.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void sendLowStockAlert(Product product) {
        log.warn(
            "[LOW STOCK ALERT] Product: '{}' (SKU: {}) | Current Stock: {} | " +
            "Threshold: {} | Supplier: {}",
            product.getName(),
            product.getSku(),
            product.getStockQuantity(),
            product.getLowStockThreshold(),
            product.getSupplierId() != null ? product.getSupplierId() : "N/A"
        );

        // --- Production extension point ---
        // kafkaTemplate.send("inventory.low-stock", buildAlertEvent(product));
        // emailClient.sendAlert(supplierEmail, buildEmailBody(product));
    }

    public void sendRestockNotification(Product product, int previousQty) {
        log.info(
            "[RESTOCK NOTIFICATION] Product: '{}' (SKU: {}) restocked from {} to {} units.",
            product.getName(),
            product.getSku(),
            previousQty,
            product.getStockQuantity()
        );
    }

    public void sendOutOfStockAlert(Product product) {
        log.error(
            "[OUT OF STOCK] Product: '{}' (SKU: {}) is now OUT OF STOCK. " +
            "Immediate restock required. Supplier: {}",
            product.getName(),
            product.getSku(),
            product.getSupplierId() != null ? product.getSupplierId() : "N/A"
        );
    }
}
