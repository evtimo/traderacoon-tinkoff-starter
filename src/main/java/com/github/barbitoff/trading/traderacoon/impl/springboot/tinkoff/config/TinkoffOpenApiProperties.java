package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="tinkoff")
@Getter
@Setter
public class TinkoffOpenApiProperties {
    private String apiToken;
    private boolean useIisAccount;
    private Sandbox sandbox;

    @Getter
    @Setter
    public static class Sandbox {
        private boolean enabled;
        private Integer initRubs;
    }
}
