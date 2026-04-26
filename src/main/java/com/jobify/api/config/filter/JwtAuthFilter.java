package com.jobify.api.config.filter;

import com.jobify.api.service.JwtService;
import com.jobify.api.service.RefreshTokenService;
import com.jobify.api.service.TokenBlacklistService;
import com.jobify.api.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ① Read header
        String authHeader = request.getHeader("Authorization");

        // ② No token — move on (permitAll URLs will pass through fine)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ③ Extract token
        String token = authHeader.substring(7);
        String userEmail = null;

        try {
            userEmail = jwtService.extractUsername(token);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
            return;
        }

        // ③b Reject immediately if jti is in the Redis blacklist (session was revoked)
        String jti = null;
        try {
            jti = jwtService.extractJti(token);
        } catch (Exception ignored) {}
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session has been revoked");
            return;
        }

        // ③c Also check via refresh token ID (rid) — primary revocation mechanism
        try {
            Long rid = jwtService.extractRefreshTokenId(token);
            if (refreshTokenService.isSessionRevoked(rid)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session has been revoked");
                return;
            }
        } catch (Exception ignored) {}

        // ④ Validate and set SecurityContext
        if (userEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(token, userDetails)) {

                // ✅ KEY LINE — tell Spring Security who this user is
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()  // roles
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }
        }

        // ⑤ Pass to next filter
        filterChain.doFilter(request, response);
    }

    // Completely skip this filter for public URLs
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/login") ||
                path.equals("/api/auth/register");
    }
}