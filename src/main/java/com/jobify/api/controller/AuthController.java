package com.jobify.api.controller;

import com.jobify.api.dto.AuthResponse;
import com.jobify.api.dto.LoginRequest;
import com.jobify.api.dto.RegisterRequest;
import com.jobify.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google/login")
    public ResponseEntity<AuthResponse> googleLogin(
            @RequestBody com.jobify.api.dto.GoogleLoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        String userAgent = httpRequest.getHeader("User-Agent");
        // Cloud Run headers or custom fallback wrappers (like cloudflare)
        String ipAddress = httpRequest.getHeader("X-Forwarded-For"); 
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = httpRequest.getRemoteAddr();
        }
        
        String country = httpRequest.getHeader("X-Custom-Country");
        String city = httpRequest.getHeader("X-Custom-City");
        if (ipAddress != null && ipAddress.contains(",")) {
            // X-Forwarded-For often contains multiple IPs (e.g. client, proxy1)
            ipAddress = ipAddress.split(",")[0].trim();
        }

        String location = null;
        if (country != null && !country.isEmpty()) {
            location = (city != null ? city + ", " : "") + country;
        } else if (ipAddress != null && !ipAddress.equals("127.0.0.1") && !ipAddress.equals("0:0:0:0:0:0:0:1")) {
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                java.util.Map<String, Object> response = restTemplate.getForObject("http://ip-api.com/json/" + ipAddress, java.util.Map.class);
                if (response != null && "success".equals(response.get("status"))) {
                    location = response.get("city") + ", " + response.get("country");
                }
            } catch (Exception e) {
                location = "Unknown Location";
            }
        }

        return ResponseEntity.ok(authService.googleLogin(request, userAgent, ipAddress, location));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody com.jobify.api.dto.RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @org.springframework.web.bind.annotation.GetMapping("/sessions")
    public ResponseEntity<java.util.List<com.jobify.api.model.RefreshToken>> getActiveSessions(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.getActiveSessions(principal.getName()));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@org.springframework.web.bind.annotation.PathVariable Long id, java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        authService.revokeSession(principal.getName(), id);
        return ResponseEntity.ok().build();
    }
}