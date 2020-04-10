package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.model.mappers;

import com.github.barbitoff.trading.traderacoon.api.model.Order;
import com.github.barbitoff.trading.traderacoon.api.model.StockOrderType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.tinkoff.invest.openapi.models.orders.PlacedOrder;

import java.math.BigDecimal;

/**
 * Mapper, that maps tinkoff orders into traderacoon orders
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "requestedLots", target = "lotsRequested")
    @Mapping(source = "executedLots", target = "lotsFilled")
    Order mapTinkoffOrder(ru.tinkoff.invest.openapi.models.orders.Order order);

    @Mapping(source = "order.requestedLots", target = "lotsRequested")
    @Mapping(source = "order.executedLots", target = "lotsFilled")
    @Mapping(source = "figi", target = "figi")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "price", target = "price")
    Order mapTinkoffOrder(PlacedOrder order, String figi, StockOrderType type, BigDecimal price);
}
