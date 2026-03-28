package com.jobify.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginRequest {
    
    // The Google ID Token string sent from NextAuth
    private String idToken;
    
    // Metadata passed from NextAuth
    private String email;
    private String name;
    private String picture;
}
