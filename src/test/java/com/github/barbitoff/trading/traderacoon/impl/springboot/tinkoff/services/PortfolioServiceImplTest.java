package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.CurrencyPortfolioPosition;
import com.github.barbitoff.trading.traderacoon.api.model.InstrumentType;
import com.github.barbitoff.trading.traderacoon.api.model.PortfolioPosition;
import com.github.barbitoff.trading.traderacoon.api.model.TradingAccount;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import com.github.barbitoff.trading.traderacoon.api.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.PortfolioContext;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.portfolio.PortfolioCurrencies;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PortfolioServiceImplTest {

    private static final String ACCOUNT_ID = "AACOUNT_ID1";

    @Mock
    private OpenApi api;
    @Mock
    private AccountService accountsService;
    @Mock
    private PortfolioContext portfolioCtx;

    private PortfolioService portfolioService;

    @BeforeEach
    public void setUp() throws TradingApiException, AccountNotFoundException {
        MockitoAnnotations.initMocks(this);
        // setup AccountsService mock
        when(accountsService.getTradingAccount()).thenReturn(new TradingAccount(ACCOUNT_ID));
        // setup api mock
        when(api.getPortfolioContext()).thenReturn(portfolioCtx);

        portfolioService = new PortfolioServiceImpl(api, accountsService);
    }

    // TODO: getportfolio tests for different cases
    @Test
    void getPortfolio() {
    }

    @Test
    void getCurrenciesSuccess() throws TradingApiException, AccountNotFoundException {
        List<PortfolioCurrencies.PortfolioCurrency> currenciesList = new ArrayList<>(2);
        PortfolioCurrencies.PortfolioCurrency cur1 = new PortfolioCurrencies.PortfolioCurrency(
                Currency.RUB,
                BigDecimal.ONE,
                BigDecimal.TEN
        );
        PortfolioCurrencies.PortfolioCurrency cur2 = new PortfolioCurrencies.PortfolioCurrency(
                Currency.EUR,
                BigDecimal.TEN,
                BigDecimal.ONE
        );
        currenciesList.add(cur1);
        currenciesList.add(cur2);

        PortfolioCurrencies currencies = new PortfolioCurrencies(currenciesList);
        CompletableFuture<PortfolioCurrencies> future = CompletableFuture.completedFuture(currencies);
        when(portfolioCtx.getPortfolioCurrencies(eq(ACCOUNT_ID))).thenReturn(future);

        final List<PortfolioPosition> curListFact = portfolioService.getCurrencies();
        assertEquals(2, curListFact.size());
        assertTrue(curListFact.stream().anyMatch(curFact ->
            curFact.getType() == InstrumentType.Currency
                && curFact.getBlocked().equals(cur1.blocked)
                && curFact.getBalance().equals(cur1.balance)
                && ((CurrencyPortfolioPosition)curFact).getCurrency()
                    .getCurrencyCode().equalsIgnoreCase(cur1.currency.name())
        ));
        assertTrue(curListFact.stream().anyMatch(curFact ->
                curFact.getType() == InstrumentType.Currency
                        && curFact.getBlocked().equals(cur2.blocked)
                        && curFact.getBalance().equals(cur2.balance)
                        && ((CurrencyPortfolioPosition)curFact).getCurrency()
                        .getCurrencyCode().equalsIgnoreCase(cur2.currency.name())
        ));
    }

    @Test
    void getCurrenciesEmpty() throws TradingApiException, AccountNotFoundException {
        List<PortfolioCurrencies.PortfolioCurrency> currenciesList = new ArrayList<>();

        PortfolioCurrencies currencies = new PortfolioCurrencies(currenciesList);
        CompletableFuture<PortfolioCurrencies> future = CompletableFuture.completedFuture(currencies);
        when(portfolioCtx.getPortfolioCurrencies(eq(ACCOUNT_ID))).thenReturn(future);

        final List<PortfolioPosition> curListFact = portfolioService.getCurrencies();
        assertTrue(curListFact.isEmpty());
    }

    @Test
    void getCurrenciesApiException() {
        IOException apiException = new IOException("FAKE");
        CompletableFuture<PortfolioCurrencies> future = CompletableFuture.failedFuture(apiException);
        when(portfolioCtx.getPortfolioCurrencies(eq(ACCOUNT_ID))).thenReturn(future);

        TradingApiException exFact = assertThrows(TradingApiException.class,
                () -> portfolioService.getCurrencies(),
                "Expected TradingApiException when an underlying API throws an exception");
        assertEquals(apiException, exFact.getCause().getCause());
    }

    @Test
    void getCurrenciesAccNotFoundException() throws TradingApiException, AccountNotFoundException {
        Mockito.reset(accountsService);
        when(accountsService.getTradingAccount()).thenThrow(new AccountNotFoundException());
        assertThrows(AccountNotFoundException.class,
                () -> portfolioService.getCurrencies(),
                "Expected TradingApiException when an underlying API throws an exception");
    }

    // TODO: getNonCurrencies tests
    @Test
    void getNonCurrencies() {
    }
}