package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.*;
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
import ru.tinkoff.invest.openapi.models.portfolio.Portfolio;
import ru.tinkoff.invest.openapi.models.portfolio.PortfolioCurrencies;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PortfolioServiceImplTest {

    private static final String ACCOUNT_ID = "AACOUNT_ID1";
    private static final String FIGI1 = "FIGI1";
    private static final String FIGI2 = "FIGI2";
    private static final String FIGI3 = "FIGI3";

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

    @Test
    void getPortfolio() throws TradingApiException, AccountNotFoundException {
        final PortfolioCurrencies portfolioCurrencies = generatePortfolioCurrencies();
        CompletableFuture<PortfolioCurrencies> future = CompletableFuture.completedFuture(portfolioCurrencies);
        when(portfolioCtx.getPortfolioCurrencies(eq(ACCOUNT_ID))).thenReturn(future);

        Portfolio portfolio = generatePortfolio();
        CompletableFuture<Portfolio> future1 = CompletableFuture.completedFuture(portfolio);
        when(portfolioCtx.getPortfolio(eq(ACCOUNT_ID))).thenReturn(future1);

        final com.github.barbitoff.trading.traderacoon.api.model.Portfolio factPortfolio = portfolioService.getPortfolio();
        final List<PortfolioPosition> factPositions = factPortfolio.getPositions();
        assertEquals(portfolio.positions.size() + portfolioCurrencies.currencies.size(), factPositions.size());
        validateCurrencies(factPositions, portfolioCurrencies);
        validateNonCurrencies(factPositions, portfolio);
    }

    @Test
    void getPortfolioEmpty() throws TradingApiException, AccountNotFoundException {
        PortfolioCurrencies currencies = new PortfolioCurrencies(Collections.emptyList());
        CompletableFuture<PortfolioCurrencies> future = CompletableFuture.completedFuture(currencies);
        when(portfolioCtx.getPortfolioCurrencies(eq(ACCOUNT_ID))).thenReturn(future);

        Portfolio portfolio = new Portfolio(Collections.emptyList());
        CompletableFuture<Portfolio> future1 = CompletableFuture.completedFuture(portfolio);
        when(portfolioCtx.getPortfolio(eq(ACCOUNT_ID))).thenReturn(future1);

        final com.github.barbitoff.trading.traderacoon.api.model.Portfolio factPortfolio = portfolioService.getPortfolio();
        assertTrue(factPortfolio.getPositions().isEmpty());
    }

    @Test
    void getPortfolioApiException() {
        IOException apiException = new IOException("FAKE");
        CompletableFuture<PortfolioCurrencies> future = CompletableFuture.failedFuture(apiException);
        when(portfolioCtx.getPortfolioCurrencies(eq(ACCOUNT_ID))).thenReturn(future);

        CompletableFuture<Portfolio> future1 = CompletableFuture.failedFuture(apiException);
        when(portfolioCtx.getPortfolio(eq(ACCOUNT_ID))).thenReturn(future1);

        TradingApiException exFact = assertThrows(TradingApiException.class,
                () -> portfolioService.getPortfolio(),
                "Expected TradingApiException when an underlying API throws an exception");
        assertEquals(apiException, exFact.getCause().getCause());
    }

    @Test
    void getPortfolioAccNotFoundException() throws TradingApiException, AccountNotFoundException {
        Mockito.reset(accountsService);
        when(accountsService.getTradingAccount()).thenThrow(new AccountNotFoundException());
        assertThrows(AccountNotFoundException.class,
                () -> portfolioService.getPortfolio(),
                "Expected TradingApiException when an underlying API throws an exception");
    }

    private PortfolioCurrencies generatePortfolioCurrencies() {
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

        return new PortfolioCurrencies(currenciesList);
    }

    private void validateCurrencies(List<PortfolioPosition> curListFact, PortfolioCurrencies currenciesSrc) {
        final PortfolioCurrencies.PortfolioCurrency cur1 = currenciesSrc.currencies.get(0);
        final PortfolioCurrencies.PortfolioCurrency cur2 = currenciesSrc.currencies.get(1);
        assertTrue(curListFact.stream().anyMatch(curFact ->
                curFact.getType() == InstrumentType.Currency
                        && curFact.getBlocked().equals(cur1.blocked)
                        && curFact.getBalance().equals(cur1.balance)
                        && ((CurrencyPortfolioPosition) curFact).getCurrency()
                        .getCurrencyCode().equalsIgnoreCase(cur1.currency.name())
        ));
        assertTrue(curListFact.stream().anyMatch(curFact ->
                curFact.getType() == InstrumentType.Currency
                        && curFact.getBlocked().equals(cur2.blocked)
                        && curFact.getBalance().equals(cur2.balance)
                        && ((CurrencyPortfolioPosition) curFact).getCurrency()
                        .getCurrencyCode().equalsIgnoreCase(cur2.currency.name())
        ));
    }

    @Test
    void getCurrenciesSuccess() throws TradingApiException, AccountNotFoundException {
        final PortfolioCurrencies portfolioCurrencies = generatePortfolioCurrencies();
        CompletableFuture<PortfolioCurrencies> future = CompletableFuture.completedFuture(portfolioCurrencies);
        when(portfolioCtx.getPortfolioCurrencies(eq(ACCOUNT_ID))).thenReturn(future);

        final List<PortfolioPosition> curListFact = portfolioService.getCurrencies();
        assertEquals(2, curListFact.size());
        validateCurrencies(curListFact, portfolioCurrencies);
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

    private Portfolio generatePortfolio() {
        List<Portfolio.PortfolioPosition> portfolioPositions = new ArrayList<>(2);
        Portfolio.PortfolioPosition pos1 = new Portfolio.PortfolioPosition(
                FIGI1,
                "ticker1",
                "isin1",
                ru.tinkoff.invest.openapi.models.portfolio.InstrumentType.Stock,
                BigDecimal.TEN,
                BigDecimal.ONE,
                null,
                2,
                null,
                null,
                "name1"
        );
        Portfolio.PortfolioPosition pos2 = new Portfolio.PortfolioPosition(
                FIGI2,
                "ticker2",
                "isin2",
                ru.tinkoff.invest.openapi.models.portfolio.InstrumentType.Bond,
                BigDecimal.ONE,
                BigDecimal.TEN,
                null,
                3,
                null,
                null,
                "name2"
        );
        Portfolio.PortfolioPosition pos3 = new Portfolio.PortfolioPosition(
                FIGI3,
                "ticker3",
                "isin3",
                ru.tinkoff.invest.openapi.models.portfolio.InstrumentType.Etf,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                null,
                4,
                null,
                null,
                "name3"
        );
        portfolioPositions.add(pos1);
        portfolioPositions.add(pos2);
        portfolioPositions.add(pos3);

        return new Portfolio(portfolioPositions);
    }

    private void validateNonCurrencies(List<PortfolioPosition> posListFact, Portfolio srcPortfolio) {
        final Portfolio.PortfolioPosition pos1 = srcPortfolio.positions.get(0);
        final Portfolio.PortfolioPosition pos2 = srcPortfolio.positions.get(1);
        final Portfolio.PortfolioPosition pos3 = srcPortfolio.positions.get(2);
        assertTrue(posListFact.stream().anyMatch(posFact ->
                posFact.getType() == InstrumentType.Stock
                        && posFact.getBlocked().equals(pos1.blocked)
                        && posFact.getBalance().equals(pos1.balance)
                        && ((NonCurrencyPortfolioPosition) posFact).getLots() == pos1.lots
                        && ((NonCurrencyPortfolioPosition) posFact).getFigi().equals(pos1.figi)
        ));
        assertTrue(posListFact.stream().anyMatch(posFact ->
                posFact.getType() == InstrumentType.Bond
                        && posFact.getBlocked().equals(pos2.blocked)
                        && posFact.getBalance().equals(pos2.balance)
                        && ((NonCurrencyPortfolioPosition) posFact).getLots() == pos2.lots
                        && ((NonCurrencyPortfolioPosition) posFact).getFigi().equals(pos2.figi)
        ));
        assertTrue(posListFact.stream().anyMatch(posFact ->
                posFact.getType() == InstrumentType.Etf
                        && posFact.getBlocked().equals(pos3.blocked)
                        && posFact.getBalance().equals(pos3.balance)
                        && ((NonCurrencyPortfolioPosition) posFact).getLots() == pos3.lots
                        && ((NonCurrencyPortfolioPosition) posFact).getFigi().equals(pos3.figi)
        ));
    }

    @Test
    void getNonCurrenciesSuccess() throws TradingApiException, AccountNotFoundException {
        Portfolio portfolio = generatePortfolio();
        CompletableFuture<Portfolio> future = CompletableFuture.completedFuture(portfolio);
        when(portfolioCtx.getPortfolio(eq(ACCOUNT_ID))).thenReturn(future);

        final List<PortfolioPosition> posListFact = portfolioService.getNonCurrencies();
        assertEquals(3, posListFact.size());
        validateNonCurrencies(posListFact, portfolio);
    }

    @Test
    void getNonCurrenciesEmpty() throws TradingApiException, AccountNotFoundException {
        Portfolio portfolio = new Portfolio(Collections.emptyList());
        CompletableFuture<Portfolio> future = CompletableFuture.completedFuture(portfolio);
        when(portfolioCtx.getPortfolio(eq(ACCOUNT_ID))).thenReturn(future);

        final List<PortfolioPosition> curListFact = portfolioService.getNonCurrencies();
        assertTrue(curListFact.isEmpty());
    }

    @Test
    void getNonCurrenciesApiException() {
        IOException apiException = new IOException("FAKE");
        CompletableFuture<Portfolio> future = CompletableFuture.failedFuture(apiException);
        when(portfolioCtx.getPortfolio(eq(ACCOUNT_ID))).thenReturn(future);

        TradingApiException exFact = assertThrows(TradingApiException.class,
                () -> portfolioService.getNonCurrencies(),
                "Expected TradingApiException when an underlying API throws an exception");
        assertEquals(apiException, exFact.getCause().getCause());
    }

    @Test
    void getNonCurrenciesAccNotFoundException() throws TradingApiException, AccountNotFoundException {
        Mockito.reset(accountsService);
        when(accountsService.getTradingAccount()).thenThrow(new AccountNotFoundException());
        assertThrows(AccountNotFoundException.class,
                () -> portfolioService.getNonCurrencies(),
                "Expected TradingApiException when an underlying API throws an exception");
    }
}