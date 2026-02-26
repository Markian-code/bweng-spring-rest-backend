package at.technikum.springrestbackend.dto;

import at.technikum.springrestbackend.entity.Role;

public class AuthResponseDto {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInMs;

    private Long userId;
    private String email;
    private String username;
    private Role role;

    public AuthResponseDto() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTokenType(final String tokenType) {
        this.tokenType = tokenType;
    }

    public void setExpiresInMs(final long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setRole(final Role role) {
        this.role = role;
    }
}