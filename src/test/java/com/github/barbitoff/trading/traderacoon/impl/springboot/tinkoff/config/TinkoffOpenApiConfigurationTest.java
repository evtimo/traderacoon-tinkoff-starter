package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.OpenApiFactoryBase;
import ru.tinkoff.invest.openapi.SandboxContext;
import ru.tinkoff.invest.openapi.SandboxOpenApi;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class TinkoffOpenApiConfigurationTest {

    private static final String API_TOKEN = "APITOKEN1";

    private TinkoffOpenApiProperties props = new TinkoffOpenApiProperties();
    private TinkoffOpenApiConfiguration config;
    @Mock
    private OpenApiFactoryBase openApiFactory;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        config = new TinkoffOpenApiConfiguration(props) {
            @Override
            public OpenApiFactoryBase getOpenApiFactory() {
                return openApiFactory;
            }
        };
    }

    @Test
    void getSandboxOpenApi() {
        TinkoffOpenApiProperties.Sandbox sandbox = new TinkoffOpenApiProperties.Sandbox();
        sandbox.setEnabled(true);
        props.setSandbox(sandbox);
        props.setApiToken(API_TOKEN);

        SandboxOpenApi sandboxApi = mock(SandboxOpenApi.class);
        SandboxContext sandboxCtx = mock(SandboxContext.class);
        when(sandboxApi.getSandboxContext()).thenReturn(sandboxCtx);
        when(openApiFactory.createSandboxOpenApiClient(any())).thenReturn(sandboxApi);
        when(sandboxCtx.performRegistration(isNull())).thenReturn(CompletableFuture.completedFuture(null));

        config.getOpenApi();

        verify(openApiFactory).createSandboxOpenApiClient(any());
        verify(sandboxApi).getSandboxContext();
        verify(sandboxCtx).performRegistration(isNull());
    }

    @Test
    void getProductionOpenApi() {
        TinkoffOpenApiProperties.Sandbox sandbox = new TinkoffOpenApiProperties.Sandbox();
        sandbox.setEnabled(false);
        props.setSandbox(sandbox);
        props.setApiToken(API_TOKEN);

        OpenApi openApi = mock(OpenApi.class);
        when(openApiFactory.createOpenApiClient(any())).thenReturn(openApi);

        assertEquals(openApi, config.getOpenApi());
    }
}