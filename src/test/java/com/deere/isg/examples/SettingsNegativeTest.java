package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Settings Negative Tests - Validation and Error Handling")
class SettingsNegativeTest {

    private Settings settings;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    @DisplayName("Populate should handle null form parameters")
    void populate_shouldHandleNullFormParameters() {
        when(context.formParam("clientId")).thenReturn(null);
        when(context.formParam("clientSecret")).thenReturn(null);
        when(context.formParam("wellKnown")).thenReturn(null);
        when(context.formParam("callbackUrl")).thenReturn(null);
        when(context.formParam("scopes")).thenReturn(null);
        when(context.formParam("state")).thenReturn(null);

        settings.populate(context);

        assertThat(settings.clientId).isNull();
        assertThat(settings.clientSecret).isNull();
        assertThat(settings.wellKnown).isNull();
        assertThat(settings.callbackUrl).isNull();
        assertThat(settings.scopes).isNull();
        assertThat(settings.state).isNull();
    }

    @Test
    @DisplayName("Populate should handle empty string form parameters")
    void populate_shouldHandleEmptyStringFormParameters() {
        when(context.formParam("clientId")).thenReturn("");
        when(context.formParam("clientSecret")).thenReturn("");
        when(context.formParam("wellKnown")).thenReturn("");
        when(context.formParam("callbackUrl")).thenReturn("");
        when(context.formParam("scopes")).thenReturn("");
        when(context.formParam("state")).thenReturn("");

        settings.populate(context);

        assertThat(settings.clientId).isEmpty();
        assertThat(settings.clientSecret).isEmpty();
        assertThat(settings.wellKnown).isEmpty();
        assertThat(settings.callbackUrl).isEmpty();
        assertThat(settings.scopes).isEmpty();
        assertThat(settings.state).isEmpty();
    }

    @Test
    @DisplayName("Basic auth header should handle empty client ID")
    void getBasicAuthHeader_shouldHandleEmptyClientId() {
        settings.clientId = "";
        settings.clientSecret = "secret";

        String authHeader = settings.getBasicAuthHeader();

        assertThat(authHeader).isNotNull();
        String decoded = new String(java.util.Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo(":secret");
    }

    @Test
    @DisplayName("Basic auth header should handle empty client secret")
    void getBasicAuthHeader_shouldHandleEmptyClientSecret() {
        settings.clientId = "client";
        settings.clientSecret = "";

        String authHeader = settings.getBasicAuthHeader();

        assertThat(authHeader).isNotNull();
        String decoded = new String(java.util.Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("client:");
    }

    @Test
    @DisplayName("Update token info should handle missing access_token field")
    void updateTokenInfo_shouldHandleMissingAccessToken() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("refresh_token", "refresh-only");
        tokenResponse.put("expires_in", 3600);

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.accessToken).isEmpty();
        assertThat(settings.refreshToken).isEqualTo("refresh-only");
    }

    @Test
    @DisplayName("Update token info should handle missing expires_in field")
    void updateTokenInfo_shouldHandleMissingExpiresIn() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token");
        tokenResponse.put("refresh_token", "refresh-token");

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.accessToken).isEqualTo("access-token");
        assertThat(settings.exp).isEqualTo(0L);
    }

    @Test
    @DisplayName("Update token info should handle empty JSON object")
    void updateTokenInfo_shouldHandleEmptyJsonObject() {
        JSONObject tokenResponse = new JSONObject();

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.accessToken).isEmpty();
        assertThat(settings.refreshToken).isEmpty();
        assertThat(settings.idToken).isEmpty();
        assertThat(settings.exp).isEqualTo(0L);
    }

    @Test
    @DisplayName("Get access token details should throw exception for malformed JWT")
    void getAccessTokenDetails_shouldThrowExceptionForMalformedJwt() {
        settings.accessToken = "not-a-valid-jwt";

        assertThatThrownBy(() -> settings.getAccessTokenDetails())
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("Get access token details should throw exception for JWT with invalid Base64 payload")
    void getAccessTokenDetails_shouldThrowExceptionForInvalidBase64Payload() {
        settings.accessToken = "header.!!!invalid-base64!!!.signature";

        assertThatThrownBy(() -> settings.getAccessTokenDetails())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Get access token details should throw exception for JWT with non-JSON payload")
    void getAccessTokenDetails_shouldThrowExceptionForNonJsonPayload() {
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("header".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("not json content".getBytes());
        settings.accessToken = header + "." + payload + ".signature";

        assertThatThrownBy(() -> settings.getAccessTokenDetails())
                .isInstanceOf(kong.unirest.core.json.JSONException.class);
    }

    @Test
    @DisplayName("Populate should handle malformed URL in wellKnown")
    void populate_shouldHandleMalformedWellKnownUrl() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("not-a-valid-url");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.wellKnown).isEqualTo("not-a-valid-url");
    }

    @Test
    @DisplayName("Populate should handle malformed URL in callbackUrl")
    void populate_shouldHandleMalformedCallbackUrl() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("not-a-valid-callback-url");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.callbackUrl).isEqualTo("not-a-valid-callback-url");
    }

    @Test
    @DisplayName("Populate should handle unsupported scope strings")
    void populate_shouldHandleUnsupportedScopeStrings() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("invalid_scope_that_does_not_exist");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.scopes).isEqualTo("invalid_scope_that_does_not_exist");
    }

    @Test
    @DisplayName("Update token info should handle negative expires_in value")
    void updateTokenInfo_shouldHandleNegativeExpiresIn() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token");
        tokenResponse.put("expires_in", -1);

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.exp).isEqualTo(-1L);
    }

    @Test
    @DisplayName("Update token info should handle zero expires_in value")
    void updateTokenInfo_shouldHandleZeroExpiresIn() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token");
        tokenResponse.put("expires_in", 0);

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.exp).isEqualTo(0L);
    }

    @Test
    @DisplayName("Update token info should handle very large expires_in value")
    void updateTokenInfo_shouldHandleVeryLargeExpiresIn() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token");
        tokenResponse.put("expires_in", Long.MAX_VALUE);

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.exp).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("Populate should handle whitespace-only form parameters")
    void populate_shouldHandleWhitespaceOnlyFormParameters() {
        when(context.formParam("clientId")).thenReturn("   ");
        when(context.formParam("clientSecret")).thenReturn("\t\n");
        when(context.formParam("wellKnown")).thenReturn("  ");
        when(context.formParam("callbackUrl")).thenReturn(" ");
        when(context.formParam("scopes")).thenReturn("   ");
        when(context.formParam("state")).thenReturn("\n");

        settings.populate(context);

        assertThat(settings.clientId).isEqualTo("   ");
        assertThat(settings.clientSecret).isEqualTo("\t\n");
    }

    @Test
    @DisplayName("Basic auth header should handle null client ID")
    void getBasicAuthHeader_shouldHandleNullClientId() {
        settings.clientId = null;
        settings.clientSecret = "secret";

        String authHeader = settings.getBasicAuthHeader();

        assertThat(authHeader).isNotNull();
        String decoded = new String(java.util.Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("null:secret");
    }

    @Test
    @DisplayName("Basic auth header should handle null client secret")
    void getBasicAuthHeader_shouldHandleNullClientSecret() {
        settings.clientId = "client";
        settings.clientSecret = null;

        String authHeader = settings.getBasicAuthHeader();

        assertThat(authHeader).isNotNull();
        String decoded = new String(java.util.Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("client:null");
    }
}
