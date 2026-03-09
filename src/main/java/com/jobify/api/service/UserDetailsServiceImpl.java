package com.jobify.api.service;

import com.jobify.api.model.User;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

     private final UserRepository userRepository;

     @Override
     public UserDetails loadUserByUsername(String email)
             throws UsernameNotFoundException {

          // Load your user from DB
          User user = userRepository.findByEmail(email)
                  .orElseThrow(() ->
                          new UsernameNotFoundException("User not found: " + email));

          // Convert to Spring Security's UserDetails format
          return org.springframework.security.core.userdetails.User
                  .builder()
                  .username(user.getEmail())
                  .password(user.getPassword())
                  .build();
     }
}
