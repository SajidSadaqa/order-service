package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderDto;
import com.example.order.dto.ProductDto;
import com.example.order.entity.OrderEntity;
import com.example.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient productServiceClient;

    public OrderService(OrderRepository orderRepository, RestClient productServiceClient) {
        this.orderRepository = orderRepository;
        this.productServiceClient = productServiceClient;
    }

    public OrderDto createOrder(CreateOrderRequest request) {
        // Fetch product information
        ProductDto product = fetchProduct(request.productId());

        // Check stock availability
        if (product.stock() < request.quantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        }

        // Decrease stock in product service
        decreaseProductStock(request.productId(), request.quantity());

        // Calculate total price and save order
        BigDecimal totalPrice = product.price().multiply(BigDecimal.valueOf(request.quantity()));
        OrderEntity order = new OrderEntity(request.productId(), request.quantity(), totalPrice);
        OrderEntity saved = orderRepository.save(order);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return mapToDto(order);
    }

    private ProductDto fetchProduct(Long productId) {
        try {
            return productServiceClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            (request, response) -> {
                                if (response.getStatusCode().value() == 404) {
                                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                                }
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product service error");
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            (request, response) -> {
                                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable");
                            })
                    .body(ProductDto.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to connect to product service");
        }
    }

    private void decreaseProductStock(Long productId, Integer quantity) {
        try {
            productServiceClient.patch()
                    .uri("/api/products/{id}/decrease-stock?qty={qty}", productId, quantity)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            (request, response) -> {
                                if (response.getStatusCode().value() == 409) {
                                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
                                }
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product service error");
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            (request, response) -> {
                                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable");
                            })
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to connect to product service");
        }
    }

    private OrderDto mapToDto(OrderEntity order) {
        return new OrderDto(order.getId(), order.getProductId(),
                order.getQuantity(), order.getTotalPrice());
    }
}