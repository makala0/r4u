package cz.be.r4u.auth;

import cz.be.r4u.entity.UserAccount;
import cz.be.r4u.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(String username, String email, String password, String confirmPassword) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();

        validateRegistration(normalizedUsername, normalizedEmail, password, confirmPassword);

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Uživatelské jméno už existuje.");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("E-mail už existuje.");
        }

        String passwordHash = passwordEncoder.encode(password);
        UserAccount user = new UserAccount(normalizedUsername, normalizedEmail, passwordHash);

        return userRepository.save(user);
    }

    public UserAccount login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim();

        if (normalizedUsername.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Vyplň uživatelské jméno a heslo.");
        }

        UserAccount user = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Neplatné přihlašovací údaje."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Neplatné přihlašovací údaje.");
        }

        return user;
    }

    private void validateRegistration(String username, String email, String password, String confirmPassword) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Uživatelské jméno je povinné.");
        }

        if (email.isBlank()) {
            throw new IllegalArgumentException("E-mail je povinný.");
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException("E-mail nemá platný formát.");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Heslo musí mít alespoň 6 znaků.");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Hesla se neshodují.");
        }
    }
}