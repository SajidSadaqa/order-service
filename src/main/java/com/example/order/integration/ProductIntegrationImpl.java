package com.example.order.integration;

import com.example.order.domain.response.ProductRes;
import com.example.order.exception.InsufficientStockException;
import com.example.order.exception.ProductNotFoundException;
import com.example.order.exception.ProductServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductIntegrationImpl implements ProductIntegrationClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductIntegrationImpl.class);

    private final RestClient productServiceClient;

    public ProductIntegrationImpl(RestClient productServiceClient) {
        this.productServiceClient = productServiceClient;
    }

    @Override
    public ProductRes getProduct(Long productId) {
        logger.debug("Fetching product with id {}", productId);

        try {
            return productServiceClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> {
                                throw new ProductNotFoundException(productId);
                            })
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new ProductServiceException("Product service client error: " + response.getStatusCode());
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new ProductServiceException("Product service server error: " + response.getStatusCode());
                            })
                    .body(ProductRes.class);
        } catch (RestClientException ex) {
            logger.error("Failed to connect to product service", ex);
            throw new ProductServiceException("Unable to connect to product service", ex);
        }
    }

    @Override
    public void decreaseProductStock(Long productId, Integer quantity) {
        logger.debug("Decreasing stock for product {} by quantity {}", productId, quantity);

        try {
            productServiceClient.patch()
                    .uri("/api/products/{id}/decrease-stock?qty={qty}", productId, quantity)
                    .retrieve()
                    .onStatus(status -> status.value() == 409,
                            (request, response) -> {
                                throw new InsufficientStockException("Insufficient stock for product " + productId);
                            })
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new ProductServiceException("Product service client error: " + response.getStatusCode());
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new ProductServiceException("Product service server error: " + response.getStatusCode());
                            })
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            logger.error("Failed to decrease product stock", ex);
            throw new ProductServiceException("Unable to connect to product service", ex);
        }
    }
}
