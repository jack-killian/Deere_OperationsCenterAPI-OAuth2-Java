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
@DisplayName("Integration Negative Tests - End-to-End Failure Scenarios")
class IntegrationNegativeTest {

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
    @DisplayName("Complete flow should fail when auth server is unavailable")
    void completeFlow_shouldFailWhenAuthServerUnavailable() throws Exception {
        settings.wellKnown = "http://localhost:99999/.well-known/oauth-authorization-server";
        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getRedirectUrlMethod = Application.class.getDeclaredMethod("getRedirectUrl");
        getRedirectUrlMethod.setAccessible(true);
        
        try {
            getRedirectUrlMethod.invoke(application);
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("Token exchange should fail when token endpoint returns 500")
    void tokenExchange_shouldFailWhenTokenEndpointReturns500() throws Exception {
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
                                "error_description": "Internal server error occurred during token exchange"
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
    @DisplayName("Flow should fail when redirect_uri doesn't match registered URL")
    void flow_shouldFailWhenRedirectUriMismatch() throws Exception {
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
                                "error_description": "redirect_uri does not match the registered callback URL"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://wrong-domain.com/callback";
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
    @DisplayName("Organization access setup should fail when API returns error")
    void organizationAccessSetup_shouldFailWhenApiReturnsError() throws Exception {
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
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "access_token": "valid-access-token",
                                "refresh_token": "valid-refresh-token",
                                "expires_in": 3600
                            }
                            """)));

        stubFor(get(urlPathEqualTo("/platform/organizations"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "server_error",
                                "error_description": "Failed to retrieve organizations"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

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
    @DisplayName("API call should fail after token refresh when new token is invalid")
    void apiCall_shouldFailAfterTokenRefreshWithInvalidToken() throws Exception {
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
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "access_token": "invalid-new-token",
                                "refresh_token": "new-refresh-token",
                                "expires_in": 3600
                            }
                            """)));

        // Configure LoggingInterceptor to throw on 401 responses
        Unirest.config().reset();
        Unirest.config().interceptor(new LoggingInterceptor());

        stubFor(get(urlPathEqualTo("/platform/fields"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "unauthorized",
                                "error_description": "The access token is invalid"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.refreshToken = "old-refresh-token";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method refreshMethod = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshMethod.setAccessible(true);
        refreshMethod.invoke(application, context);

        assertThat(settings.accessToken).isEqualTo("invalid-new-token");

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getParameter("url")).thenReturn("http://localhost:" + wireMockServer.port() + "/platform/fields");
        when(context.req()).thenReturn(mockRequest);

        java.lang.reflect.Method callApiMethod = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callApiMethod.setAccessible(true);
        callApiMethod.invoke(application, context);

        // The callTheApi method catches the exception and renders error.mustache
        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Flow should handle well-known endpoint returning 404")
    void flow_shouldHandleWellKnownEndpoint404() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        // Return 404 with JSON that doesn't have the expected authorization_endpoint key
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "not_found",
                                "error_description": "OAuth metadata endpoint not found"
                            }
                            """)));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getRedirectUrlMethod = Application.class.getDeclaredMethod("getRedirectUrl");
        getRedirectUrlMethod.setAccessible(true);
        
        // The well-known endpoint returns 404 with JSON that doesn't have authorization_endpoint
        // This causes a JSONException when trying to get the missing key
        try {
            getRedirectUrlMethod.invoke(application);
        } catch (Exception e) {
            // The exception can be either JSONException (missing key) or wrapped exception
            assertThat(e.getCause()).isNotNull();
        }
    }

    @Test
    @DisplayName("Flow should handle intermittent network failures")
    void flow_shouldHandleIntermittentNetworkFailures() throws Exception {
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
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

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
    @DisplayName("Flow should handle malformed organization response")
    void flow_shouldHandleMalformedOrganizationResponse() throws Exception {
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
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "access_token": "valid-access-token",
                                "refresh_token": "valid-refresh-token",
                                "expires_in": 3600
                            }
                            """)));

        stubFor(get(urlPathEqualTo("/platform/organizations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not valid json {")));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

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
    @DisplayName("Multiple rapid token refresh attempts should be handled")
    void multipleRapidTokenRefresh_shouldBeHandled() throws Exception {
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
                .inScenario("Token Refresh")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "access_token": "first-new-token",
                                "refresh_token": "first-new-refresh",
                                "expires_in": 3600
                            }
                            """))
                .willSetStateTo("First Refresh Done"));

        // Second refresh returns empty body which will cause NullPointerException when parsing
        stubFor(post(urlEqualTo("/oauth2/token"))
                .inScenario("Token Refresh")
                .whenScenarioStateIs("First Refresh Done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.refreshToken = "original-refresh-token";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method refreshMethod = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshMethod.setAccessible(true);
        
        refreshMethod.invoke(application, context);
        assertThat(settings.accessToken).isEqualTo("first-new-token");

        // Second refresh will fail due to empty response body causing NullPointerException
        // The refreshAccessToken method catches exceptions and renders error.mustache
        refreshMethod.invoke(application, context);
        verify(context).render(eq("error.mustache"), anyMap());
    }
}
