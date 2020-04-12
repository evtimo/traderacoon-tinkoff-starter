package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config.sandbox;

import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config.TinkoffOpenApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.sandbox.CurrencyBalance;

import java.math.BigDecimal;

/**
 * Initializes sandbox account with some starting RUB value according to
 * the application configuration
 *
 * @see TinkoffOpenApiProperties
 */
@Slf4j
@Component
public class SandboxAccountInitializer implements InitializingBean {

    private OpenApi api;
    private AccountService accountService;
    private TinkoffOpenApiProperties props;

    public SandboxAccountInitializer(OpenApi api, AccountService accountService, TinkoffOpenApiProperties props) {
        this.api = api;
        this.accountService = accountService;
        this.props = props;
    }

    @Override
    public void afterPropertiesSet() {
        if (props.getSandbox().isEnabled()) {
            try {
                // process clearOnStartup flag
                if (props.getSandbox().isClearOnStartup()) {
                    log.info("Claering sandbox portfolio");
                    ((SandboxOpenApi) api).getSandboxContext().clearAll(
                            accountService.getTradingAccount().getId()
                    );
                    log.info("Portfolio cleared");
                }
                // process initRubs
                if (props.getSandbox().getInitRubs() != null) {
                    log.info("Initializing sandbox with initial RUB value");
                    ((SandboxOpenApi) api).getSandboxContext().setCurrencyBalance(
                            new CurrencyBalance(Currency.RUB,
                                    BigDecimal.valueOf(props.getSandbox().getInitRubs())
                            ),
                            accountService.getTradingAccount().getId()
                    );
                    log.info("Sandbox account initialized with initial RUB value");
                }
            } catch (AccountNotFoundException | TradingApiException ex) {
                throw new RuntimeException("Account not found while trying to initialize Sandbox", ex);
            }
        }
    }
}
