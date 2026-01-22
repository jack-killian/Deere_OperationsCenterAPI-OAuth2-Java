package com.deere.isg.examples;

import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Form Validation Tests - Input Sanitization and Security")
class FormValidationTest {

    private Settings settings;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    @DisplayName("Populate should handle XSS script tags in clientId")
    void populate_shouldHandleXssScriptTagsInClientId() {
        when(context.formParam("clientId")).thenReturn("<script>alert('xss')</script>");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.clientId).isEqualTo("<script>alert('xss')</script>");
    }

    @Test
    @DisplayName("Populate should handle XSS in callback URL")
    void populate_shouldHandleXssInCallbackUrl() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("javascript:alert('xss')");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.callbackUrl).isEqualTo("javascript:alert('xss')");
    }

    @Test
    @DisplayName("Populate should handle SQL injection attempts in clientId")
    void populate_shouldHandleSqlInjectionInClientId() {
        when(context.formParam("clientId")).thenReturn("'; DROP TABLE users; --");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.clientId).isEqualTo("'; DROP TABLE users; --");
    }

    @Test
    @DisplayName("Populate should handle SQL injection attempts in scopes")
    void populate_shouldHandleSqlInjectionInScopes() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid OR 1=1; --");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.scopes).isEqualTo("openid OR 1=1; --");
    }

    @Test
    @DisplayName("Populate should handle extremely long clientId")
    void populate_shouldHandleExtremelyLongClientId() {
        String longClientId = "a".repeat(10000);
        when(context.formParam("clientId")).thenReturn(longClientId);
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.clientId).hasSize(10000);
    }

    @Test
    @DisplayName("Populate should handle extremely long clientSecret")
    void populate_shouldHandleExtremelyLongClientSecret() {
        String longSecret = "s".repeat(10000);
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn(longSecret);
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.clientSecret).hasSize(10000);
    }

    @Test
    @DisplayName("Populate should handle extremely long wellKnown URL")
    void populate_shouldHandleExtremelyLongWellKnownUrl() {
        String longUrl = "https://auth.example.com/" + "a".repeat(10000) + "/.well-known";
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn(longUrl);
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.wellKnown).isEqualTo(longUrl);
    }

    @Test
    @DisplayName("Populate should handle extremely long scopes")
    void populate_shouldHandleExtremelyLongScopes() {
        String longScopes = "openid " + "custom_scope_".repeat(1000);
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn(longScopes);
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.scopes).isEqualTo(longScopes);
    }

    @Test
    @DisplayName("Populate should handle HTML entities in form fields")
    void populate_shouldHandleHtmlEntitiesInFormFields() {
        when(context.formParam("clientId")).thenReturn("&lt;client&gt;");
        when(context.formParam("clientSecret")).thenReturn("&amp;secret&amp;");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.clientId).isEqualTo("&lt;client&gt;");
        assertThat(settings.clientSecret).isEqualTo("&amp;secret&amp;");
    }

    @Test
    @DisplayName("Populate should handle unicode characters in form fields")
    void populate_shouldHandleUnicodeCharactersInFormFields() {
        when(context.formParam("clientId")).thenReturn("client-日本語-中文");
        when(context.formParam("clientSecret")).thenReturn("secret-émojis-🔐");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.clientId).isEqualTo("client-日本語-中文");
        assertThat(settings.clientSecret).isEqualTo("secret-émojis-🔐");
    }

    @Test
    @DisplayName("Populate should handle null byte injection attempts")
    void populate_shouldHandleNullByteInjection() {
        when(context.formParam("clientId")).thenReturn("client\u0000injected");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.clientId).isEqualTo("client\u0000injected");
    }

    @Test
    @DisplayName("Populate should handle CRLF injection attempts in callback URL")
    void populate_shouldHandleCrlfInjectionInCallbackUrl() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback\r\nSet-Cookie: malicious=true");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.callbackUrl).contains("\r\n");
    }

    @Test
    @DisplayName("Populate should handle path traversal attempts in wellKnown URL")
    void populate_shouldHandlePathTraversalInWellKnownUrl() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/../../../etc/passwd");
        when(context.formParam("callbackUrl")).thenReturn("http://localhost/callback");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.wellKnown).isEqualTo("https://auth.example.com/../../../etc/passwd");
    }

    @Test
    @DisplayName("Populate should handle data URI in callback URL")
    void populate_shouldHandleDataUriInCallbackUrl() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("data:text/html,<script>alert('xss')</script>");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.callbackUrl).startsWith("data:");
    }

    @Test
    @DisplayName("Populate should handle file URI in callback URL")
    void populate_shouldHandleFileUriInCallbackUrl() {
        when(context.formParam("clientId")).thenReturn("client");
        when(context.formParam("clientSecret")).thenReturn("secret");
        when(context.formParam("wellKnown")).thenReturn("https://auth.example.com/.well-known");
        when(context.formParam("callbackUrl")).thenReturn("file:///etc/passwd");
        when(context.formParam("scopes")).thenReturn("openid");
        when(context.formParam("state")).thenReturn("state");

        settings.populate(context);

        assertThat(settings.callbackUrl).isEqualTo("file:///etc/passwd");
    }

    @Test
    @DisplayName("Basic auth header should handle special characters requiring URL encoding")
    void getBasicAuthHeader_shouldHandleSpecialCharactersRequiringUrlEncoding() {
        settings.clientId = "client+id=test&more";
        settings.clientSecret = "secret/with?special=chars";

        String authHeader = settings.getBasicAuthHeader();

        assertThat(authHeader).isNotNull();
        String decoded = new String(java.util.Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("client+id=test&more:secret/with?special=chars");
    }
}
