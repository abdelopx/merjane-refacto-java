package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@UnitTest
@DisplayName("ProductServiceImpl Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setLeadTime(15);
    }

    @Test
    @DisplayName("Should notify delay and save product when notifyDelay is called")
    void shouldNotifyDelayAndSaveProduct() {
        int leadTime = 20;
        product.setLeadTime(10);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.notifyDelay(leadTime, product);

        assertEquals(leadTime, product.getLeadTime());
        verify(productRepository, times(1)).save(product);
        verify(notificationService, times(1)).sendDelayNotification(leadTime, product.getName());
    }

    @Test
    @DisplayName("Should process normal product when available")
    void shouldProcessNormalProductWhenAvailable() {
        product.setType(ProductType.NORMAL);
        product.setAvailable(10);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processNormalProduct(product);

        assertEquals(9, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verify(notificationService, never()).sendDelayNotification(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should notify delay when normal product is not available")
    void shouldNotifyDelayWhenNormalProductNotAvailable() {
        product.setType(ProductType.NORMAL);
        product.setAvailable(0);
        product.setLeadTime(15);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processNormalProduct(product);

        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        verify(productRepository, times(1)).save(product);
        verify(notificationService, times(1)).sendDelayNotification(15, product.getName());
    }

    @Test
    @DisplayName("Should not notify delay when normal product is not available and leadTime is 0")
    void shouldNotNotifyDelayWhenNormalProductNotAvailableAndLeadTimeIsZero() {
        product.setType(ProductType.NORMAL);
        product.setAvailable(0);
        product.setLeadTime(0);

        productService.processNormalProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository, never()).save(product);
        verify(notificationService, never()).sendDelayNotification(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should process seasonal product when in season and available")
    void shouldProcessSeasonalProductWhenInSeasonAndAvailable() {
        product.setType(ProductType.SEASONAL);
        product.setAvailable(10);
        product.setSeasonStartDate(LocalDate.now().minusDays(10));
        product.setSeasonEndDate(LocalDate.now().plusDays(50));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processSeasonalProduct(product);

        assertEquals(9, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verify(notificationService, never()).sendOutOfStockNotification(anyString());
    }

    @Test
    @DisplayName("Should handle seasonal product when out of season")
    void shouldHandleSeasonalProductWhenOutOfSeason() {
        product.setType(ProductType.SEASONAL);
        product.setAvailable(10);
        product.setSeasonStartDate(LocalDate.now().plusDays(10));
        product.setSeasonEndDate(LocalDate.now().plusDays(50));
        product.setLeadTime(15);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processSeasonalProduct(product);

        verify(productRepository, atLeastOnce()).save(product);
        verify(notificationService, times(1)).sendOutOfStockNotification(product.getName());
    }

    @Test
    @DisplayName("Should handle seasonal product when season ends before delivery")
    void shouldHandleSeasonalProductWhenSeasonEndsBeforeDelivery() {
        product.setType(ProductType.SEASONAL);
        product.setAvailable(0);
        product.setSeasonStartDate(LocalDate.now().minusDays(10));
        product.setSeasonEndDate(LocalDate.now().plusDays(5));
        product.setLeadTime(15);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processSeasonalProduct(product);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(productCaptor.capture());
        assertEquals(0, productCaptor.getValue().getAvailable());
        verify(notificationService, times(1)).sendOutOfStockNotification(product.getName());
    }

    @Test
    @DisplayName("Should process expirable product when available and not expired")
    void shouldProcessExpirableProductWhenAvailableAndNotExpired() {
        product.setType(ProductType.EXPIRABLE);
        product.setAvailable(10);
        product.setExpiryDate(LocalDate.now().plusDays(10));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processExpirableProduct(product);

        assertEquals(9, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verify(notificationService, never()).sendExpirationNotification(anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should handle expired product when expired")
    void shouldHandleExpiredProductWhenExpired() {
        product.setType(ProductType.EXPIRABLE);
        product.setAvailable(10);
        product.setExpiryDate(LocalDate.now().minusDays(5));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processExpirableProduct(product);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(productCaptor.capture());
        assertEquals(0, productCaptor.getValue().getAvailable());
        verify(notificationService, times(1)).sendExpirationNotification(product.getName(), product.getExpiryDate());
    }

    @Test
    @DisplayName("Should handle expired product when not available")
    void shouldHandleExpiredProductWhenNotAvailable() {
        product.setType(ProductType.EXPIRABLE);
        product.setAvailable(0);
        product.setExpiryDate(LocalDate.now().plusDays(10));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processExpirableProduct(product);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(productCaptor.capture());
        assertEquals(0, productCaptor.getValue().getAvailable());
        verify(notificationService, times(1)).sendExpirationNotification(product.getName(), product.getExpiryDate());
    }

    @Test
    @DisplayName("Should handle seasonal product and notify delay when in season but not available")
    void shouldHandleSeasonalProductAndNotifyDelayWhenInSeasonButNotAvailable() {
        product.setType(ProductType.SEASONAL);
        product.setAvailable(0);
        product.setSeasonStartDate(LocalDate.now().minusDays(10));
        product.setSeasonEndDate(LocalDate.now().plusDays(50));
        product.setLeadTime(15);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.processSeasonalProduct(product);

        verify(productRepository, atLeastOnce()).save(product);
        verify(notificationService, times(1)).sendDelayNotification(15, product.getName());
    }
}

