package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config;

import com.github.barbitoff.trading.traderacoon.api.model.exception.AccountNotFoundException;
import com.github.barbitoff.trading.traderacoon.api.model.exception.TradingApiException;
import com.github.barbitoff.trading.traderacoon.api.service.AccountService;
import com.github.barbitoff.trading.traderacoon.api.service.OrderService;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services.AccountServiceImpl;
import com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.sandbox.CurrencyBalance;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Configuration
@EnableConfigurationProperties(TinkoffOpenApiProperties.class)
@Slf4j
public class TinkoffOpenApiConfiguration {
    private TinkoffOpenApiProperties props;
    private ApplicationContext ctx;

    public TinkoffOpenApiConfiguration(TinkoffOpenApiProperties props, ApplicationContext appCtx) {
        this.props = props;
    }

    @Bean(destroyMethod = "close")
    public OpenApi getOpenApi() {
        log.debug("Initializing Tinkoff API");
        OpenApi api;
        var factory = new OkHttpOpenApiFactory(props.getApiToken(), Logger.getLogger(getClass().getCanonicalName()));
        if (props.getSandbox().isEnabled()) {
            log.debug("Using SANDBOX mode");
            api = factory.createSandboxOpenApiClient(Executors.newSingleThreadExecutor());
            ((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();
        } else {
            log.debug("Using PRODUCTIVE mode");
            api = factory.createOpenApiClient(Executors.newSingleThreadExecutor());
        }
        log.debug("Initialization finished");
        return api;
    }

    @Bean
    public AccountService getAccountService() {
        OpenApi api = ctx.getBean(OpenApi.class);
        return new AccountServiceImpl(api, props.getSandbox().isEnabled(), props.isUseIisAccount());
    }

    @Bean
    public OrderService getOrderService() {
        return new OrderServiceImpl();
    }

    @PostConstruct
    public void initSandboxAccount() {
        if(props.getSandbox().isEnabled()
                && props.getSandbox().getInitRubs() != null) {
            log.info("Initializing sandbox with initial RUB value");
            OpenApi api = ctx.getBean(OpenApi.class);
            AccountService accountService = ctx.getBean(AccountService.class);
            try {
                ((SandboxOpenApi) api).getSandboxContext().setCurrencyBalance(
                        new CurrencyBalance(Currency.RUB,
                                BigDecimal.valueOf(props.getSandbox().getInitRubs())
                        ),
                        accountService.getTradingAccount().getId()
                );
            } catch (AccountNotFoundException ex) {
                throw new RuntimeException("Account not found while trying to initialize Sandbox", ex);
            }
            log.info("Sandbox account initialized with initial RUB value");
        }
    }
}
