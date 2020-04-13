package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.*;
import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import com.github.barbitoff.trading.traderacoon.api.service.PortfolioService;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config.TinkoffOpenApiConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.portfolio.PortfolioCurrencies;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * PortfolioService implementation, based on Tinkoff API
 */
@AllArgsConstructor
@Slf4j
@Service(TinkoffOpenApiConfiguration.BEANS_QUALIFIER_PREFIX + "PortfolioService")
public class PortfolioServiceImpl implements PortfolioService {
    private final OpenApi api;
    private final AccountService accountService;

    /**
     * Creates a portfolio by joining currency and non-currency parts
     *
     * @return portfolio
     * @throws TradingApiException      if Tinkoff API throws an exception
     * @throws AccountNotFoundException if account service can't get an account
     */
    public Portfolio getPortfolio() throws TradingApiException, AccountNotFoundException {
        List<PortfolioPosition> pos1 = getCurrencies();
        List<PortfolioPosition> pos2 = getNonCurrencies();
        List<PortfolioPosition> joined = new ArrayList<>(pos1.size() + pos2.size());
        joined.addAll(pos1);
        joined.addAll(pos2);
        return new Portfolio(Collections.unmodifiableList(joined));
    }

    /**
     * Gets currencies and converts them into the target models
     *
     * @return currencies
     * @throws TradingApiException      if Tinkoff API throws an exception
     * @throws AccountNotFoundException if account service can't get an account
     */
    @Override
    public List<PortfolioPosition> getCurrencies() throws TradingApiException, AccountNotFoundException {
        try {
            return api.getPortfolioContext().getPortfolioCurrencies(
                    accountService.getTradingAccount().getId()
            ).get().currencies.stream().map(src -> {
                CurrencyPortfolioPosition dest = new CurrencyPortfolioPosition();
                dest.setCurrency(Currency.getInstance(src.currency.name()));
                dest.setBalance(src.balance);
                dest.setBlocked(src.blocked);
                return dest;
            }).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException ex) {
            throw new TradingApiException("Error getting portfolio currencies", ex);
        }
    }

    /**
     * Gets non-currency portfolio positions and converts them into the target models
     *
     * @return non-currency portfolio positions
     * @throws TradingApiException      if Tinkoff API throws an exception
     * @throws AccountNotFoundException if account service can't get an account
     */
    @Override
    public List<PortfolioPosition> getNonCurrencies() throws TradingApiException, AccountNotFoundException {
        try {
            return api.getPortfolioContext().getPortfolio(
                    accountService.getTradingAccount().getId()
            ).get().positions.stream().map(src -> {
                NonCurrencyPortfolioPosition dest = new NonCurrencyPortfolioPosition();
                dest.setType(InstrumentType.valueOf(src.instrumentType.name()));
                dest.setBalance(src.balance);
                dest.setBlocked(src.blocked);
                dest.setFigi(src.figi);
                dest.setLots(src.lots);
                return dest;
            }).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException ex) {
            throw new TradingApiException("Error getting portfolio positions", ex);
        }
    }
}
