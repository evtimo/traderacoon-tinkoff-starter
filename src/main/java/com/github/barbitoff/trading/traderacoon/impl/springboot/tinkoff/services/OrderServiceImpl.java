package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.Order;
import com.github.barbitoff.trading.traderacoon.api.model.exception.OrderRejectedException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * OrderService implementation, based on Tinkoff API
 */
public class OrderServiceImpl implements OrderService {
    @Override
    public List<Order> getActiveOrders() throws TradingApiException {
        return null;
    }

    @Override
    public void cancelOrders(List<Order> list) throws TradingApiException {

    }

    @Override
    public Order buy(String s, int i, BigDecimal bigDecimal) throws TradingApiException, OrderRejectedException {
        return null;
    }

    @Override
    public Order sell(String s, int i, Optional<BigDecimal> optional) throws TradingApiException, OrderRejectedException {
        return null;
    }
}
