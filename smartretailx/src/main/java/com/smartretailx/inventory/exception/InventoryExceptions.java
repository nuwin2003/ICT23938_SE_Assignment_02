package com.smartretailx.inventory.exception;


public class InventoryExceptions {

    public static class ProductNotFoundException extends RuntimeException {

        private final String productId;

        public ProductNotFoundException(String productId) {
            super("Product not found with ID: " + productId);
            this.productId = productId;
        }

        public String getProductId() {
            return productId;
        }
    }

    public static class InsufficientStockException extends RuntimeException {

        private final String productId;
        private final int requestedQuantity;
        private final int availableQuantity;

        public InsufficientStockException(String productId, int requestedQuantity, int availableQuantity) {
            super(String.format(
                "Insufficient stock for product [%s]. Requested: %d, Available: %d",
                productId, requestedQuantity, availableQuantity
            ));
            this.productId = productId;
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
        }

        public String getProductId() { return productId; }
        public int getRequestedQuantity() { return requestedQuantity; }
        public int getAvailableQuantity() { return availableQuantity; }
    }

    public static class DuplicateSkuException extends RuntimeException {

        private final String sku;

        public DuplicateSkuException(String sku) {
            super("A product with SKU [" + sku + "] already exists");
            this.sku = sku;
        }

        public String getSku() { return sku; }
    }

    public static class InvalidStockOperationException extends RuntimeException {

        public InvalidStockOperationException(String message) {
            super(message);
        }
    }
}
