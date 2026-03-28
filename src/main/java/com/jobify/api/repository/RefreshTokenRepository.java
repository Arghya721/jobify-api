package com.jobify.api.repository;

import com.jobify.api.model.RefreshToken;
import com.jobify.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
    
    Optional<RefreshToken> findByUser(User user);

}
