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
    void testDefaultValues() {
        assertEquals("http://localhost:9090", Settings.SERVER_URL);
        assertEquals("", settings.clientId);
        assertEquals("", settings.clientSecret);
        assertEquals("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/.well-known/oauth-authorization-server", settings.wellKnown);
        assertEquals("https://sandboxapi.deere.com/platform", settings.apiUrl);
        assertEquals("http://localhost:9090/callback", settings.callbackUrl);
        assertEquals("openid profile offline_access ag1 eq1", settings.scopes);
        assertNotNull(settings.state);
        assertNull(settings.idToken);
        assertNull(settings.accessToken);
        assertNull(settings.refreshToken);
        assertNull(settings.apiResponse);
        assertNull(settings.exp);
    }

    @Test
    void testPopulate() {
        when(mockContext.formParam("clientId")).thenReturn("test-client-id");
        when(mockContext.formParam("clientSecret")).thenReturn("test-client-secret");
        when(mockContext.formParam("wellKnown")).thenReturn("https://example.com/.well-known");
        when(mockContext.formParam("callbackUrl")).thenReturn("http://localhost:8080/callback");
        when(mockContext.formParam("scopes")).thenReturn("openid profile");
        when(mockContext.formParam("state")).thenReturn("test-state-123");

        settings.populate(mockContext);

        assertEquals("test-client-id", settings.clientId);
        assertEquals("test-client-secret", settings.clientSecret);
        assertEquals("https://example.com/.well-known", settings.wellKnown);
        assertEquals("http://localhost:8080/callback", settings.callbackUrl);
        assertEquals("openid profile", settings.scopes);
        assertEquals("test-state-123", settings.state);
    }

    @Test
    void testGetBasicAuthHeader() {
        settings.clientId = "myClientId";
        settings.clientSecret = "myClientSecret";

        String result = settings.getBasicAuthHeader();

        String expected = Base64.getEncoder().encodeToString("myClientId:myClientSecret".getBytes());
        assertEquals(expected, result);
    }

    @Test
    void testGetBasicAuthHeaderWithEmptyCredentials() {
        settings.clientId = "";
        settings.clientSecret = "";

        String result = settings.getBasicAuthHeader();

        String expected = Base64.getEncoder().encodeToString(":".getBytes());
        assertEquals(expected, result);
    }

    @Test
    void testGetBasicAuthHeaderWithSpecialCharacters() {
        settings.clientId = "client:with:colons";
        settings.clientSecret = "secret@with#special!chars";

        String result = settings.getBasicAuthHeader();

        String expected = Base64.getEncoder().encodeToString("client:with:colons:secret@with#special!chars".getBytes());
        assertEquals(expected, result);
    }

    @Test
    void testUpdateTokenInfo() {
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
    void testUpdateTokenInfoWithMissingFields() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "only-access-token");

        settings.updateTokenInfo(tokenResponse);

        assertEquals("", settings.idToken);
        assertEquals("only-access-token", settings.accessToken);
        assertEquals("", settings.refreshToken);
        assertEquals(0L, settings.exp);
    }

    @Test
    void testGetExpirationWithNullExp() {
        settings.exp = null;

        String result = settings.getExpiration();

        assertNull(result);
    }

    @Test
    void testGetExpirationWithValidExp() {
        settings.exp = 3600L;

        String result = settings.getExpiration();

        assertNotNull(result);
        assertTrue(result.contains("T"));
    }

    @Test
    void testGetAccessTokenDetailsWithNullToken() {
        settings.accessToken = null;

        String result = settings.getAccessTokenDetails();

        assertNull(result);
    }

    @Test
    void testGetAccessTokenDetailsWithEmptyToken() {
        settings.accessToken = "";

        String result = settings.getAccessTokenDetails();

        assertNull(result);
    }

    @Test
    void testGetAccessTokenDetailsWithValidJwt() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1234567890\",\"name\":\"Test User\",\"iat\":1516239022}".getBytes());
        String signature = "test-signature";
        settings.accessToken = header + "." + payload + "." + signature;

        String result = settings.getAccessTokenDetails();

        assertNotNull(result);
        assertTrue(result.contains("sub"));
        assertTrue(result.contains("1234567890"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("Test User"));
    }

    @Test
    void testStateIsUniquePerInstance() {
        Settings settings1 = new Settings();
        Settings settings2 = new Settings();

        assertNotEquals(settings1.state, settings2.state);
    }
}
