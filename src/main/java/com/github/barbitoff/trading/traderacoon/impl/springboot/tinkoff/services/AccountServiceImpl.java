package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.TradingAccount;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config.TinkoffOpenApiConfiguration;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config.TinkoffOpenApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.user.AccountsList;
import ru.tinkoff.invest.openapi.models.user.BrokerAccountType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * AccountService implementation, based on Tinkoff API
 */
@Slf4j
@Service(TinkoffOpenApiConfiguration.BEANS_QUALIFIER_PREFIX + "AccountService")
public class AccountServiceImpl implements AccountService {

    private OpenApi api;
    private boolean sandbox;
    private boolean useIisAccount;
    private volatile TradingAccount tradingAccount;

    public AccountServiceImpl(OpenApi api, TinkoffOpenApiProperties props) {
        this.api = api;
        this.sandbox = props.getSandbox().isEnabled();
        this.useIisAccount = props.isUseIisAccount();
    }

    /**
     * Retrieves trading account using Tinkoff API depending on configuration (sandbox and IIS account usage). For
     * sandbox gets a first account, for non-sandbox mode retrieves an account with the type, corresponding to the
     * "useIisAccount" setting
     *
     * @throws TradingApiException      if Tinkoff API throws an exception
     * @throws AccountNotFoundException if account can't be found
     */
    protected void retrieveTradingAccount() throws TradingApiException, AccountNotFoundException {
        log.debug("Preparing account information");
        CompletableFuture<AccountsList> accountsCf = api.getUserContext().getAccounts();
        AccountsList accountsList;
        try {
            accountsList = accountsCf.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new TradingApiException("Error getting accounts list", ex);
        }

        if (accountsList != null && accountsList.accounts.size() > 0) {
            log.debug("Got " + accountsList.accounts.size() + " accounts");
            this.tradingAccount = accountsList.accounts.stream()
                    .filter(acc ->
                            sandbox ||
                                    useIisAccount && acc.brokerAccountType == BrokerAccountType.TinkoffIis
                                    || !useIisAccount && acc.brokerAccountType == BrokerAccountType.Tinkoff)
                    .findFirst()
                    .map(acc -> new TradingAccount(acc.brokerAccountId))
                    .orElseThrow(AccountNotFoundException::new);
        } else {
            throw new AccountNotFoundException();
        }
    }

    /**
     * If called for the first time, retrieves an account using Tinkoff API. Otherwise returns previously retrieved
     * account
     *
     * @return account
     * @throws TradingApiException      if Tinkoff API throws an exception
     * @throws AccountNotFoundException if account can't be found
     */
    @Override
    public TradingAccount getTradingAccount() throws TradingApiException, AccountNotFoundException {
        if (tradingAccount == null) {
            synchronized (this) {
                if (tradingAccount == null) {
                    retrieveTradingAccount();
                }
            }
        }
        return tradingAccount;
    }
}
