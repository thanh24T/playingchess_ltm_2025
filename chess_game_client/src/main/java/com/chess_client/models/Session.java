package com.chess_client.models;

/**
 * Lightweight session holder for per-window state (placeholder).
 * Currently not wired into services — intended for future refactor to support per-window tokens.
 */
public class Session {
    private String accessToken;
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
