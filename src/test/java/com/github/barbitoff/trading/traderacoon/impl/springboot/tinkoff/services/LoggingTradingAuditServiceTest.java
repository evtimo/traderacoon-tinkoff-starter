package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.events.TradingAuditEvent;
import com.github.barbitoff.trading.traderacoon.api.service.TradingAuditService;
import org.answerit.mock.slf4j.MockSlf4j;
import static org.answerit.mock.slf4j.MockSlf4jMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.answerit.mock.slf4j.That.that;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

class LoggingTradingAuditServiceTest {

    private TradingAuditService svc;
    private Logger mockedLogger;

    @BeforeEach
    public void setUp() {
        svc = new LoggingTradingAuditService();
        mockedLogger = MockSlf4j.mockStatic(LoggingTradingAuditService.class, "log");
    }

    @Test
    void fireEvent() {
        TradingAuditEvent evt = Mockito.mock(TradingAuditEvent.class);
        String str = "STR1";
        when(evt.toString()).thenReturn(str);

        svc.fireEvent(evt);

        assertThat(mockedLogger, hasEntriesCount(1));
        assertThat(mockedLogger, hasAtLeastOneEntry(
                that(haveMessage(
                        containsString(str)
                )))
        );
    }
}