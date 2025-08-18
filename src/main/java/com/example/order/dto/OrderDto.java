package com.example.order.dto;

import java.math.BigDecimal;

public record OrderDto(
        Long id,
        Long productId,
        Integer quantity,
        BigDecimal totalPrice
) {}