package com.intratrade;

import java.math.BigDecimal;

record PriceLevel(String source, BigDecimal price, long quantity) {}
