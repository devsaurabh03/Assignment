package com.intratrade;

import java.math.BigDecimal;

public record MarketData(String source, String instrument, Side side, BigDecimal price, long quantity) {}

