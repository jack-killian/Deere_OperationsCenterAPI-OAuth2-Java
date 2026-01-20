package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsTest {

    private Settings settings;

    @Mock
    private Context mockContext;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    void shouldPopulateSettingsFromContext() {
        when(mockContext.formParam("clientId")).thenReturn("test-client-id");
        when(mockContext.formParam("clientSecret")).thenReturn("test-client-secret");
        when(mockContext.formParam("wellKnown")).thenReturn("https://example.com/.well-known");
        when(mockContext.formParam("callbackUrl")).thenReturn("http://localhost:9090/callback");
        when(mockContext.formParam("scopes")).thenReturn("openid profile");
        when(mockContext.formParam("state")).thenReturn("test-state");

        settings.populate(mockContext);

        assertEquals("test-client-id", settings.clientId);
        assertEquals("test-client-secret", settings.clientSecret);
        assertEquals("https://example.com/.well-known", settings.wellKnown);
        assertEquals("http://localhost:9090/callback", settings.callbackUrl);
        assertEquals("openid profile", settings.scopes);
        assertEquals("test-state", settings.state);
    }

    @Test
    void shouldGenerateCorrectBasicAuthHeader() {
        settings.clientId = "myClientId";
        settings.clientSecret = "myClientSecret";

        String authHeader = settings.getBasicAuthHeader();

        String expected = Base64.getEncoder().encodeToString("myClientId:myClientSecret".getBytes());
        assertEquals(expected, authHeader);
    }

    @Test
    void shouldUpdateTokenInfoFromJsonResponse() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        tokenResponse.put("expires_in", 3600L);

        settings.updateTokenInfo(tokenResponse);

        assertEquals("test-id-token", settings.idToken);
        assertEquals("test-access-token", settings.accessToken);
        assertEquals("test-refresh-token", settings.refreshToken);
        assertEquals(3600L, settings.exp);
    }

    @Test
    void shouldHandleMissingTokenFieldsGracefully() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "only-access-token");

        settings.updateTokenInfo(tokenResponse);

        assertEquals("only-access-token", settings.accessToken);
        assertEquals("", settings.idToken);
        assertEquals("", settings.refreshToken);
    }

    @Test
    void shouldReturnNullExpirationWhenExpIsNull() {
        settings.exp = null;

        String expiration = settings.getExpiration();

        assertNull(expiration);
    }

    @Test
    void shouldReturnExpirationWhenExpIsSet() {
        settings.exp = 3600L;

        String expiration = settings.getExpiration();

        assertNotNull(expiration);
    }

    @Test
    void shouldReturnNullAccessTokenDetailsWhenTokenIsNull() {
        settings.accessToken = null;

        String details = settings.getAccessTokenDetails();

        assertNull(details);
    }

    @Test
    void shouldReturnNullAccessTokenDetailsWhenTokenIsEmpty() {
        settings.accessToken = "";

        String details = settings.getAccessTokenDetails();

        assertNull(details);
    }

    @Test
    void shouldDecodeValidJwtAccessToken() {
        String header = Base64.getEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getEncoder().encodeToString("{\"sub\":\"user123\",\"exp\":1234567890}".getBytes());
        String signature = "signature";
        settings.accessToken = header + "." + payload + "." + signature;

        String details = settings.getAccessTokenDetails();

        assertNotNull(details);
        assertTrue(details.contains("user123"));
        assertTrue(details.contains("1234567890"));
    }

    @Test
    void shouldHaveDefaultServerUrl() {
        assertEquals("http://localhost:9090", Settings.SERVER_URL);
    }

    @Test
    void shouldHaveDefaultCallbackUrl() {
        Settings newSettings = new Settings();
        assertEquals("http://localhost:9090/callback", newSettings.callbackUrl);
    }

    @Test
    void shouldHaveDefaultApiUrl() {
        Settings newSettings = new Settings();
        assertEquals("https://sandboxapi.deere.com/platform", newSettings.apiUrl);
    }

    @Test
    void shouldHaveDefaultScopes() {
        Settings newSettings = new Settings();
        assertEquals("openid profile offline_access ag1 eq1", newSettings.scopes);
    }

    @Test
    void shouldGenerateUniqueStateOnConstruction() {
        Settings settings1 = new Settings();
        Settings settings2 = new Settings();

        assertNotNull(settings1.state);
        assertNotNull(settings2.state);
        assertNotEquals(settings1.state, settings2.state);
    }

    @Test
    void shouldHandleSpecialCharactersInCredentials() {
        settings.clientId = "client:with:colons";
        settings.clientSecret = "secret=with+special&chars";

        String authHeader = settings.getBasicAuthHeader();

        String decoded = new String(Base64.getDecoder().decode(authHeader));
        assertEquals("client:with:colons:secret=with+special&chars", decoded);
    }

    @Test
    void shouldHandleEmptyCredentials() {
        settings.clientId = "";
        settings.clientSecret = "";

        String authHeader = settings.getBasicAuthHeader();

        String decoded = new String(Base64.getDecoder().decode(authHeader));
        assertEquals(":", decoded);
    }
}
