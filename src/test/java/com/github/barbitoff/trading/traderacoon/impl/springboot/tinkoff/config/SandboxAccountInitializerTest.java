package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config;

import com.github.barbitoff.trading.traderacoon.api.model.TradingAccount;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;
import ru.tinkoff.invest.openapi.SandboxContext;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.sandbox.CurrencyBalance;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SandboxAccountInitializerTest {

    private static final String ACCOUNT_ID = "ACC1";
    private static final int BALANCE = 1000000;

    @Mock
    private SandboxOpenApi api;
    @Mock
    private AccountService accountService;
    @Mock
    private SandboxContext sandboxContext;

    private TinkoffOpenApiProperties props = new TinkoffOpenApiProperties();

    private SandboxAccountInitializer initializer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(api.getSandboxContext()).thenReturn(sandboxContext);
        initializer = new SandboxAccountInitializer(api, accountService, props);
    }

    private void initSuccessfulAccountService() throws TradingApiException, AccountNotFoundException {
        when(accountService.getTradingAccount()).thenReturn(new TradingAccount(ACCOUNT_ID));
    }

    /**
     * Sandbox account won't be initialized if tinkoff.sandbox.initRubs property is not set, never mind whether
     * a "sandbox" flag set or not
     * @param isSandbox "sandbox" flag value
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testNoInitRub(boolean isSandbox) throws TradingApiException, AccountNotFoundException {
        TinkoffOpenApiProperties.Sandbox sandbox = new TinkoffOpenApiProperties.Sandbox();
        sandbox.setEnabled(isSandbox);
        props.setSandbox(sandbox);
        initSuccessfulAccountService();

        initializer.afterPropertiesSet();
        verify(api, never()).getSandboxContext();
    }

    @Test
    public void testSandboxWithInitRub() throws TradingApiException, AccountNotFoundException {
        TinkoffOpenApiProperties.Sandbox sandbox = new TinkoffOpenApiProperties.Sandbox();
        sandbox.setEnabled(true);
        sandbox.setInitRubs(BALANCE);
        props.setSandbox(sandbox);
        initSuccessfulAccountService();

        initializer.afterPropertiesSet();

        ArgumentCaptor<CurrencyBalance> balanceCaptor = ArgumentCaptor.forClass(CurrencyBalance.class);
        verify(sandboxContext).setCurrencyBalance(balanceCaptor.capture(), eq(ACCOUNT_ID));
        assertEquals(BigDecimal.valueOf(BALANCE), balanceCaptor.getValue().balance);
        assertEquals(Currency.RUB, balanceCaptor.getValue().currency);
    }

    @Test
    public void testAccountNotFoundException() throws TradingApiException, AccountNotFoundException {
        Exception exceptionExpected = new AccountNotFoundException();
        testExceptionWhileGettingAccount(exceptionExpected);
    }

    @Test
    public void testTradingApiException() throws TradingApiException, AccountNotFoundException {
        Exception exceptionExpected = new TradingApiException("fake");
        testExceptionWhileGettingAccount(exceptionExpected);
    }

    public void testExceptionWhileGettingAccount(Exception exceptionExpected) throws TradingApiException, AccountNotFoundException {
        TinkoffOpenApiProperties.Sandbox sandbox = new TinkoffOpenApiProperties.Sandbox();
        sandbox.setEnabled(true);
        sandbox.setInitRubs(BALANCE);
        props.setSandbox(sandbox);
        when(accountService.getTradingAccount()).thenThrow(exceptionExpected);

        RuntimeException exceptionFact = assertThrows(RuntimeException.class,
                () -> initializer.afterPropertiesSet(),
                "Exception expected when can't get an account because of exception"
        );
        assertEquals(exceptionExpected, exceptionFact.getCause());
    }
}