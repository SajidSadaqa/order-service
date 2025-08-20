package com.example.order.domain.response;

import java.math.BigDecimal;

public record ProductRes(
        Long id,
        String name,
        BigDecimal price,
        Integer stock
) {}