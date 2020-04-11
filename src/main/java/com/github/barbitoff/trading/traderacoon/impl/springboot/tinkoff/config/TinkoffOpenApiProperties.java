package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Starter configuration
 */
@ConfigurationProperties(prefix="tinkoff")
@Getter
@Setter
public class TinkoffOpenApiProperties {
    /**
     * Token for the API
     */
    private String apiToken;
    /**
     * Flag, indicating whether or not use IIS account
     */
    private boolean useIisAccount;
    /**
     * Tinkoff API sandbox configuration
     */
    private Sandbox sandbox;

    @Getter
    @Setter
    public static class Sandbox {
        /**
         * Enable Tinkoff Sandbox usage
         */
        private boolean enabled;
        /**
         * If set, sandbox account gets initialized on start with the specified
         * value (in RUB currency)
         */
        private Integer initRubs;
    }
}
