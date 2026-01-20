package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsTest {

    private Settings settings;

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
    }

    @Test
    void testPopulateFromContext() {
        Context mockContext = mock(Context.class);
        
        when(mockContext.formParam("clientId")).thenReturn("test-client-id");
        when(mockContext.formParam("clientSecret")).thenReturn("test-client-secret");
        when(mockContext.formParam("wellKnown")).thenReturn("https://test.wellknown.url");
        when(mockContext.formParam("callbackUrl")).thenReturn("http://localhost:8080/callback");
        when(mockContext.formParam("scopes")).thenReturn("openid profile");
        when(mockContext.formParam("state")).thenReturn("test-state-123");
        
        settings.populate(mockContext);
        
        assertEquals("test-client-id", settings.clientId);
        assertEquals("test-client-secret", settings.clientSecret);
        assertEquals("https://test.wellknown.url", settings.wellKnown);
        assertEquals("http://localhost:8080/callback", settings.callbackUrl);
        assertEquals("openid profile", settings.scopes);
        assertEquals("test-state-123", settings.state);
    }

    @Test
    void testGetBasicAuthHeader() {
        settings.clientId = "myClientId";
        settings.clientSecret = "myClientSecret";
        
        String expectedHeader = Base64.getEncoder().encodeToString("myClientId:myClientSecret".getBytes());
        
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
        
        assertEquals("only-access-token", settings.accessToken);
        assertEquals("", settings.idToken);
        assertEquals("", settings.refreshToken);
    }

    @Test
    void testGetAccessTokenDetailsReturnsNullWhenNoToken() {
        settings.accessToken = null;
        assertNull(settings.getAccessTokenDetails());
        
        settings.accessToken = "";
        assertNull(settings.getAccessTokenDetails());
    }

    @Test
    void testGetExpirationReturnsNullWhenNoExp() {
        settings.exp = null;
        assertNull(settings.getExpiration());
    }

    @Test
    void testGetExpirationReturnsDateTimeWhenExpSet() {
        settings.exp = 3600L;
        String expiration = settings.getExpiration();
        
        assertNotNull(expiration);
        assertTrue(expiration.contains("T"));
    }

    @Test
    void testStateIsUUID() {
        Settings newSettings = new Settings();
        assertNotNull(newSettings.state);
        assertDoesNotThrow(() -> java.util.UUID.fromString(newSettings.state));
    }
}
