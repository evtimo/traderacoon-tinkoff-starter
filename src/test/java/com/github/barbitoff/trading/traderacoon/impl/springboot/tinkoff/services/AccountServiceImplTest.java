package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.TradingAccount;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.UserContext;
import ru.tinkoff.invest.openapi.models.user.AccountsList;
import ru.tinkoff.invest.openapi.models.user.BrokerAccount;
import ru.tinkoff.invest.openapi.models.user.BrokerAccountType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceImplTest {
    @Mock
    UserContext userCtx;
    @Mock
    OpenApi api;
    BrokerAccount iisAcc = new BrokerAccount(BrokerAccountType.TinkoffIis, "ACC1");
    BrokerAccount nonIisAcc = new BrokerAccount(BrokerAccountType.Tinkoff, "ACC2");

    private AccountService accountsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(api.getUserContext()).thenReturn(userCtx);
    }

    private void configureSuccessfullResponse() {
        CompletableFuture<AccountsList> future = CompletableFuture.completedFuture(new AccountsList(
                List.of(iisAcc, nonIisAcc)
        ));
        when(userCtx.getAccounts()).thenReturn(future);
    }

    @Test
    void getTradingAccountWhenSandbox() throws AccountNotFoundException, TradingApiException {
        configureSuccessfullResponse();
        accountsService = new AccountServiceImpl(api, true, false);
        TradingAccount tradingAccount = accountsService.getTradingAccount();
        // any account is ok when using sandbox
        assertTrue(tradingAccount.getId().equals(iisAcc.brokerAccountId) || tradingAccount.getId().equals(nonIisAcc.brokerAccountId));
    }

    @Test
    void getTradingAccountWhenUsingIis() throws AccountNotFoundException, TradingApiException {
        configureSuccessfullResponse();
        accountsService = new AccountServiceImpl(api, false, true);
        TradingAccount tradingAccount = accountsService.getTradingAccount();
        assertEquals(tradingAccount.getId(), iisAcc.brokerAccountId);
    }

    @Test
    void getTradingAccountWhenNotUsingIis() throws AccountNotFoundException, TradingApiException {
        configureSuccessfullResponse();
        accountsService = new AccountServiceImpl(api, false, false);
        TradingAccount tradingAccount = accountsService.getTradingAccount();
        assertEquals(tradingAccount.getId(), nonIisAcc.brokerAccountId);
    }

    @Test
    void getTradingAccountWithCaching() throws AccountNotFoundException, TradingApiException {
        configureSuccessfullResponse();
        accountsService = new AccountServiceImpl(api, false, false);
        accountsService.getTradingAccount();
        accountsService.getTradingAccount();
        InOrder inOrder = inOrder(userCtx); // is needed for calls() verification
        inOrder.verify(userCtx, calls(1)).getAccounts();
    }

    private static Stream<Arguments> provideBooleanCombinations() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(true, true),
                Arguments.of(false, true),
                Arguments.of(true, false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideBooleanCombinations")
    void getTradingAccountWhenApiError(boolean isSandbox, boolean useIisAccount) {
        IOException apiException = new IOException("Fake ex");
        CompletableFuture<AccountsList> future = CompletableFuture.failedFuture(apiException);
        when(userCtx.getAccounts()).thenReturn(future);

        accountsService = new AccountServiceImpl(api, isSandbox, useIisAccount);
        TradingApiException factEx = assertThrows(TradingApiException.class,
                () -> accountsService.getTradingAccount(),
                "Expected exception when api throws exception"
        );
        assertEquals(apiException, factEx.getCause().getCause());
    }

    @ParameterizedTest
    @MethodSource("provideBooleanCombinations")
    void getTradingAccountWhenNoAccounts(boolean isSandbox, boolean useIisAccount) {
        CompletableFuture<AccountsList> future = CompletableFuture.completedFuture(new AccountsList(Collections.emptyList()));
        when(userCtx.getAccounts()).thenReturn(future);

        accountsService = new AccountServiceImpl(api, isSandbox, useIisAccount);
        assertThrows(AccountNotFoundException.class,
                () -> accountsService.getTradingAccount(),
                "Expected exception when api returns no account"
        );
    }
}