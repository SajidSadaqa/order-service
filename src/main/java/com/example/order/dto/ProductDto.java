package com.example.order.dto;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String name,
        BigDecimal price,
        Integer stock
) {}