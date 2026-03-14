package com.jobify.api.repository;

import com.jobify.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Make sure this method exists
    Optional<User> findByEmail(String email);
}