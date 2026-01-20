package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Settings Tests")
class SettingsTest {

    private Settings settings;

    @Mock
    private Context mockContext;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    @DisplayName("Should generate correct Basic Auth header for API authorization")
    void shouldGenerateCorrectBasicAuthHeader() {
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-client-secret";

        String authHeader = settings.getBasicAuthHeader();

        String expectedHeader = Base64.getEncoder().encodeToString("test-client-id:test-client-secret".getBytes());
        assertEquals(expectedHeader, authHeader);
    }

    @Test
    @DisplayName("Should handle empty client credentials for authorization")
    void shouldHandleEmptyClientCredentials() {
        settings.clientId = "";
        settings.clientSecret = "";

        String authHeader = settings.getBasicAuthHeader();

        String expectedHeader = Base64.getEncoder().encodeToString(":".getBytes());
        assertEquals(expectedHeader, authHeader);
    }

    @Test
    @DisplayName("Should handle special characters in client credentials")
    void shouldHandleSpecialCharactersInCredentials() {
        settings.clientId = "client:with:colons";
        settings.clientSecret = "secret@with#special!chars";

        String authHeader = settings.getBasicAuthHeader();

        String expectedHeader = Base64.getEncoder().encodeToString("client:with:colons:secret@with#special!chars".getBytes());
        assertEquals(expectedHeader, authHeader);
    }

    @Test
    @DisplayName("Should update token info from OAuth response")
    void shouldUpdateTokenInfoFromOAuthResponse() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("expires_in", 3600L);

        settings.updateTokenInfo(tokenResponse);

        assertEquals("test-access-token", settings.accessToken);
        assertEquals("test-refresh-token", settings.refreshToken);
        assertEquals("test-id-token", settings.idToken);
        assertEquals(3600L, settings.exp);
    }

    @Test
    @DisplayName("Should handle missing tokens in OAuth response")
    void shouldHandleMissingTokensInOAuthResponse() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "test-access-token");

        settings.updateTokenInfo(tokenResponse);

        assertEquals("test-access-token", settings.accessToken);
        assertEquals("", settings.refreshToken);
        assertEquals("", settings.idToken);
    }

    @Test
    @DisplayName("Should return null for access token details when no token present")
    void shouldReturnNullForAccessTokenDetailsWhenNoToken() {
        settings.accessToken = null;

        String details = settings.getAccessTokenDetails();

        assertNull(details);
    }

    @Test
    @DisplayName("Should return null for access token details when token is empty")
    void shouldReturnNullForAccessTokenDetailsWhenTokenEmpty() {
        settings.accessToken = "";

        String details = settings.getAccessTokenDetails();

        assertNull(details);
    }

    @Test
    @DisplayName("Should return null for expiration when exp is null")
    void shouldReturnNullForExpirationWhenExpIsNull() {
        settings.exp = null;

        String expiration = settings.getExpiration();

        assertNull(expiration);
    }

    @Test
    @DisplayName("Should return expiration time when exp is set")
    void shouldReturnExpirationTimeWhenExpIsSet() {
        settings.exp = 3600L;

        String expiration = settings.getExpiration();

        assertNotNull(expiration);
    }

    @Test
    @DisplayName("Should populate settings from form context")
    void shouldPopulateSettingsFromFormContext() {
        when(mockContext.formParam("clientId")).thenReturn("form-client-id");
        when(mockContext.formParam("clientSecret")).thenReturn("form-client-secret");
        when(mockContext.formParam("wellKnown")).thenReturn("https://example.com/.well-known");
        when(mockContext.formParam("callbackUrl")).thenReturn("http://localhost:9090/callback");
        when(mockContext.formParam("scopes")).thenReturn("openid profile");
        when(mockContext.formParam("state")).thenReturn("test-state");

        settings.populate(mockContext);

        assertEquals("form-client-id", settings.clientId);
        assertEquals("form-client-secret", settings.clientSecret);
        assertEquals("https://example.com/.well-known", settings.wellKnown);
        assertEquals("http://localhost:9090/callback", settings.callbackUrl);
        assertEquals("openid profile", settings.scopes);
        assertEquals("test-state", settings.state);
    }

    @Test
    @DisplayName("Should have default API URL pointing to sandbox")
    void shouldHaveDefaultApiUrlPointingToSandbox() {
        assertEquals("https://sandboxapi.deere.com/platform", settings.apiUrl);
    }

    @Test
    @DisplayName("Should have default callback URL with server URL")
    void shouldHaveDefaultCallbackUrl() {
        assertEquals(Settings.SERVER_URL + "/callback", settings.callbackUrl);
    }

    @Test
    @DisplayName("Should have default scopes including offline_access for refresh tokens")
    void shouldHaveDefaultScopesIncludingOfflineAccess() {
        assertTrue(settings.scopes.contains("offline_access"));
    }

    @Test
    @DisplayName("Should generate unique state for CSRF protection")
    void shouldGenerateUniqueStateForCsrfProtection() {
        Settings settings1 = new Settings();
        Settings settings2 = new Settings();

        assertNotEquals(settings1.state, settings2.state);
    }
}
