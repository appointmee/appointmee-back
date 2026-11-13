package com.gg.turnlook.auth.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor @Data
public class LoginResponseDTO {
    private String token;
    private String refreshToken;
    private long expiresIn;
    public String getAccessToken() { return token; }
}
