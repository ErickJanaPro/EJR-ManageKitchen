package com.ejr.managekitchen.operations.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailMaskerTest {

    @Test
    void masqueLaPartieLocaleDeLAdresse() {
        assertThat(EmailMasker.mask("client@ejr.local")).isEqualTo("c***@ejr.local");
    }

    @Test
    void gereLesValeursInattendues() {
        assertThat(EmailMasker.mask(null)).isEqualTo("(inconnu)");
        assertThat(EmailMasker.mask("sans-arobase")).isEqualTo("***");
        assertThat(EmailMasker.mask("@ejr.local")).isEqualTo("***");
    }
}