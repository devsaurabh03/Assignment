package com.intratrade;

import java.math.BigDecimal;
import java.util.List;

interface IPriceBook {
    void reset();
    void update(List<MarketData> messages);
    BigDecimal getVwapForQuantityAndSide(long quantity, Side side);
    long getTotalQuantityForPriceAndSide(BigDecimal price, Side side);
}
