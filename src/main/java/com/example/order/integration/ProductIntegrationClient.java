package com.example.order.integration;

import com.example.order.domain.response.ProductRes;
import com.example.order.exception.InsufficientStockException;
import com.example.order.exception.ProductNotFoundException;
import com.example.order.exception.ProductServiceException;

/**
 * Client interface for product service integration
 */
public interface ProductIntegrationClient {

    /**
     * Retrieves product information by ID
     * @param productId the product ID
     * @return product response
     * @throws ProductNotFoundException if product is not found
     * @throws ProductServiceException if service is unavailable
     */
    ProductRes getProduct(Long productId);

    /**
     * Decreases product stock
     * @param productId the product ID
     * @param quantity the quantity to decrease
     * @throws InsufficientStockException if not enough stock
     * @throws ProductServiceException if service is unavailable
     */
    void decreaseProductStock(Long productId, Integer quantity);
}