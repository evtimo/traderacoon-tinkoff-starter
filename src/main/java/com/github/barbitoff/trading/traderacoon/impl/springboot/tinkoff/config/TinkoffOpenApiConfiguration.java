package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Configuration
@EnableConfigurationProperties(TinkoffOpenApiProperties.class)
@ComponentScan("com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff")
@Slf4j
public class TinkoffOpenApiConfiguration {

    public static final String BEANS_QUALIFIER = "Tinkoff";

    private TinkoffOpenApiProperties props;

    public TinkoffOpenApiConfiguration(TinkoffOpenApiProperties props) {
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
}
