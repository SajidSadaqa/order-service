package com.example.order.domain.response;

import java.math.BigDecimal;

public record OrderRes(
        Long id,
        Long productId,
        Integer quantity,
        BigDecimal totalPrice
) {}