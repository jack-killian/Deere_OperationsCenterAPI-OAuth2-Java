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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Settings Management Tests")
class SettingsTest {

    private Settings settings;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    @DisplayName("Settings should have default values initialized")
    void settings_shouldHaveDefaultValues() {
        assertThat(settings.wellKnown).isEqualTo("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/.well-known/oauth-authorization-server");
        assertThat(settings.apiUrl).isEqualTo("https://sandboxapi.deere.com/platform");
        assertThat(settings.callbackUrl).isEqualTo("http://localhost:9090/callback");
        assertThat(settings.scopes).isEqualTo("openid profile offline_access ag1 eq1");
        assertThat(settings.state).isNotNull();
        assertThat(settings.clientId).isEmpty();
        assertThat(settings.clientSecret).isEmpty();
    }

    @Test
    @DisplayName("Populate should extract all form parameters from context")
    void populate_shouldExtractFormParameters() {
        when(context.formParam("clientId")).thenReturn("my-client-id");
        when(context.formParam("clientSecret")).thenReturn("my-client-secret");
        when(context.formParam("wellKnown")).thenReturn("https://custom.auth.server/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://myapp.com/callback");
        when(context.formParam("scopes")).thenReturn("openid profile custom_scope");
        when(context.formParam("state")).thenReturn("custom-state-value");

        settings.populate(context);

        assertThat(settings.clientId).isEqualTo("my-client-id");
        assertThat(settings.clientSecret).isEqualTo("my-client-secret");
        assertThat(settings.wellKnown).isEqualTo("https://custom.auth.server/.well-known");
        assertThat(settings.callbackUrl).isEqualTo("http://myapp.com/callback");
        assertThat(settings.scopes).isEqualTo("openid profile custom_scope");
        assertThat(settings.state).isEqualTo("custom-state-value");
    }

    @Test
    @DisplayName("Basic auth header should be correctly Base64 encoded")
    void getBasicAuthHeader_shouldReturnBase64EncodedCredentials() {
        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";

        String authHeader = settings.getBasicAuthHeader();

        String decoded = new String(Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("test-client:test-secret");
    }

    @Test
    @DisplayName("Basic auth header should handle special characters")
    void getBasicAuthHeader_shouldHandleSpecialCharacters() {
        settings.clientId = "client@example.com";
        settings.clientSecret = "secret!@#$%^&*()";

        String authHeader = settings.getBasicAuthHeader();

        String decoded = new String(Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("client@example.com:secret!@#$%^&*()");
    }

    @Test
    @DisplayName("Update token info should store all token fields")
    void updateTokenInfo_shouldStoreAllTokenFields() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token-123");
        tokenResponse.put("refresh_token", "refresh-token-456");
        tokenResponse.put("id_token", "id-token-789");
        tokenResponse.put("expires_in", 3600);

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.accessToken).isEqualTo("access-token-123");
        assertThat(settings.refreshToken).isEqualTo("refresh-token-456");
        assertThat(settings.idToken).isEqualTo("id-token-789");
        assertThat(settings.exp).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Update token info should handle missing optional fields")
    void updateTokenInfo_shouldHandleMissingOptionalFields() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token-only");

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.accessToken).isEqualTo("access-token-only");
        assertThat(settings.refreshToken).isEmpty();
        assertThat(settings.idToken).isEmpty();
    }

    @Test
    @DisplayName("Get access token details should decode JWT payload")
    void getAccessTokenDetails_shouldDecodeJwtPayload() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"user123\",\"name\":\"Test User\",\"iat\":1516239022}".getBytes());
        String signature = "signature";
        settings.accessToken = header + "." + payload + "." + signature;

        String details = settings.getAccessTokenDetails();

        assertThat(details).contains("user123");
        assertThat(details).contains("Test User");
    }

    @Test
    @DisplayName("Get access token details should return null when token is empty")
    void getAccessTokenDetails_shouldReturnNullWhenTokenEmpty() {
        settings.accessToken = "";

        String details = settings.getAccessTokenDetails();

        assertThat(details).isNull();
    }

    @Test
    @DisplayName("Get access token details should return null when token is null")
    void getAccessTokenDetails_shouldReturnNullWhenTokenNull() {
        settings.accessToken = null;

        String details = settings.getAccessTokenDetails();

        assertThat(details).isNull();
    }

    @Test
    @DisplayName("Get expiration should return formatted datetime")
    void getExpiration_shouldReturnFormattedDatetime() {
        settings.exp = 3600L;

        String expiration = settings.getExpiration();

        assertThat(expiration).isNotNull();
        assertThat(expiration).contains("T");
    }

    @Test
    @DisplayName("Get expiration should return null when exp is null")
    void getExpiration_shouldReturnNullWhenExpNull() {
        settings.exp = null;

        String expiration = settings.getExpiration();

        assertThat(expiration).isNull();
    }

    @Test
    @DisplayName("State should be a valid UUID format")
    void state_shouldBeValidUuidFormat() {
        assertThat(settings.state).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("SERVER_URL constant should be localhost:9090")
    void serverUrl_shouldBeLocalhost9090() {
        assertThat(Settings.SERVER_URL).isEqualTo("http://localhost:9090");
    }
}
