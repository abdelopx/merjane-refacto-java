package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.services.ProductService;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@UnitTest
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private Set<Product> products;

    @BeforeEach
    void setUp() {
        products = new HashSet<>();
        products.add(createProduct(1L, ProductType.NORMAL, "Test1", 10));
        products.add(createProduct(2L, ProductType.SEASONAL, "test2", 5));
        products.add(createProduct(3L, ProductType.EXPIRABLE, "test3", 8));
        order = new Order();
        order.setId(1L);
        order.setItems(products);
    }

    @Test
    @DisplayName("Should process order successfully when order exists")
    void shouldProcessOrderSuccessfully() {
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(productService).processProduct(any(Product.class));

        ProcessOrderResponse response = orderService.processOrder(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        verify(orderRepository, times(1)).findById(orderId);
        verify(productService, times(3)).processProduct(any(Product.class));
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderService.processOrder(orderId));

        assertEquals("Order not found with id: " + orderId, exception.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
        verify(productService, never()).processProduct(any(Product.class));
    }

    @Test
    @DisplayName("Should process order with only normal products")
    void shouldProcessOrderWithOnlyNormalProducts() {
        Long orderId = 1L;
        Set<Product> normalProducts = new HashSet<>();
        normalProducts.add(createProduct(1L, ProductType.NORMAL, "Test1", 10));
        normalProducts.add(createProduct(2L, ProductType.NORMAL, "Test2", 5));

        Order normalOrder = new Order();
        normalOrder.setId(orderId);
        normalOrder.setItems(normalProducts);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(normalOrder));
        doNothing().when(productService).processProduct(any(Product.class));

        ProcessOrderResponse response = orderService.processOrder(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        verify(productService, times(2)).processProduct(any(Product.class));
    }

    @Test
    @DisplayName("Should process order with empty product list")
    void shouldProcessOrderWithEmptyProductList() {
        Long orderId = 1L;
        Order emptyOrder = new Order();
        emptyOrder.setId(orderId);
        emptyOrder.setItems(new HashSet<>());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(emptyOrder));

        ProcessOrderResponse response = orderService.processOrder(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        verify(productService, never()).processProduct(any(Product.class));
    }

    private Product createProduct(Long id, ProductType type, String name, Integer available) {
        Product product = new Product();
        product.setId(id);
        product.setType(type);
        product.setName(name);
        product.setAvailable(available);
        product.setLeadTime(15);
        return product;
    }
}

