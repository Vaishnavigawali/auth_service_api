package com.library.auth.service;

import com.library.auth.dto.*;
import com.library.auth.entity.PasswordResetToken;
import com.library.auth.entity.UserCredentials;
import com.library.auth.repository.PasswordResetTokenRepository;
import com.library.auth.repository.UserCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

	@Autowired
    private UserCredentialsRepository userCredentialsRepository;

	@Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

	@Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Registering user: {}", request.getEmail());

        if (userCredentialsRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userCredentialsRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String requestedRole = request.getRole() != null ? request.getRole().toUpperCase() : "STUDENT";
        if (!requestedRole.equals("STUDENT") && !requestedRole.equals("LIBRARIAN") && !requestedRole.equals("ADMIN")) {
            throw new RuntimeException("Invalid role specified");
        }

        UserCredentials userCredentials = UserCredentials.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(requestedRole)
                .active(true)
                .build();

        userCredentialsRepository.save(userCredentials);
        log.info("User registered successfully: {}", request.getUsername());
    }


    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        UserCredentials user = userCredentialsRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!user.getActive()) {
            throw new RuntimeException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
        
        // TODO: Validate refresh token from cache/database
        // For now, generate a new access token
        String accessToken = generateAccessToken(null);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    public void logout(String token) {
        log.info("Logging out user with token");
        // TODO: Blacklist token in Redis
    }

    @Transactional
    public void forgotPassword(String email) {
        log.info("Forgot password request for: {}", email);

        if (!userCredentialsRepository.existsByEmail(email)) {
            throw new RuntimeException("Email not found");
        }

        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(resetToken)
                .email(email)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        passwordResetTokenRepository.save(passwordResetToken);

        // TODO: Send reset link via email
        log.info("Password reset token generated for: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Resetting password");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (resetToken.getUsed()) {
            throw new RuntimeException("Token already used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        UserCredentials user = userCredentialsRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userCredentialsRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for: {}", user.getUsername());
    }

    private String generateAccessToken(UserCredentials user) {
        // TODO: Implement JWT token generation
        // For now, return a dummy token
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}
