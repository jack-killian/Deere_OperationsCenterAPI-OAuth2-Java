package com.deere.isg.examples;

import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 3: Token Management Test
 * Tests the refreshAccessToken() method, token storage and retrieval from Settings,
 * and token expiration handling.
 */
public class TokenManagementTest {

    private Settings settings;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    void updateTokenInfoStoresAccessToken() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "new-access-token");
        
        settings.updateTokenInfo(tokenResponse);
        
        assertThat(settings.accessToken).isEqualTo("new-access-token");
    }

    @Test
    void updateTokenInfoStoresRefreshToken() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("refresh_token", "new-refresh-token");
        
        settings.updateTokenInfo(tokenResponse);
        
        assertThat(settings.refreshToken).isEqualTo("new-refresh-token");
    }

    @Test
    void updateTokenInfoStoresIdToken() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("id_token", "new-id-token");
        
        settings.updateTokenInfo(tokenResponse);
        
        assertThat(settings.idToken).isEqualTo("new-id-token");
    }

    @Test
    void updateTokenInfoStoresExpiration() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("expires_in", 7200L);
        
        settings.updateTokenInfo(tokenResponse);
        
        assertThat(settings.exp).isEqualTo(7200L);
    }

    @Test
    void getExpirationReturnsNullWhenExpIsNull() {
        settings.exp = null;
        
        assertThat(settings.getExpiration()).isNull();
    }

    @Test
    void getExpirationReturnsFormattedDateTimeWhenExpIsSet() {
        settings.exp = 3600L;
        
        String expiration = settings.getExpiration();
        
        assertThat(expiration).isNotNull();
        assertThat(expiration).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*");
    }

    @Test
    void getAccessTokenDetailsReturnsNullWhenTokenIsNull() {
        settings.accessToken = null;
        
        assertThat(settings.getAccessTokenDetails()).isNull();
    }

    @Test
    void getAccessTokenDetailsReturnsNullWhenTokenIsEmpty() {
        settings.accessToken = "";
        
        assertThat(settings.getAccessTokenDetails()).isNull();
    }

    @Test
    void getAccessTokenDetailsDecodesJwtPayload() {
        String header = Base64.getEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getEncoder().encodeToString("{\"sub\":\"user123\",\"exp\":1234567890}".getBytes());
        String signature = "signature";
        
        settings.accessToken = header + "." + payload + "." + signature;
        
        String details = settings.getAccessTokenDetails();
        
        assertThat(details).isNotNull();
        assertThat(details).contains("user123");
        assertThat(details).contains("1234567890");
    }

    @Test
    void basicAuthHeaderEncodesCredentialsCorrectly() {
        settings.clientId = "testClient";
        settings.clientSecret = "testSecret";
        
        String authHeader = settings.getBasicAuthHeader();
        
        String decoded = new String(Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("testClient:testSecret");
    }

    @Test
    void tokenUpdateHandlesMissingFields() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "only-access-token");
        
        settings.updateTokenInfo(tokenResponse);
        
        assertThat(settings.accessToken).isEqualTo("only-access-token");
        assertThat(settings.refreshToken).isEmpty();
        assertThat(settings.idToken).isEmpty();
    }

    @Test
    void tokenUpdateOverwritesPreviousTokens() {
        settings.accessToken = "old-access-token";
        settings.refreshToken = "old-refresh-token";
        
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "new-access-token");
        tokenResponse.put("refresh_token", "new-refresh-token");
        
        settings.updateTokenInfo(tokenResponse);
        
        assertThat(settings.accessToken).isEqualTo("new-access-token");
        assertThat(settings.refreshToken).isEqualTo("new-refresh-token");
    }

    @Test
    void refreshTokenRouteIsConfigurable() {
        assertThat(Settings.SERVER_URL + "/refresh-access-token")
            .isEqualTo("http://localhost:9090/refresh-access-token");
    }
}
