package com.ejr.managekitchen.operations.notification;

/** Masque une adresse e-mail pour les logs : client@ejr.local -> c***@ejr.local */
final class EmailMasker {

    private EmailMasker() {
    }

    static String mask(String email) {
        if (email == null) {
            return "(inconnu)";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}