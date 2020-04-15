package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.Instrument;
import com.github.barbitoff.trading.traderacoon.api.model.InstrumentType;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.InstrumentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;

import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Instrument service implementation
 */
@Service
@Slf4j
@AllArgsConstructor
public class InstrumentServiceImpl implements InstrumentService {

    private OpenApi api;

    @Override
    public Optional<Instrument> getInstrument(String figi) throws TradingApiException {
        try {
            final Optional<ru.tinkoff.invest.openapi.models.market.Instrument> instrument = api
                    .getMarketContext().searchMarketInstrumentByFigi(figi).get();
            return instrument.map(src -> {
                Instrument dest = new Instrument();
                dest.setFigi(src.figi);
                dest.setCurrency(src.currency == null ? null : Currency.getInstance(src.currency.name()));
                dest.setLotSize(src.lot);
                dest.setMinPriceIncrement(src.minPriceIncrement);
                dest.setType(InstrumentType.valueOf(src.type.name()));
                return dest;
            });
        } catch (InterruptedException | ExecutionException ex) {
            throw new TradingApiException("Error getting information about an instrument", ex);
        }
    }
}
