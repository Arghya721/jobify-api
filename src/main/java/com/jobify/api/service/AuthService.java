package com.jobify.api.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jobify.api.dto.AuthResponse;
import com.jobify.api.dto.GoogleLoginRequest;
import com.jobify.api.dto.LoginRequest;
import com.jobify.api.dto.RefreshRequest;
import com.jobify.api.dto.RegisterRequest;
import com.jobify.api.model.RefreshToken;
import com.jobify.api.model.User;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.auth.google-client-id}")
    private String googleClientId;

    public String register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider("LOCAL")
                .build();

        userRepository.save(user);
        return "Registered successfully";
    }

    public String login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository
                .findByEmail(request.getEmail()).get();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword() != null ? user.getPassword() : "")
                        .build();

        return jwtService.generateToken(userDetails);
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // Verify the ID Token
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String subject = payload.getSubject(); // Google ID

                // Find or format User
                Optional<User> userOptional = userRepository.findByEmail(email);
                User user;
                if (userOptional.isPresent()) {
                    user = userOptional.get();
                    // Update user details if needed
                    user.setName(name);
                    user.setPicture(pictureUrl);
                    if (user.getGoogleId() == null) {
                        user.setGoogleId(subject);
                        user.setAuthProvider("GOOGLE");
                    }
                    user = userRepository.save(user);
                } else {
                    user = User.builder()
                            .email(email)
                            .name(name)
                            .picture(pictureUrl)
                            .authProvider("GOOGLE")
                            .googleId(subject)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString())) // dummy password
                            .build();
                    user = userRepository.save(user);
                }

                // Generate Auth Tokens
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .build();

                String jwt = jwtService.generateToken(userDetails);
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

                return AuthResponse.builder()
                        .accessToken(jwt)
                        .refreshToken(refreshToken.getToken())
                        .user(new AuthResponse.UserDto(user.getEmail(), user.getName(), user.getPicture()))
                        .build();

            } else {
                throw new RuntimeException("Invalid ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed", e);
        }
    }

    public AuthResponse refreshToken(RefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails userDetails = org.springframework.security.core.userdetails.User
                            .builder()
                            .username(user.getEmail())
                            .password(user.getPassword())
                            .build();
                    String jwt = jwtService.generateToken(userDetails);
                    return AuthResponse.builder()
                            .accessToken(jwt)
                            .refreshToken(request.getRefreshToken()) // Return the same refresh token
                            .user(new AuthResponse.UserDto(user.getEmail(), user.getName(), user.getPicture()))
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
