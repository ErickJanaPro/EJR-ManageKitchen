package com.ejr.managekitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entrée de la plateforme EJR-ManageKitchen.
 * L'authentification repose sur JWT : l'utilisateur en mémoire généré par défaut par Spring est désactivé.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ManageKitchenApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManageKitchenApplication.class, args);
    }
}
