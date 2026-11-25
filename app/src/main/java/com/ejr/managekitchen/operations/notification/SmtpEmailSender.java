package com.ejr.managekitchen.operations.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Envoi réel des e-mails par SMTP (Mailpit en développement).
 * Actif uniquement si app.notifications.email.mode=smtp.
 * Un échec d'envoi est journalisé mais ne fait jamais échouer la commande.
 */
@Component
@ConditionalOnProperty(prefix = "app.notifications.email", name = "mode", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${app.notifications.email.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async
    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("E-mail envoyé à {}", EmailMasker.mask(to));
        } catch (MailException e) {
            log.warn("Échec de l'envoi de l'e-mail à {} : {}", EmailMasker.mask(to), e.getMessage());
        }
    }
}