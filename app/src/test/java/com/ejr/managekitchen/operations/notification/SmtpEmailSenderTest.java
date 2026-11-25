package com.ejr.managekitchen.operations.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Tests sans serveur SMTP : JavaMailSender est simulé. */
class SmtpEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender, "no-reply@test.local");

    @Test
    void construitLeBonMessage() {
        sender.send("client@ejr.local", "Votre commande est prête", "Corps du message");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@test.local");
        assertThat(message.getTo()).containsExactly("client@ejr.local");
        assertThat(message.getSubject()).isEqualTo("Votre commande est prête");
        assertThat(message.getText()).isEqualTo("Corps du message");
    }

    @Test
    void unEchecDEnvoiNeRemontePas() {
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> sender.send("client@ejr.local", "Sujet", "Corps"))
                .doesNotThrowAnyException();
    }
}