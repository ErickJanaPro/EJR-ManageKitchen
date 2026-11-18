package com.ejr.managekitchen.operations.user;

import com.ejr.managekitchen.common.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Profil dev uniquement : crée un compte par rôle si la base est vide (mot de passe : password123). */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final String DEV_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        String hash = passwordEncoder.encode(DEV_PASSWORD);
        userRepository.save(new User("admin@ejr.local", hash, "Admin Démo", Role.ADMIN));
        userRepository.save(new User("cuisinier@ejr.local", hash, "Chef Démo", Role.CUISINIER));
        userRepository.save(new User("client@ejr.local", hash, "Client Démo", Role.CLIENT));
        log.info("Comptes de démonstration créés (admin / cuisinier / client @ejr.local, mot de passe {})", DEV_PASSWORD);
    }
}
