package com.github.barbitoff.trading.traderacoon.impl.springboot.tinkoff.services;

import com.github.barbitoff.trading.traderacoon.api.model.events.TradingEvent;
import com.github.barbitoff.trading.traderacoon.api.service.TradingAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Simple implementation for the audit service to use as a default one.
 * Just logs the audit as a string using slf4j logger
 */
@Service
@Slf4j
@ConditionalOnMissingBean(TradingAuditService.class)
public class LoggingTradingAuditService implements TradingAuditService {

    public static final String MSG_PREFIX = "Audit event got: ";

    @Override
    public void fireEvent(TradingEvent tradingEvent) {
        log.info(MSG_PREFIX + tradingEvent.toString());
    }
}
