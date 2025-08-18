package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testorderdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OrderServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static MockWebServer mockProductService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        mockProductService = new MockWebServer();
        mockProductService.start();
        registry.add("product-service.base-url", () -> mockProductService.url("/").toString());
    }

    @BeforeEach
    void setUp() {
        // Clear any previous requests
        while (mockProductService.getRequestCount() > 0) {
            try {
                mockProductService.takeRequest();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockProductService != null) {
            mockProductService.shutdown();
        }
    }

    @Test
    void createOrder_HappyPath_ShouldCreateOrderSuccessfully() throws Exception {
        // Mock product service responses
        mockProductService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(
                        new com.example.order.dto.ProductDto(1L, "Test Product", new BigDecimal("15.00"), 10)))
                .addHeader("Content-Type", "application/json"));

        mockProductService.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));

        CreateOrderRequest request = new CreateOrderRequest(1L, 3);

        ResponseEntity<OrderDto> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders",
                request,
                OrderDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().productId()).isEqualTo(1L);
        assertThat(response.getBody().quantity()).isEqualTo(3);
        assertThat(response.getBody().totalPrice()).isEqualTo(new BigDecimal("45.00"));
    }

    @Test
    void createOrder_InsufficientStock_ShouldReturn409() throws Exception {
        // Mock product service response with low stock
        mockProductService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(
                        new com.example.order.dto.ProductDto(1L, "Test Product", new BigDecimal("15.00"), 2)))
                .addHeader("Content-Type", "application/json"));

        CreateOrderRequest request = new CreateOrderRequest(1L, 5);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}