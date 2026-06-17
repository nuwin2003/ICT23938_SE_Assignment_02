package com.smartretailx.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateProductRequest {

        @NotBlank(message = "Product name is required")
        private String name;

        @NotBlank(message = "SKU is required")
        private String sku;

        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
        private BigDecimal price;

        @Min(value = 0, message = "Initial stock cannot be negative")
        private int initialStock;

        @Min(value = 0, message = "Low stock threshold cannot be negative")
        private int lowStockThreshold;

        private String supplierId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStockRequest {

        @Min(value = 0, message = "Stock quantity cannot be negative")
        @NotNull(message = "New quantity is required")
        private Integer newQuantity;

        private String updateReason; // e.g., "RESTOCK", "SALE", "DAMAGE_WRITE_OFF"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustStockRequest {

        @NotNull(message = "Quantity delta is required")
        private Integer quantityDelta; // positive = add, negative = deduct

        @NotBlank(message = "Reason is required")
        private String reason; // e.g., "ORDER_PLACED", "ORDER_CANCELLED", "RESTOCK"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductResponse {

        private String productId;
        private String name;
        private String sku;
        private String category;
        private BigDecimal price;
        private int stockQuantity;
        private int lowStockThreshold;
        private boolean lowStock;
        private String supplierId;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockUpdateResponse {

        private String productId;
        private String productName;
        private int previousQuantity;
        private int newQuantity;
        private boolean lowStockAlertTriggered;
        private String message;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductListResponse {

        private java.util.List<ProductResponse> products;
        private int totalCount;
        private String message;
    }
}
