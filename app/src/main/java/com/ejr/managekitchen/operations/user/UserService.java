package com.ejr.managekitchen.operations.user;

import com.ejr.managekitchen.common.exception.ConflictException;
import com.ejr.managekitchen.common.exception.NotFoundException;
import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.operations.user.dto.CreateUserRequest;
import com.ejr.managekitchen.operations.user.dto.UpdateUserRequest;
import com.ejr.managekitchen.operations.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Inscription publique : toujours un rôle CLIENT. */
    public User registerClient(String email, String rawPassword, String fullName) {
        return createUser(email, rawPassword, fullName, Role.CLIENT);
    }

    /** Création par l'administrateur, quel que soit le rôle. */
    public UserResponse create(CreateUserRequest request) {
        return UserResponse.from(createUser(request.email(), request.password(), request.fullName(), request.role()));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(getEntity(id));
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getEntity(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return UserResponse.from(user);
    }

    /** Suppression logique : le compte est désactivé (ses commandes restent consultables). */
    public void disable(Long id) {
        getEntity(id).setEnabled(false);
    }

    private User createUser(String email, String rawPassword, String fullName, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Un compte existe déjà avec cet e-mail");
        }
        return userRepository.save(new User(normalizedEmail, passwordEncoder.encode(rawPassword), fullName, role));
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
    }
}
