package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.Order;
import com.github.barbitoff.trading.traderacoon.api.model.OrderStatus;
import com.github.barbitoff.trading.traderacoon.api.model.StockOrderType;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.OrderRejectedException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import com.github.barbitoff.trading.traderacoon.api.service.OrderService;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config.TinkoffOpenApiConfiguration;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.model.mappers.OrderMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.orders.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * OrderService implementation, based on Tinkoff API
 */
@AllArgsConstructor
@Slf4j
@Service(TinkoffOpenApiConfiguration.BEANS_QUALIFIER_PREFIX + "OrderService")
public class OrderServiceImpl implements OrderService {
    private final OpenApi api;
    private final AccountService accountService;
    private final OrderMapper orderMapper;

    @Override
    public List<Order> getActiveOrders() throws AccountNotFoundException, TradingApiException {
        log.info("Getting orders list");
        try {
            List<ru.tinkoff.invest.openapi.models.orders.Order> orders = api.getOrdersContext()
                    .getOrders(accountService.getTradingAccount().getId())
                    .get();
            if (orders == null) {
                orders = Collections.emptyList();
            }
            log.info("Got {} orders", orders.size());
            return orders.stream()
                    .map(orderMapper::mapTinkoffOrder)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException ex) {
            throw new TradingApiException("Error getting orders list", ex);
        }
    }

    @Override
    public void cancelOrders(List<Order> orders) throws AccountNotFoundException, TradingApiException {
        log.info("Cancelling {} orders", orders.size());
        String accountId = accountService.getTradingAccount().getId();
        try {
            for (Order order : orders) {
                log.info("Cancelling order {}", order.getId());
                if (order.getStatus() != OrderStatus.New && order.getStatus() != OrderStatus.PendingNew
                        && order.getStatus() != OrderStatus.PartiallyFill) {
                    // TODO: make audit here
                    log.info("Order {} has status {}, can't cancel", order.getId(), order.getStatus());
                } else {
                    // TODO: make audit here
                    CompletableFuture<Void> future = api.getOrdersContext().cancelOrder(
                            order.getId(),
                            accountId);
                    future.get();
                    log.info("Cancelled order {}", order.getId());
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            throw new TradingApiException("Error cancelling orders", ex);
        }
    }

    @Override
    public Order buy(String figi, int lots, BigDecimal price) throws TradingApiException, OrderRejectedException, AccountNotFoundException {
        log.info("Buying {}, {} lots, with price {}", figi, lots, price);
        return orderMapper.mapTinkoffOrder(
                placeOrder(figi, lots, price, Operation.Buy),
                figi,
                price != null ? StockOrderType.Limit : StockOrderType.Market,
                price
        );
    }

    @Override
    public Order sell(String figi, int lots, BigDecimal price) throws TradingApiException, OrderRejectedException, AccountNotFoundException {
        log.info("Selling {}, {} lots, with price {}", figi, lots, price);
        return orderMapper.mapTinkoffOrder(
                placeOrder(figi, lots, price, Operation.Sell),
                figi,
                price != null ? StockOrderType.Limit : StockOrderType.Market,
                price
        );
    }

    /**
     * Place limited or market order for buying/selling something
     *
     * @param figi      Figi code of the instrument
     * @param lots      number of lots
     * @param price     price for the limited order. If empty - market order is created
     * @param operation operation to perform
     * @throws TradingApiException    if the API throws an exception
     * @throws OrderRejectedException if the order is rejected for some reason
     */
    private PlacedOrder placeOrder(String figi, int lots, BigDecimal price, Operation operation)
            throws TradingApiException, OrderRejectedException, AccountNotFoundException {
        // TODO: make audit here
        String accountId = accountService.getTradingAccount().getId();
        PlacedOrder placedOrder;
        try {
            if (price == null) { // place market order
                MarketOrder order = new MarketOrder(lots, operation);
                placedOrder = api.getOrdersContext().placeMarketOrder(figi, order, accountId).get();
            } else { // place LimitOrder
                LimitOrder order = new LimitOrder(lots, operation, price);
                placedOrder = api.getOrdersContext().placeLimitOrder(figi, order, accountId).get();
            }
        } catch (ExecutionException | InterruptedException ex) {
            throw new TradingApiException("Error sending '" + operation.name() + "' order", ex);
        }
        if (placedOrder.status == Status.Rejected) {
            // TODO: make audit here
            throw new OrderRejectedException(placedOrder.rejectReason);
        }
        // TODO: make audit here, collect metrics (on commission and cash flow)
        log.info("The order is placed");
        return placedOrder;
    }
}
