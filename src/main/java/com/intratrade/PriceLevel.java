package com.intratrade;

import java.math.BigDecimal;

public record PriceLevel(String source, BigDecimal price, long quantity) {}
