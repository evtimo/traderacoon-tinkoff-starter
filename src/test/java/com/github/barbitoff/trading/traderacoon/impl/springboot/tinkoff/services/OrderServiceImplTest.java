package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.*;
import com.github.barbitoff.trading.traderacoon.api.model.Order;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.OrderRejectedException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import com.github.barbitoff.trading.traderacoon.api.service.OrderService;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.model.mappers.OrderMapper;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.OrdersContext;
import ru.tinkoff.invest.openapi.models.orders.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private static final String ACCOUNT_ID = "AACOUNT_ID1";
    private static final String ORDER_ID = "ORDER_ID_1";
    private static final String FIGI = "FIGI1";

    @Mock
    private OpenApi api;
    @Mock
    private AccountService accountsService;
    @Mock
    private OrdersContext ordersCtx;
    @Mock
    private OrderMapper orderMapper;

    private OrderService ordersService;

    @BeforeEach
    void setUp() throws TradingApiException, AccountNotFoundException {
        MockitoAnnotations.initMocks(this);
        // create object under test
        ordersService = new OrderServiceImpl(api, accountsService, orderMapper);
        // setup AccountsService mock
        when(accountsService.getTradingAccount()).thenReturn(new TradingAccount(ACCOUNT_ID));
        // setup OpenApi mock
        when(api.getOrdersContext()).thenReturn(ordersCtx);
    }

    @Test
    void getAllOrdersNoOrders() throws TradingApiException, AccountNotFoundException {
        CompletableFuture<List<ru.tinkoff.invest.openapi.models.orders.Order>> emptyFuture = CompletableFuture.completedFuture(Collections.emptyList());
        when(ordersCtx.getOrders(eq(ACCOUNT_ID))).thenReturn(emptyFuture);

        List<Order> orders = ordersService.getActiveOrders();
        assertTrue(orders.isEmpty());
    }

    @Test
    void getAllOrdersNullFromApi() throws TradingApiException, AccountNotFoundException {
        CompletableFuture<List<ru.tinkoff.invest.openapi.models.orders.Order>> nullFuture = CompletableFuture.completedFuture(null);
        when(ordersCtx.getOrders(eq(ACCOUNT_ID))).thenReturn(nullFuture);

        List<Order> orders = ordersService.getActiveOrders();
        assertTrue(orders.isEmpty());
    }

    @Test
    void getAllOrders() throws TradingApiException, AccountNotFoundException {
        ru.tinkoff.invest.openapi.models.orders.Order tinkOrder = createNewTinkoffOrder();
        List<ru.tinkoff.invest.openapi.models.orders.Order> targetList = Collections.singletonList(tinkOrder);
        CompletableFuture<List<ru.tinkoff.invest.openapi.models.orders.Order>> future = CompletableFuture.completedFuture(targetList);

        Order order = createNewOrder();
        when(ordersCtx.getOrders(eq(ACCOUNT_ID))).thenReturn(future);
        when(orderMapper.mapTinkoffOrder(eq(tinkOrder))).thenReturn(order);

        List<Order> orders = ordersService.getActiveOrders();
        assertEquals(1, orders.size());
        assertEquals(order, orders.get(0));
    }

    @Test
    void getAllOrdersErrorInApi() throws AccountNotFoundException {
        Exception apiException = new IOException("Fake IO exception");
        CompletableFuture<List<ru.tinkoff.invest.openapi.models.orders.Order>> failedFuture = CompletableFuture.failedFuture(apiException);
        when(ordersCtx.getOrders(eq(ACCOUNT_ID))).thenReturn(failedFuture);

        try {
            ordersService.getActiveOrders();
            fail("Exception expected when a backing API throws an exception");
        } catch (TradingApiException ex) {
            assertEquals(apiException, ex.getCause().getCause());
        }
    }

    @Test
    void cancelOrdersEmptyList() throws TradingApiException, AccountNotFoundException {
        ordersService.cancelOrders(Collections.emptyList());
        verify(ordersCtx, never()).cancelOrder(any(), any());
    }

    @Test
    void cancelOrdersSingleNewOrder() throws TradingApiException, AccountNotFoundException {
        Order newOrder = createNewOrder();
        // emulate successful API call
        CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);
        when(ordersCtx.cancelOrder(eq(ORDER_ID), eq(ACCOUNT_ID))).thenReturn(successFuture);

        ordersService.cancelOrders(Collections.singletonList(newOrder));
        verify(ordersCtx).cancelOrder(eq(ORDER_ID), eq(ACCOUNT_ID));
    }

    @Test
    void cancelOrdersSingleNewOrderFail() throws AccountNotFoundException {
        Order newOrder = createNewOrder();
        IOException apiException = new IOException("Fake IO Exception");
        // emulate failed API call
        CompletableFuture<Void> failedFuture = CompletableFuture.failedFuture(apiException);
        when(ordersCtx.cancelOrder(eq(ORDER_ID), eq(ACCOUNT_ID))).thenReturn(failedFuture);

        try {
            ordersService.cancelOrders(Collections.singletonList(newOrder));
            fail("Exception expected when a backing API throws an exception");
        } catch (TradingApiException ex) {
            assertEquals(apiException, ex.getCause().getCause());
        }
    }

    @Test
    void cancelOrdersDifferentTypesSuccess() throws TradingApiException, AccountNotFoundException {
        CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);

        List<Order> orders = createOrdersOfAllTypes();

        // setup order ctx to successfully cancel "right" orders
        orders.stream().filter(ord -> ord.getStatus() == OrderStatus.New
                || ord.getStatus() == OrderStatus.PendingNew
                || ord.getStatus() == OrderStatus.PartiallyFill
        ).forEach(ord ->
                when(ordersCtx.cancelOrder(eq(ord.getId()), eq(ACCOUNT_ID))).thenReturn(successFuture)
        );

        // call method under test
        ordersService.cancelOrders(orders);

        // check that "right" orders were cancelled, for non-"right" cancel wasn't called
        orders.stream().filter(ord -> ord.getStatus() == OrderStatus.New
                || ord.getStatus() == OrderStatus.PendingNew
                || ord.getStatus() == OrderStatus.PartiallyFill
        ).forEach(ord ->
                verify(ordersCtx).cancelOrder(eq(ord.getId()), eq(ACCOUNT_ID))
        );
        orders.stream().filter(ord -> ord.getStatus() != OrderStatus.New
                && ord.getStatus() != OrderStatus.PendingNew
                && ord.getStatus() != OrderStatus.PartiallyFill
        ).forEach(ord ->
                verify(ordersCtx, never()).cancelOrder(eq(ord.getId()), eq(ACCOUNT_ID))
        );
    }

    @Test
    void cancelOrdersDifferentTypesOneFailed() {
        IOException apiException = new IOException("Emulated");
        CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> failedFuture = CompletableFuture.failedFuture(apiException);

        List<Order> orders = createOrdersOfAllTypes();

        // setup order ctx to successfully cancel "right" orders, but for partialy filled order - fail
        orders.stream().filter(ord -> ord.getStatus() == OrderStatus.New
                || ord.getStatus() == OrderStatus.PendingNew
        ).forEach(ord ->
                when(ordersCtx.cancelOrder(eq(ord.getId()), eq(ACCOUNT_ID))).thenReturn(successFuture)
        );
        orders.stream().filter(ord -> ord.getStatus() == OrderStatus.PartiallyFill)
                .forEach(ord ->
                        when(ordersCtx.cancelOrder(eq(ord.getId()), eq(ACCOUNT_ID))).thenReturn(failedFuture)
                );

        // call method under test
        TradingApiException factException = assertThrows(
                TradingApiException.class,
                () -> ordersService.cancelOrders(orders),
                "Expected exception when one cancellation fails"
        );
        assertEquals(apiException, factException.getCause().getCause());
    }

    @Test
    void cancelAllOrders() throws TradingApiException, AccountNotFoundException {
        CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);

        List<ru.tinkoff.invest.openapi.models.orders.Order> orders = createTinkoffOrdersOfAllTypes();

        // setup order ctx to successfully cancel "right" orders
        orders.stream().filter(ord -> ord.status == Status.New
                || ord.status == Status.PendingNew
                || ord.status == Status.PartiallyFill
        ).forEach(ord ->
                when(ordersCtx.cancelOrder(eq(ord.id), eq(ACCOUNT_ID))).thenReturn(successFuture)
        );

        CompletableFuture<List<ru.tinkoff.invest.openapi.models.orders.Order>> future = CompletableFuture.completedFuture(orders);

        // setup orderCtx to return preconfigured orders list
        when(ordersCtx.getOrders(eq(ACCOUNT_ID))).thenReturn(future);

        // configure mapper to return orders with the corresponding id and status
        orders.forEach(order -> when(orderMapper.mapTinkoffOrder(order)).thenReturn(
                createOrderWithIdAndStatus(order.id, OrderStatus.valueOf(order.status.name()))
        ));

        // call method under test
        ordersService.cancelAllOrders();

        // check that "right" orders were cancelled, for non-"right" cancel wasn't called
        orders.stream().filter(ord -> ord.status == Status.New
                || ord.status == Status.PendingNew
                || ord.status == Status.PartiallyFill
        ).forEach(ord ->
                verify(ordersCtx).cancelOrder(eq(ord.id), eq(ACCOUNT_ID))
        );
        orders.stream().filter(ord -> ord.status != Status.New
                && ord.status != Status.PendingNew
                && ord.status != Status.PartiallyFill
        ).forEach(ord ->
                verify(ordersCtx, never()).cancelOrder(eq(ord.id), eq(ACCOUNT_ID))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buyOrSellLimit(boolean isSell) throws OrderRejectedException, TradingApiException, AccountNotFoundException {
        BigDecimal price = BigDecimal.TEN;
        int lots = 12;

        PlacedOrder placedOrder = generatePlacedOrder(Status.Fill);
        CompletableFuture<PlacedOrder> future = CompletableFuture.completedFuture(placedOrder);
        when(ordersCtx.placeLimitOrder(any(), any(), any())).thenReturn(future);

        Order limitOrder = mock(Order.class);
        when(orderMapper.mapTinkoffOrder(eq(placedOrder), eq(FIGI), eq(StockOrderType.Limit), eq(price))).thenReturn(limitOrder);

        Order result;
        if (isSell) {
            result = ordersService.sell(FIGI, lots, price);
        } else {
            result = ordersService.buy(FIGI, lots, price);
        }
        assertSame(limitOrder, result);

        ArgumentCaptor<LimitOrder> orderCaptor = ArgumentCaptor.forClass(LimitOrder.class);
        verify(ordersCtx).placeLimitOrder(eq(FIGI), orderCaptor.capture(), eq(ACCOUNT_ID));
        assertEquals(lots, orderCaptor.getValue().lots);
        assertEquals(price, orderCaptor.getValue().price);
        assertEquals(isSell ? Operation.Sell : Operation.Buy, orderCaptor.getValue().operation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buyOrSellLimitRejected(boolean isSell) {
        BigDecimal price = BigDecimal.TEN;
        int lots = 12;

        PlacedOrder placedOrder = generatePlacedOrder(Status.Rejected);
        CompletableFuture<PlacedOrder> future = CompletableFuture.completedFuture(placedOrder);
        when(ordersCtx.placeLimitOrder(any(), any(), any())).thenReturn(future);

        assertThrows(OrderRejectedException.class,
                () -> {
                    if (isSell) {
                        ordersService.sell(FIGI, lots, price);
                    } else {
                        ordersService.buy(FIGI, lots, price);
                    }
                },
                "Expected exception when the order is rejected");

        ArgumentCaptor<LimitOrder> orderCaptor = ArgumentCaptor.forClass(LimitOrder.class);
        verify(ordersCtx).placeLimitOrder(eq(FIGI), orderCaptor.capture(), eq(ACCOUNT_ID));
        assertEquals(lots, orderCaptor.getValue().lots);
        assertEquals(price, orderCaptor.getValue().price);
        assertEquals(isSell ? Operation.Sell : Operation.Buy, orderCaptor.getValue().operation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buyOrSellMarket(boolean isSell) throws OrderRejectedException, TradingApiException, AccountNotFoundException {
        int lots = 12;

        PlacedOrder placedOrder = generatePlacedOrder(Status.Fill);
        CompletableFuture<PlacedOrder> future = CompletableFuture.completedFuture(placedOrder);
        when(ordersCtx.placeMarketOrder(any(), any(), any())).thenReturn(future);

        Order marketOrder = mock(Order.class);
        when(orderMapper.mapTinkoffOrder(eq(placedOrder), eq(FIGI), eq(StockOrderType.Market), isNull())).thenReturn(marketOrder);

        Order result;
        if (isSell) {
            result = ordersService.sell(FIGI, lots, null);
        } else {
            result = ordersService.buy(FIGI, lots, null);
        }
        assertSame(marketOrder, result);

        ArgumentCaptor<MarketOrder> orderCaptor = ArgumentCaptor.forClass(MarketOrder.class);
        verify(ordersCtx).placeMarketOrder(eq(FIGI), orderCaptor.capture(), eq(ACCOUNT_ID));
        assertEquals(lots, orderCaptor.getValue().lots);
        assertEquals(isSell ? Operation.Sell : Operation.Buy, orderCaptor.getValue().operation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buyOrSellMarketRejected(boolean isSell) {
        int lots = 12;

        PlacedOrder placedOrder = generatePlacedOrder(Status.Rejected);
        CompletableFuture<PlacedOrder> future = CompletableFuture.completedFuture(placedOrder);
        when(ordersCtx.placeMarketOrder(any(), any(), any())).thenReturn(future);

        assertThrows(OrderRejectedException.class,
                () -> {
                    if (isSell) {
                        ordersService.sell(FIGI, lots, null);
                    } else {
                        ordersService.buy(FIGI, lots, null);
                    }
                },
                "Expected exception when the order is rejected");

        ArgumentCaptor<MarketOrder> orderCaptor = ArgumentCaptor.forClass(MarketOrder.class);
        verify(ordersCtx).placeMarketOrder(eq(FIGI), orderCaptor.capture(), eq(ACCOUNT_ID));
        assertEquals(lots, orderCaptor.getValue().lots);
        assertEquals(isSell ? Operation.Sell : Operation.Buy, orderCaptor.getValue().operation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buyOrSellMarketException(boolean isSell) {
        int lots = 12;

        IOException apiException = new IOException("Fake");
        CompletableFuture<PlacedOrder> future = CompletableFuture.failedFuture(apiException);
        when(ordersCtx.placeMarketOrder(any(), any(), any())).thenReturn(future);

        TradingApiException factEx = assertThrows(TradingApiException.class,
                () -> {
                    if (isSell) {
                        ordersService.sell(FIGI, lots, null);
                    } else {
                        ordersService.buy(FIGI, lots, null);
                    }
                },
                "Expected exception when the API throws an Exception");
        assertEquals(apiException, factEx.getCause().getCause());

        ArgumentCaptor<MarketOrder> orderCaptor = ArgumentCaptor.forClass(MarketOrder.class);
        verify(ordersCtx).placeMarketOrder(eq(FIGI), orderCaptor.capture(), eq(ACCOUNT_ID));
        assertEquals(lots, orderCaptor.getValue().lots);
        assertEquals(isSell ? Operation.Sell : Operation.Buy, orderCaptor.getValue().operation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buyOrSellLimitException(boolean isSell) {
        BigDecimal price = BigDecimal.TEN;
        int lots = 12;

        IOException apiException = new IOException("Fake");
        CompletableFuture<PlacedOrder> future = CompletableFuture.failedFuture(apiException);
        when(ordersCtx.placeLimitOrder(any(), any(), any())).thenReturn(future);

        TradingApiException factEx = assertThrows(TradingApiException.class,
                () -> {
                    if (isSell) {
                        ordersService.sell(FIGI, lots, price);
                    } else {
                        ordersService.buy(FIGI, lots, price);
                    }
                },
                "Expected exception when the API throws an Exception");
        assertEquals(apiException, factEx.getCause().getCause());

        ArgumentCaptor<LimitOrder> orderCaptor = ArgumentCaptor.forClass(LimitOrder.class);
        verify(ordersCtx).placeLimitOrder(eq(FIGI), orderCaptor.capture(), eq(ACCOUNT_ID));
        assertEquals(lots, orderCaptor.getValue().lots);
        assertEquals(price, orderCaptor.getValue().price);
        assertEquals(isSell ? Operation.Sell : Operation.Buy, orderCaptor.getValue().operation);
    }

    /**
     * Generates PlacedOrder with given status
     *
     * @param status status to set
     * @return placedOrder instance with all fields, other than status, randomly set
     */
    private static PlacedOrder generatePlacedOrder(Status status) {
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomize(field -> field.getName().equals("status"), () -> status);
        EasyRandom easyRandom = new EasyRandom(parameters);
        return easyRandom.objects(PlacedOrder.class, 1).findAny().get();
    }

    /**
     * Creates orders, one order for every Status value
     *
     * @return orders
     */
    private static List<Order> createOrdersOfAllTypes() {
        return Arrays.stream(OrderStatus.values())
                .map(s -> Order.builder()
                        .id(ORDER_ID + s.name())
                        .figi("FIGI_1" + s.name())
                        .operation(StockOperation.Buy)
                        .status(s)
                        .lotsFilled(0)
                        .lotsRequested(1)
                        .type(StockOrderType.Market)
                        .price(BigDecimal.ZERO)
                        .build()
                ).collect(Collectors.toList());
    }

    private static List<ru.tinkoff.invest.openapi.models.orders.Order> createTinkoffOrdersOfAllTypes() {
        return Arrays.stream(Status.values())
                .map(s -> new ru.tinkoff.invest.openapi.models.orders.Order(
                        ORDER_ID + s.name(),
                        "FIGI_1" + s.name(),
                        Operation.Buy,
                        s,
                        1,
                        0,
                        OrderType.Market,
                        BigDecimal.ZERO
                )).collect(Collectors.toList());
    }

    private static Order createNewOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .figi("FIGI_1")
                .operation(StockOperation.Buy)
                .status(OrderStatus.New)
                .lotsFilled(0)
                .lotsRequested(1)
                .type(StockOrderType.Market)
                .price(BigDecimal.ZERO)
                .build();
    }

    private static Order createOrderWithIdAndStatus(String id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .figi("FIGI_1")
                .operation(StockOperation.Buy)
                .status(status)
                .lotsFilled(0)
                .lotsRequested(1)
                .type(StockOrderType.Market)
                .price(BigDecimal.ZERO)
                .build();
    }

    private static ru.tinkoff.invest.openapi.models.orders.Order createNewTinkoffOrder() {
        return new ru.tinkoff.invest.openapi.models.orders.Order(
                ORDER_ID,
                "FIGI_1",
                Operation.Buy,
                Status.New,
                1,
                0,
                OrderType.Market,
                BigDecimal.ZERO
        );
    }
}