package com.smartretailx.inventory.service;

import com.smartretailx.inventory.dto.InventoryDTO.*;
import com.smartretailx.inventory.exception.InventoryExceptions.*;
import com.smartretailx.inventory.model.Product;
import com.smartretailx.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSku());

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .category(request.getCategory())
                .price(request.getPrice())
                .stockQuantity(request.getInitialStock())
                .lowStockThreshold(request.getLowStockThreshold())
                .supplierId(request.getSupplierId())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getProductId());

        // Check if initial stock is already low
        if (savedProduct.isLowStock()) {
            notificationService.sendLowStockAlert(savedProduct);
        }

        return mapToProductResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(String productId) {
        log.debug("Fetching product with ID: {}", productId);

        Product product = findProductOrThrow(productId);
        return mapToProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductListResponse getAllProducts() {
        List<Product> products = productRepository.findAll();
        List<ProductResponse> productResponses = products.stream()
                .map(this::mapToProductResponse)
                .toList();

        return ProductListResponse.builder()
                .products(productResponses)
                .totalCount(productResponses.size())
                .message("Products retrieved successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public ProductListResponse getProductsByCategory(String category) {
        List<Product> products = productRepository.findByCategory(category);
        List<ProductResponse> productResponses = products.stream()
                .map(this::mapToProductResponse)
                .toList();

        return ProductListResponse.builder()
                .products(productResponses)
                .totalCount(productResponses.size())
                .message("Products in category '" + category + "' retrieved successfully")
                .build();
    }

    @Transactional
    public StockUpdateResponse updateStock(String productId, UpdateStockRequest request) {
        log.info("Updating stock for product [{}] to {} units. Reason: {}",
                productId, request.getNewQuantity(), request.getUpdateReason());

        Product product = findProductOrThrow(productId);
        int previousQuantity = product.getStockQuantity();

        product.setStockQuantity(request.getNewQuantity());
        productRepository.save(product);

        boolean alertTriggered = handleStockNotifications(product, previousQuantity);

        log.info("Stock updated for product [{}]: {} → {} units",
                productId, previousQuantity, request.getNewQuantity());

        return StockUpdateResponse.builder()
                .productId(product.getProductId())
                .productName(product.getName())
                .previousQuantity(previousQuantity)
                .newQuantity(product.getStockQuantity())
                .lowStockAlertTriggered(alertTriggered)
                .message("Stock updated successfully")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public StockUpdateResponse adjustStock(String productId, AdjustStockRequest request) {
        log.info("Adjusting stock for product [{}] by {} units. Reason: {}",
                productId, request.getQuantityDelta(), request.getReason());

        Product product = findProductOrThrow(productId);
        int previousQuantity = product.getStockQuantity();
        int newQuantity = previousQuantity + request.getQuantityDelta();

        // Prevent negative stock
        if (newQuantity < 0) {
            throw new InsufficientStockException(productId,
                    Math.abs(request.getQuantityDelta()), previousQuantity);
        }

        product.setStockQuantity(newQuantity);
        productRepository.save(product);

        boolean alertTriggered = handleStockNotifications(product, previousQuantity);

        log.info("Stock adjusted for product [{}]: {} → {} units",
                productId, previousQuantity, newQuantity);

        return StockUpdateResponse.builder()
                .productId(product.getProductId())
                .productName(product.getName())
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .lowStockAlertTriggered(alertTriggered)
                .message("Stock adjusted successfully")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductListResponse getLowStockProducts() {
        List<Product> lowStockProducts = productRepository.findAllLowStockProducts();
        List<ProductResponse> productResponses = lowStockProducts.stream()
                .map(this::mapToProductResponse)
                .toList();

        log.info("Low stock report: {} products below threshold", productResponses.size());

        return ProductListResponse.builder()
                .products(productResponses)
                .totalCount(productResponses.size())
                .message(productResponses.size() + " product(s) are below low stock threshold")
                .build();
    }

    private Product findProductOrThrow(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private boolean handleStockNotifications(Product product, int previousQuantity) {
        boolean alertTriggered = false;

        if (product.getStockQuantity() == 0) {
            notificationService.sendOutOfStockAlert(product);
            alertTriggered = true;
        } else if (product.isLowStock()) {
            notificationService.sendLowStockAlert(product);
            alertTriggered = true;
        } else if (previousQuantity <= product.getLowStockThreshold()
                && product.getStockQuantity() > product.getLowStockThreshold()) {
            // Stock was previously low but has now been restocked above threshold
            notificationService.sendRestockNotification(product, previousQuantity);
        }

        return alertTriggered;
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .sku(product.getSku())
                .category(product.getCategory())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .lowStock(product.isLowStock())
                .supplierId(product.getSupplierId())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
