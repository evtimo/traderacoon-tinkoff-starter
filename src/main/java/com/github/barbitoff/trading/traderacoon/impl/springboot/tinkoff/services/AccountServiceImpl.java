package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.TradingAccount;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.user.AccountsList;
import ru.tinkoff.invest.openapi.models.user.BrokerAccountType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * AccountService implementation, based on Tinkoff API
 */
@Slf4j
public class AccountServiceImpl implements AccountService {

    private TradingAccount tradingAccount;

    // TODO: don't throw exception from the constructor!
    public AccountServiceImpl(OpenApi api, boolean sandbox, boolean useIisAccount) throws TradingApiException {
        log.debug("Preparing account information");
        CompletableFuture<AccountsList> accountsCf = api.getUserContext().getAccounts();
        AccountsList accountsList;
        try {
            accountsList = accountsCf.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new TradingApiException("Error getting accounts list", ex);
        }

        if (accountsList != null) {
            log.debug("Got " + accountsList.accounts.size() + " accounts");
            this.tradingAccount = accountsList.accounts.stream()
                    .filter(acc ->
                            sandbox ||
                                    useIisAccount && acc.brokerAccountType == BrokerAccountType.TinkoffIis
                                    || !useIisAccount && acc.brokerAccountType == BrokerAccountType.Tinkoff)
                    .findFirst()
                    .map(tinkAcc -> new TradingAccount(tinkAcc.brokerAccountId))
                    .orElse(null);
        }
    }

    @Override
    public TradingAccount getTradingAccount() throws AccountNotFoundException {
        if (tradingAccount == null) {
            throw new AccountNotFoundException();
        }
        return tradingAccount;
    }
}
