package com.deere.isg.examples;

import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

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
        assertNull(settings.idToken);
        assertNull(settings.accessToken);
        assertNull(settings.refreshToken);
        assertNull(settings.apiResponse);
        assertNull(settings.exp);
    }

    @Test
    void testGetBasicAuthHeader() {
        settings.clientId = "testClientId";
        settings.clientSecret = "testClientSecret";

        String result = settings.getBasicAuthHeader();

        String expected = Base64.getEncoder().encodeToString("testClientId:testClientSecret".getBytes());
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
        settings.clientId = "client@id!";
        settings.clientSecret = "secret#123$";

        String result = settings.getBasicAuthHeader();

        String expected = Base64.getEncoder().encodeToString("client@id!:secret#123$".getBytes());
        assertEquals(expected, result);
    }

    @Test
    void testUpdateTokenInfo() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("id_token", "test_id_token");
        tokenResponse.put("access_token", "test_access_token");
        tokenResponse.put("refresh_token", "test_refresh_token");
        tokenResponse.put("expires_in", 3600L);

        settings.updateTokenInfo(tokenResponse);

        assertEquals("test_id_token", settings.idToken);
        assertEquals("test_access_token", settings.accessToken);
        assertEquals("test_refresh_token", settings.refreshToken);
        assertEquals(3600L, settings.exp);
    }

    @Test
    void testUpdateTokenInfoWithMissingFields() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "test_access_token");

        settings.updateTokenInfo(tokenResponse);

        assertEquals("", settings.idToken);
        assertEquals("test_access_token", settings.accessToken);
        assertEquals("", settings.refreshToken);
        assertEquals(0L, settings.exp);
    }

    @Test
    void testGetExpirationReturnsNullWhenExpIsNull() {
        settings.exp = null;

        String result = settings.getExpiration();

        assertNull(result);
    }

    @Test
    void testGetExpirationReturnsDateTimeWhenExpIsSet() {
        settings.exp = 3600L;

        String result = settings.getExpiration();

        assertNotNull(result);
        assertTrue(result.contains("T"));
    }

    @Test
    void testGetAccessTokenDetailsReturnsNullWhenAccessTokenIsNull() {
        settings.accessToken = null;

        String result = settings.getAccessTokenDetails();

        assertNull(result);
    }

    @Test
    void testGetAccessTokenDetailsReturnsNullWhenAccessTokenIsEmpty() {
        settings.accessToken = "";

        String result = settings.getAccessTokenDetails();

        assertNull(result);
    }

    @Test
    void testGetAccessTokenDetailsDecodesJwtPayload() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"user123\",\"exp\":1234567890}".getBytes());
        String signature = "signature";
        settings.accessToken = header + "." + payload + "." + signature;

        String result = settings.getAccessTokenDetails();

        assertNotNull(result);
        assertTrue(result.contains("user123"));
        assertTrue(result.contains("1234567890"));
    }

    @Test
    void testStateIsUniqueForEachInstance() {
        Settings settings1 = new Settings();
        Settings settings2 = new Settings();

        assertNotEquals(settings1.state, settings2.state);
    }
}
