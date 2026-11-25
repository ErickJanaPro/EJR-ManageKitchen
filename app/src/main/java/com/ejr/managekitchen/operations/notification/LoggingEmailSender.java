package com.ejr.managekitchen.operations.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Implémentation provisoire : l'e-mail est écrit dans les logs au lieu d'être envoyé. */
@Component
@ConditionalOnProperty(prefix = "app.notifications.email", name = "mode", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
            log.info("[E-MAIL simulé] à={} | sujet={} | {}", EmailMasker.mask(to), subject, body);
    }
}
