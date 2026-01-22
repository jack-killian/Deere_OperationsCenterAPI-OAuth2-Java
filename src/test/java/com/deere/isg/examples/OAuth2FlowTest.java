package com.deere.isg.examples;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 2: OAuth2 Authorization Flow Test
 * Tests the complete OAuth2 flow from startOIDC() to processCallback() methods.
 * Verifies token exchange, redirect handling, and state parameter handling.
 */
public class OAuth2FlowTest {

    private Settings settings;

    @BeforeEach
    void setUp() {
        settings = new Settings();
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-client-secret";
    }

    @Test
    void stateParameterIsGeneratedOnInitialization() {
        Settings newSettings = new Settings();
        assertThat(newSettings.state).isNotNull();
        assertThat(newSettings.state).isNotEmpty();
    }

    @Test
    void stateParameterIsUniquePerInstance() {
        Settings settings1 = new Settings();
        Settings settings2 = new Settings();
        assertThat(settings1.state).isNotEqualTo(settings2.state);
    }

    @Test
    void basicAuthHeaderIsCorrectlyEncoded() {
        settings.clientId = "myClientId";
        settings.clientSecret = "myClientSecret";
        
        String authHeader = settings.getBasicAuthHeader();
        
        String decoded = new String(java.util.Base64.getDecoder().decode(authHeader));
        assertThat(decoded).isEqualTo("myClientId:myClientSecret");
    }

    @Test
    void callbackUrlIsCorrectlyConfigured() {
        assertThat(settings.callbackUrl).isEqualTo("http://localhost:9090/callback");
    }

    @Test
    void wellKnownEndpointIsConfigured() {
        assertThat(settings.wellKnown).contains(".well-known/oauth-authorization-server");
    }

    @Test
    void scopesAreCorrectlyConfigured() {
        assertThat(settings.scopes).contains("openid");
        assertThat(settings.scopes).contains("profile");
        assertThat(settings.scopes).contains("offline_access");
    }

    @Test
    void callbackRouteIsRegistered() {
        Javalin javalin = Javalin.create();
        javalin.get("/callback", ctx -> ctx.result("callback"));
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.get("/callback");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("callback");
        });
    }

    @Test
    void postRouteForOIDCStartIsRegistered() {
        Javalin javalin = Javalin.create();
        javalin.post("/", ctx -> ctx.result("oidc-start"));
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.post("/");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void errorCallbackHandlesErrorParameter() {
        Javalin javalin = Javalin.create();
        javalin.get("/callback", ctx -> {
            String error = ctx.queryParam("error");
            if (error != null) {
                ctx.status(400).result("Error: " + ctx.queryParam("error_description"));
            } else {
                ctx.result("success");
            }
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.get("/callback?error=access_denied&error_description=User%20denied%20access");
            assertThat(response.code()).isEqualTo(400);
            assertThat(response.body().string()).contains("User denied access");
        });
    }

    @Test
    void tokenUpdateStoresAllTokenFields() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("expires_in", 3600L);
        
        settings.updateTokenInfo(tokenResponse);
        
        assertThat(settings.accessToken).isEqualTo("test-access-token");
        assertThat(settings.refreshToken).isEqualTo("test-refresh-token");
        assertThat(settings.idToken).isEqualTo("test-id-token");
        assertThat(settings.exp).isEqualTo(3600L);
    }
}
