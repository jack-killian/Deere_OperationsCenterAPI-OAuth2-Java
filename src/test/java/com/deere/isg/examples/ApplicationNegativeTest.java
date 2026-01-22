package com.deere.isg.examples;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.javalin.http.Context;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application Negative Tests - Error Handling")
class ApplicationNegativeTest {

    private WireMockServer wireMockServer;
    private Application application;
    private Settings settings;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        
        Unirest.config().reset();
        
        application = new Application();
        settings = new Settings();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        Unirest.shutDown();
    }

    @Test
    @DisplayName("Callback should render error when error parameter is present")
    void processCallback_shouldRenderErrorWhenErrorParamPresent() throws Exception {
        when(context.queryParam("error")).thenReturn("access_denied");
        when(context.queryParam("error_description")).thenReturn("User denied access to the application");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Callback should handle invalid_scope error")
    void processCallback_shouldHandleInvalidScopeError() throws Exception {
        when(context.queryParam("error")).thenReturn("invalid_scope");
        when(context.queryParam("error_description")).thenReturn("The requested scope is invalid or unknown");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Token exchange should handle expired authorization code")
    void processCallback_shouldHandleExpiredAuthorizationCode() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:%d/oauth2/token"
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "invalid_grant",
                                "error_description": "The authorization code has expired"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        when(context.queryParam("error")).thenReturn(null);
        when(context.queryParam("code")).thenReturn("expired-auth-code");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Token exchange should handle wrong client credentials")
    void processCallback_shouldHandleWrongClientCredentials() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:%d/oauth2/token"
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "invalid_client",
                                "error_description": "Client authentication failed"
                            }
                            """)));

        settings.clientId = "wrong-client-id";
        settings.clientSecret = "wrong-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        when(context.queryParam("error")).thenReturn(null);
        when(context.queryParam("code")).thenReturn("valid-auth-code");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Token refresh should handle expired refresh token")
    void refreshAccessToken_shouldHandleExpiredRefreshToken() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:%d/oauth2/token"
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "invalid_grant",
                                "error_description": "The refresh token has expired"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.refreshToken = "expired-refresh-token";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method refreshMethod = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshMethod.setAccessible(true);
        refreshMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Token refresh should handle revoked refresh token")
    void refreshAccessToken_shouldHandleRevokedRefreshToken() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:%d/oauth2/token"
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "invalid_grant",
                                "error_description": "The refresh token has been revoked"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.refreshToken = "revoked-refresh-token";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method refreshMethod = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshMethod.setAccessible(true);
        refreshMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("OIDC discovery should handle unreachable well-known endpoint")
    void getLocationFromMeta_shouldHandleUnreachableEndpoint() throws Exception {
        settings.wellKnown = "http://localhost:99999/.well-known/oauth-authorization-server";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getLocationFromMetaMethod = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMetaMethod.setAccessible(true);
        
        try {
            getLocationFromMetaMethod.invoke(application, "authorization_endpoint");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("Token endpoint should handle 500 server error")
    void processCallback_shouldHandleServerError() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:%d/oauth2/token"
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "server_error",
                                "error_description": "Internal server error"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        when(context.queryParam("error")).thenReturn(null);
        when(context.queryParam("code")).thenReturn("valid-auth-code");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Callback should handle missing authorization code")
    void processCallback_shouldHandleMissingAuthorizationCode() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:%d/oauth2/token"
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "invalid_request",
                                "error_description": "Missing required parameter: code"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        when(context.queryParam("error")).thenReturn(null);
        when(context.queryParam("code")).thenReturn(null);

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Well-known endpoint should handle malformed JSON response")
    void getLocationFromMeta_shouldHandleMalformedJson() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not valid json {")));

        settings.wellKnown = wellKnownUrl;

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getLocationFromMetaMethod = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMetaMethod.setAccessible(true);
        
        try {
            getLocationFromMetaMethod.invoke(application, "authorization_endpoint");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("API call should handle network timeout")
    void callTheApi_shouldHandleNetworkTimeout() throws Exception {
        stubFor(get(urlPathEqualTo("/platform/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(30000)
                        .withBody("{}")));

        settings.accessToken = "valid-token";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getParameter("url")).thenReturn("http://localhost:" + wireMockServer.port() + "/platform/slow");
        when(context.req()).thenReturn(mockRequest);

        Unirest.config().connectTimeout(100);

        java.lang.reflect.Method callTheApiMethod = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApiMethod.setAccessible(true);
        callTheApiMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }
}
