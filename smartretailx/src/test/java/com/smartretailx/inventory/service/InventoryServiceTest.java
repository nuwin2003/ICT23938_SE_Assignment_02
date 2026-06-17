package com.smartretailx.inventory.service;

import com.smartretailx.inventory.dto.InventoryDTO.*;
import com.smartretailx.inventory.exception.InventoryExceptions.*;
import com.smartretailx.inventory.model.Product;
import com.smartretailx.inventory.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InventoryService inventoryService;

    // ---------------------------------------------------------------
    // Test Fixtures
    // ---------------------------------------------------------------

    private Product buildSampleProduct(String productId, int stockQty, int lowStockThreshold) {
        return Product.builder()
                .productId(productId)
                .name("Test Product")
                .sku("SKU-001")
                .category("Beverages")
                .price(new BigDecimal("250.00"))
                .stockQuantity(stockQty)
                .lowStockThreshold(lowStockThreshold)
                .supplierId("SUPPLIER-001")
                .build();
    }

    // ---------------------------------------------------------------
    // createProduct Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("createProduct()")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully when SKU is unique")
        void createProduct_uniqueSku_returnsProductResponse() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Orange Juice")
                    .sku("SKU-OJ-001")
                    .category("Beverages")
                    .price(new BigDecimal("150.00"))
                    .initialStock(100)
                    .lowStockThreshold(10)
                    .supplierId("SUPPLIER-001")
                    .build();

            Product savedProduct = buildSampleProduct("P-001", 100, 10);
            savedProduct.setSku("SKU-OJ-001");
            savedProduct.setName("Orange Juice");

            when(productRepository.existsBySku("SKU-OJ-001")).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductResponse response = inventoryService.createProduct(request);

            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Orange Juice");
            assertThat(response.getSku()).isEqualTo("SKU-OJ-001");
            assertThat(response.isLowStock()).isFalse();

            verify(productRepository, times(1)).save(any(Product.class));
            verifyNoInteractions(notificationService); // no alert for adequate stock
        }

        @Test
        @DisplayName("Should throw DuplicateSkuException when SKU already exists")
        void createProduct_duplicateSku_throwsDuplicateSkuException() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Milk")
                    .sku("SKU-EXISTING")
                    .category("Dairy")
                    .price(new BigDecimal("100.00"))
                    .initialStock(50)
                    .lowStockThreshold(5)
                    .build();

            when(productRepository.existsBySku("SKU-EXISTING")).thenReturn(true);

            assertThatThrownBy(() -> inventoryService.createProduct(request))
                    .isInstanceOf(DuplicateSkuException.class)
                    .hasMessageContaining("SKU-EXISTING");

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should send low stock alert when initial stock is below threshold")
        void createProduct_initialStockBelowThreshold_sendsLowStockAlert() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Butter")
                    .sku("SKU-BUTTER")
                    .category("Dairy")
                    .price(new BigDecimal("300.00"))
                    .initialStock(3)       // below threshold
                    .lowStockThreshold(10)
                    .supplierId("SUPPLIER-002")
                    .build();

            Product savedProduct = buildSampleProduct("P-002", 3, 10);

            when(productRepository.existsBySku("SKU-BUTTER")).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            inventoryService.createProduct(request);

            verify(notificationService, times(1)).sendLowStockAlert(any(Product.class));
        }
    }

    // ---------------------------------------------------------------
    // updateStock Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("updateStock()")
    class UpdateStockTests {

        @Test
        @DisplayName("Should update stock and return correct before/after values")
        void updateStock_validRequest_returnsStockUpdateResponse() {
            Product product = buildSampleProduct("P-001", 50, 10);
            UpdateStockRequest request = new UpdateStockRequest(80, "RESTOCK");

            when(productRepository.findById("P-001")).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            StockUpdateResponse response = inventoryService.updateStock("P-001", request);

            assertThat(response.getPreviousQuantity()).isEqualTo(50);
            assertThat(response.getNewQuantity()).isEqualTo(80);
            assertThat(response.isLowStockAlertTriggered()).isFalse();
        }

        @Test
        @DisplayName("Should trigger low stock alert when new quantity is below threshold")
        void updateStock_newQuantityBelowThreshold_triggersLowStockAlert() {
            Product product = buildSampleProduct("P-001", 50, 10);
            UpdateStockRequest request = new UpdateStockRequest(5, "DAMAGE_WRITE_OFF");

            when(productRepository.findById("P-001")).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            StockUpdateResponse response = inventoryService.updateStock("P-001", request);

            assertThat(response.isLowStockAlertTriggered()).isTrue();
            verify(notificationService, times(1)).sendLowStockAlert(any(Product.class));
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException for unknown product ID")
        void updateStock_unknownProductId_throwsProductNotFoundException() {
            when(productRepository.findById("UNKNOWN-ID")).thenReturn(Optional.empty());

            UpdateStockRequest request = new UpdateStockRequest(50, "RESTOCK");

            assertThatThrownBy(() -> inventoryService.updateStock("UNKNOWN-ID", request))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("UNKNOWN-ID");
        }
    }

    // ---------------------------------------------------------------
    // adjustStock Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("adjustStock()")
    class AdjustStockTests {

        @Test
        @DisplayName("Should deduct stock successfully when sufficient stock available")
        void adjustStock_sufficientStock_deductsCorrectly() {
            Product product = buildSampleProduct("P-001", 50, 10);
            AdjustStockRequest request = new AdjustStockRequest(-20, "ORDER_PLACED");

            when(productRepository.findById("P-001")).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            StockUpdateResponse response = inventoryService.adjustStock("P-001", request);

            assertThat(response.getPreviousQuantity()).isEqualTo(50);
            assertThat(response.getNewQuantity()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should throw InsufficientStockException when deduction exceeds available stock")
        void adjustStock_deductionExceedsStock_throwsInsufficientStockException() {
            Product product = buildSampleProduct("P-001", 10, 5);
            AdjustStockRequest request = new AdjustStockRequest(-20, "ORDER_PLACED");

            when(productRepository.findById("P-001")).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> inventoryService.adjustStock("P-001", request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("Should send out-of-stock alert when stock reaches zero")
        void adjustStock_stockReachesZero_sendsOutOfStockAlert() {
            Product product = buildSampleProduct("P-001", 10, 5);
            AdjustStockRequest request = new AdjustStockRequest(-10, "ORDER_PLACED");

            when(productRepository.findById("P-001")).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            inventoryService.adjustStock("P-001", request);

            verify(notificationService, times(1)).sendOutOfStockAlert(any(Product.class));
        }

        @Test
        @DisplayName("Should add stock correctly for positive delta")
        void adjustStock_positiveDetla_addsStockCorrectly() {
            Product product = buildSampleProduct("P-001", 5, 10);
            AdjustStockRequest request = new AdjustStockRequest(50, "RESTOCK");

            when(productRepository.findById("P-001")).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            StockUpdateResponse response = inventoryService.adjustStock("P-001", request);

            assertThat(response.getNewQuantity()).isEqualTo(55);
        }
    }

    // ---------------------------------------------------------------
    // getLowStockProducts Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getLowStockProducts()")
    class LowStockTests {

        @Test
        @DisplayName("Should return all products below low stock threshold")
        void getLowStockProducts_existingLowStockItems_returnsCorrectList() {
            List<Product> lowStockList = List.of(
                    buildSampleProduct("P-001", 3, 10),
                    buildSampleProduct("P-002", 5, 20)
            );

            when(productRepository.findAllLowStockProducts()).thenReturn(lowStockList);

            ProductListResponse response = inventoryService.getLowStockProducts();

            assertThat(response.getTotalCount()).isEqualTo(2);
            assertThat(response.getProducts()).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no products are low on stock")
        void getLowStockProducts_noLowStockItems_returnsEmptyList() {
            when(productRepository.findAllLowStockProducts()).thenReturn(List.of());

            ProductListResponse response = inventoryService.getLowStockProducts();

            assertThat(response.getTotalCount()).isZero();
            assertThat(response.getProducts()).isEmpty();
        }
    }
}
