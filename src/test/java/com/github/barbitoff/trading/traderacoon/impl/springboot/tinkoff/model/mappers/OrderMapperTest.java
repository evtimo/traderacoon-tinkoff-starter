package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.model.mappers;

import com.github.barbitoff.trading.traderacoon.api.model.StockOperation;
import com.github.barbitoff.trading.traderacoon.api.model.StockOrderType;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import ru.tinkoff.invest.openapi.models.orders.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private static final String FIGI = "FIGI1";
    private static final BigDecimal PRICE = BigDecimal.TEN;

    OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @ParameterizedTest
    @EnumSource(OrderType.class)
    public void testTinkoffOrderMappingForDifferentOrderTypes(OrderType type) {
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomize(field -> field.getName().equals("type"), () -> type);
        EasyRandom easyRandom = new EasyRandom(parameters);
        Order src = easyRandom.objects(Order.class,1).findFirst().get();
        mapTinkoffOrderAndValidate(src);
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    public void testTinkoffOrderMappingForDifferentStatuses(Status status) {
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomize(field -> field.getName().equals("status"), () -> status);
        EasyRandom easyRandom = new EasyRandom(parameters);
        Order src = easyRandom.objects(Order.class,1).findFirst().get();
        mapTinkoffOrderAndValidate(src);
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    public void testTinkoffOrderMappingForDifferentOperations(Operation op) {
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomize(field -> field.getName().equals("operation"), () -> op);
        EasyRandom easyRandom = new EasyRandom(parameters);
        Order src = easyRandom.objects(Order.class,1).findFirst().get();
        mapTinkoffOrderAndValidate(src);
    }

    private void mapTinkoffOrderAndValidate(Order src) {
        com.github.barbitoff.trading.traderacoon.api.model.Order target = mapper.mapTinkoffOrder(src);

        assertEquals(src.id, target.getId());
        assertEquals(src.figi, target.getFigi());
        assertEquals(src.executedLots, target.getLotsFilled());
        assertEquals(src.requestedLots, target.getLotsRequested());
        assertEquals(src.operation.name(), target.getOperation().name());
        assertEquals(src.price, target.getPrice());
        assertEquals(src.status.name(), target.getStatus().name());
        assertEquals(src.type.name(), target.getType().name());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    public void testPlacedOrderMappingForDifferentOperations(Operation op) {
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomize(field -> field.getName().equals("operation"), () -> op);
        EasyRandom easyRandom = new EasyRandom(parameters);

        PlacedOrder src = easyRandom.objects(PlacedOrder.class,1).findFirst().get();
        mapPlacedOrderAndValidate(src);
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    public void testPlacedOrderMappingForDifferentStatuses(Status status) {
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomize(field -> field.getName().equals("status"), () -> status);
        EasyRandom easyRandom = new EasyRandom(parameters);

        PlacedOrder src = easyRandom.objects(PlacedOrder.class,1).findFirst().get();
        mapPlacedOrderAndValidate(src);
    }

    private void mapPlacedOrderAndValidate(PlacedOrder src) {
        mapPlacedOrderAndValidate(src, StockOrderType.Market);
        mapPlacedOrderAndValidate(src, StockOrderType.Limit);
    }

    private void mapPlacedOrderAndValidate(PlacedOrder src, StockOrderType type) {
        com.github.barbitoff.trading.traderacoon.api.model.Order target = mapper.mapTinkoffOrder(
                src,
                FIGI,
                type,
                PRICE
        );

        assertEquals(src.id, target.getId());
        assertEquals(FIGI, target.getFigi());
        assertEquals(src.executedLots, target.getLotsFilled());
        assertEquals(src.requestedLots, target.getLotsRequested());
        assertEquals(src.operation.name(), target.getOperation().name());
        assertEquals(PRICE, target.getPrice());
        assertEquals(src.status.name(), target.getStatus().name());
        assertEquals(type.name(), target.getType().name());
    }
}