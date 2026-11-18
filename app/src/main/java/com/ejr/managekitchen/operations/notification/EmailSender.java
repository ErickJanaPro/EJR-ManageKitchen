package com.ejr.managekitchen.operations.notification;

/**
 * Envoi d'e-mails automatisés. Le service réel (SMTP, Mailtrap...) n'est pas encore choisi :
 * remplacer {@link LoggingEmailSender} par une implémentation basée sur JavaMailSender le moment venu.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
