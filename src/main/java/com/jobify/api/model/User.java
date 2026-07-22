package com.jobify.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;  // BCrypt hashed, nullable for OAuth users

    @Column(name = "name")
    private String name;

    @Column(name = "picture")
    private String picture;

    @Column(name = "auth_provider")
    private String authProvider; // e.g. "LOCAL", "GOOGLE"

    @Column(name = "google_id", unique = true)
    private String googleId;

    // Null until the user finishes (or skips) the first-login feature tour.
    // Stored server-side so the tour never reappears after re-login or on another device.
    @Column(name = "onboarding_completed_at")
    private OffsetDateTime onboardingCompletedAt;

}