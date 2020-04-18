package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.tinkoff.invest.openapi.MarketContext;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.market.InstrumentType;
import ru.tinkoff.invest.openapi.models.portfolio.PortfolioCurrencies;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class InstrumentServiceImplTest {
    private static final String FIGI = "FIGI1";

    @Mock
    private OpenApi api;
    @Mock
    private MarketContext marketCtx;

    private InstrumentServiceImpl instrumentService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(api.getMarketContext()).thenReturn(marketCtx);

        instrumentService = new InstrumentServiceImpl(api);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getInstrument(boolean useNullCurrency) throws TradingApiException {
        Instrument src = new Instrument(
                FIGI,
                "ticker1",
                "isin1",
                BigDecimal.TEN,
                11,
                useNullCurrency ? null : Currency.RUB,
                "name1",
                InstrumentType.Bond
        );
        CompletableFuture<Optional<Instrument>> future = CompletableFuture.completedFuture(Optional.of(src));
        when(marketCtx.searchMarketInstrumentByFigi(eq(FIGI))).thenReturn(future);

        final Optional<com.github.barbitoff.trading.traderacoon.api.model.Instrument> dest = instrumentService.getInstrument(FIGI);
        assertTrue(dest.isPresent());
        assertEquals(FIGI, dest.get().getFigi());
        assertEquals(BigDecimal.TEN, dest.get().getMinPriceIncrement());
        assertEquals(11, dest.get().getLotSize());
        assertEquals(com.github.barbitoff.trading.traderacoon.api.model.InstrumentType.Bond, dest.get().getType());
        if(useNullCurrency) {
            assertNull(dest.get().getCurrency());
        } else {
            assertEquals(java.util.Currency.getInstance("RUB"), dest.get().getCurrency());
        }
    }

    @Test
    void getInstrumentApiException() {
        IOException apiException = new IOException("FAKE");
        CompletableFuture<Optional<Instrument>> future = CompletableFuture.failedFuture(apiException);
        when(marketCtx.searchMarketInstrumentByFigi(eq(FIGI))).thenReturn(future);

        TradingApiException exFact = assertThrows(TradingApiException.class,
                () -> instrumentService.getInstrument(FIGI),
                "Expected TradingApiException when an underlying API throws an exception");
        assertEquals(apiException, exFact.getCause().getCause());
    }
}