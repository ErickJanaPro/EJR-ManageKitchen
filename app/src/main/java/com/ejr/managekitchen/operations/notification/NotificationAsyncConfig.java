package com.ejr.managekitchen.operations.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Active @Async : l'envoi des e-mails ne bloque pas la requête du cuisinier. */
@Configuration
@EnableAsync
public class NotificationAsyncConfig {
}