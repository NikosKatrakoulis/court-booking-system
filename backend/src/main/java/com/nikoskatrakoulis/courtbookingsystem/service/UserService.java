package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.dto.response.AuthResponse;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.UserResponse;
import com.nikoskatrakoulis.courtbookingsystem.entity.RefreshToken;
import com.nikoskatrakoulis.courtbookingsystem.entity.Role;
import com.nikoskatrakoulis.courtbookingsystem.entity.User;
import com.nikoskatrakoulis.courtbookingsystem.exception.EmailAlreadyExistsException;
import com.nikoskatrakoulis.courtbookingsystem.exception.InvalidCredentialsException;
import com.nikoskatrakoulis.courtbookingsystem.exception.InvalidRefreshTokenException;
import com.nikoskatrakoulis.courtbookingsystem.repository.RefreshTokenRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenExpirationMs;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMs
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }


    @Transactional
    public AuthResponse login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user);

        String rawRefreshToken = generateSecureRandomToken();
        String hashedRefreshToken = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(hashedRefreshToken);
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken);
    }

    @Transactional
    public UserResponse register(String email, String password, String firstName, String lastName) {


        if (userRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException("Email is already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(hashedPassword);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setRole(Role.USER);

        User savedUser = userRepository.save(newUser);
        return toResponse(savedUser);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(),user.getFirstName(), user.getLastName(), user.getRole().name());
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {

        String hashedToken = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid token or token is expired"));

        if (storedToken.isRevoked()) {
            throw new InvalidRefreshTokenException("Invalid token or token is expired");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Invalid token or token is expired");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRawRefreshToken = generateSecureRandomToken();
        String newHashedRefreshToken = hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setToken(newHashedRefreshToken);
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000));
        newRefreshToken.setRevoked(false);

        refreshTokenRepository.save(newRefreshToken);

        return new AuthResponse(newAccessToken, newRawRefreshToken);

    }

    @Transactional
    public void logout(String rawRefreshToken) {

        String hashedToken = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByToken(hashedToken).
                orElseThrow(() -> new InvalidRefreshTokenException("Invalid token or token is expired"));

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    


}
