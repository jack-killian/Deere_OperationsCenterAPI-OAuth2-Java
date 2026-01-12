package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SettingsTest {

    private Settings settings;

    @Mock
    private Context mockContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
        when(mockContext.formParam("wellKnown")).thenReturn("https://test.wellknown.url");
        when(mockContext.formParam("callbackUrl")).thenReturn("http://test.callback.url");
        when(mockContext.formParam("scopes")).thenReturn("openid profile");
        when(mockContext.formParam("state")).thenReturn("test-state");

        settings.populate(mockContext);

        assertEquals("test-client-id", settings.clientId);
        assertEquals("test-client-secret", settings.clientSecret);
        assertEquals("https://test.wellknown.url", settings.wellKnown);
        assertEquals("http://test.callback.url", settings.callbackUrl);
        assertEquals("openid profile", settings.scopes);
        assertEquals("test-state", settings.state);
    }

    @Test
    void testGetBasicAuthHeader() {
        settings.clientId = "myClientId";
        settings.clientSecret = "myClientSecret";

        String expectedHeader = Base64.getEncoder().encodeToString("myClientId:myClientSecret".getBytes());
        assertEquals(expectedHeader, settings.getBasicAuthHeader());
    }

    @Test
    void testGetBasicAuthHeaderWithEmptyCredentials() {
        settings.clientId = "";
        settings.clientSecret = "";

        String expectedHeader = Base64.getEncoder().encodeToString(":".getBytes());
        assertEquals(expectedHeader, settings.getBasicAuthHeader());
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
        assertNull(settings.getExpiration());
    }

    @Test
    void testGetExpirationWithValidExp() {
        settings.exp = 3600L;
        String expiration = settings.getExpiration();
        assertNotNull(expiration);
        assertTrue(expiration.length() > 0);
    }

    @Test
    void testGetAccessTokenDetailsWithNullToken() {
        settings.accessToken = null;
        assertNull(settings.getAccessTokenDetails());
    }

    @Test
    void testGetAccessTokenDetailsWithEmptyToken() {
        settings.accessToken = "";
        assertNull(settings.getAccessTokenDetails());
    }

    @Test
    void testGetAccessTokenDetailsWithValidJwt() {
        String header = Base64.getEncoder().encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getEncoder().encodeToString("{\"sub\":\"1234567890\",\"name\":\"Test User\",\"iat\":1516239022}".getBytes());
        String signature = "test-signature";
        settings.accessToken = header + "." + payload + "." + signature;

        String details = settings.getAccessTokenDetails();
        assertNotNull(details);
        assertTrue(details.contains("sub"));
        assertTrue(details.contains("1234567890"));
        assertTrue(details.contains("name"));
        assertTrue(details.contains("Test User"));
    }

    @Test
    void testStateIsUniquePerInstance() {
        Settings settings1 = new Settings();
        Settings settings2 = new Settings();
        assertNotEquals(settings1.state, settings2.state);
    }
}
