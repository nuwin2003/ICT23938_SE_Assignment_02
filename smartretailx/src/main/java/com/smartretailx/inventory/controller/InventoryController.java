package com.smartretailx.inventory.controller;

import com.smartretailx.inventory.dto.InventoryDTO.*;
import com.smartretailx.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        log.info("POST /api/v1/inventory/products - Creating product: {}", request.getSku());
        ProductResponse response = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/products")
    public ResponseEntity<ProductListResponse> getAllProducts() {
        log.info("GET /api/v1/inventory/products - Retrieving all products");
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable String productId) {

        log.info("GET /api/v1/inventory/products/{}", productId);
        return ResponseEntity.ok(inventoryService.getProductById(productId));
    }

    @GetMapping("/products/category/{category}")
    public ResponseEntity<ProductListResponse> getProductsByCategory(
            @PathVariable String category) {

        log.info("GET /api/v1/inventory/products/category/{}", category);
        return ResponseEntity.ok(inventoryService.getProductsByCategory(category));
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<ProductListResponse> getLowStockProducts() {
        log.info("GET /api/v1/inventory/products/low-stock");
        return ResponseEntity.ok(inventoryService.getLowStockProducts());
    }

    @PutMapping("/products/{productId}/stock")
    public ResponseEntity<StockUpdateResponse> updateStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {

        log.info("PUT /api/v1/inventory/products/{}/stock - New quantity: {}",
                productId, request.getNewQuantity());
        return ResponseEntity.ok(inventoryService.updateStock(productId, request));
    }

    @PatchMapping("/products/{productId}/stock")
    public ResponseEntity<StockUpdateResponse> adjustStock(
            @PathVariable String productId,
            @Valid @RequestBody AdjustStockRequest request) {

        log.info("PATCH /api/v1/inventory/products/{}/stock - Delta: {}",
                productId, request.getQuantityDelta());
        return ResponseEntity.ok(inventoryService.adjustStock(productId, request));
    }
}
