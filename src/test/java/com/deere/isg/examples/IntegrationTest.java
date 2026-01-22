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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Integration Tests - End-to-End OAuth2 Flow")
class IntegrationTest {

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
    @DisplayName("Complete OAuth2 flow should work end-to-end with mocked John Deere endpoints")
    void completeOAuth2Flow_shouldWorkEndToEnd() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:%d/oauth2/token",
                                "issuer": "http://localhost:%d"
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port(), wireMockServer.port()))));

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwibmFtZSI6IlRlc3QgVXNlciIsImlhdCI6MTUxNjIzOTAyMn0.signature",
                                "refresh_token": "refresh-token-integration-test",
                                "id_token": "id-token-integration-test",
                                "expires_in": 3600,
                                "token_type": "Bearer"
                            }
                            """)));

        stubFor(get(urlPathEqualTo("/platform/organizations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "values": [
                                    {
                                        "id": "org-123",
                                        "name": "Test Farm",
                                        "links": [
                                            {"rel": "self", "uri": "http://localhost:%d/platform/organizations/org-123"}
                                        ]
                                    }
                                ],
                                "total": 1
                            }
                            """.formatted(wireMockServer.port()))));

        settings.clientId = "integration-test-client";
        settings.clientSecret = "integration-test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile offline_access";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getRedirectUrlMethod = Application.class.getDeclaredMethod("getRedirectUrl");
        getRedirectUrlMethod.setAccessible(true);
        String authUrl = (String) getRedirectUrlMethod.invoke(application);

        assertThat(authUrl).contains("client_id=integration-test-client");
        assertThat(authUrl).contains("response_type=code");

        when(context.queryParam("error")).thenReturn(null);
        when(context.queryParam("code")).thenReturn("integration-auth-code");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        assertThat(settings.accessToken).isNotNull();
        assertThat(settings.refreshToken).isEqualTo("refresh-token-integration-test");

        verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("code=integration-auth-code")));
    }

    @Test
    @DisplayName("Organization access verification should detect connections rel")
    void organizationAccessVerification_shouldDetectConnectionsRel() throws Exception {
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

        stubFor(get(urlPathEqualTo("/platform/organizations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "values": [
                                    {
                                        "id": "org-needs-setup",
                                        "name": "Farm Needing Setup",
                                        "links": [
                                            {"rel": "self", "uri": "http://localhost:%d/platform/organizations/org-needs-setup"},
                                            {"rel": "connections", "uri": "http://localhost:%d/connections/setup"}
                                        ]
                                    }
                                ],
                                "total": 1
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        settings.accessToken = "valid-access-token";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method needsOrgAccessMethod = Application.class.getDeclaredMethod("needsOrganizationAccess");
        needsOrgAccessMethod.setAccessible(true);
        String redirectUrl = (String) needsOrgAccessMethod.invoke(application);

        assertThat(redirectUrl).isNotNull();
        assertThat(redirectUrl).contains("connections/setup");
        assertThat(redirectUrl).contains("redirect_uri");
    }

    @Test
    @DisplayName("Organization access verification should return null when no connections rel")
    void organizationAccessVerification_shouldReturnNullWhenNoConnectionsRel() throws Exception {
        stubFor(get(urlPathEqualTo("/platform/organizations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "values": [
                                    {
                                        "id": "org-complete",
                                        "name": "Complete Farm",
                                        "links": [
                                            {"rel": "self", "uri": "http://localhost:%d/platform/organizations/org-complete"},
                                            {"rel": "fields", "uri": "http://localhost:%d/platform/organizations/org-complete/fields"}
                                        ]
                                    }
                                ],
                                "total": 1
                            }
                            """.formatted(wireMockServer.port(), wireMockServer.port()))));

        settings.accessToken = "valid-access-token";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method needsOrgAccessMethod = Application.class.getDeclaredMethod("needsOrganizationAccess");
        needsOrgAccessMethod.setAccessible(true);
        String redirectUrl = (String) needsOrgAccessMethod.invoke(application);

        assertThat(redirectUrl).isNull();
    }

    @Test
    @DisplayName("API call flow should work with valid access token")
    void apiCallFlow_shouldWorkWithValidToken() throws Exception {
        stubFor(get(urlPathEqualTo("/platform/fields"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "values": [
                                    {"id": "field-1", "name": "North Field", "area": 100.5},
                                    {"id": "field-2", "name": "South Field", "area": 75.3}
                                ],
                                "total": 2
                            }
                            """)));

        settings.accessToken = "valid-access-token-for-api";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getParameter("url")).thenReturn("http://localhost:" + wireMockServer.port() + "/platform/fields");
        when(context.req()).thenReturn(mockRequest);

        java.lang.reflect.Method callTheApiMethod = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApiMethod.setAccessible(true);
        callTheApiMethod.invoke(application, context);

        assertThat(settings.apiResponse).contains("North Field");
        assertThat(settings.apiResponse).contains("South Field");

        verify(getRequestedFor(urlPathEqualTo("/platform/fields"))
                .withHeader("authorization", equalTo("Bearer valid-access-token-for-api")));
    }

    @Test
    @DisplayName("Token refresh and API call should work in sequence")
    void tokenRefreshAndApiCall_shouldWorkInSequence() throws Exception {
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
                                "access_token": "new-refreshed-access-token",
                                "refresh_token": "new-refresh-token",
                                "expires_in": 7200,
                                "token_type": "Bearer"
                            }
                            """)));

        stubFor(get(urlPathEqualTo("/platform/machines"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "values": [{"id": "machine-1", "name": "Tractor 1"}],
                                "total": 1
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

        assertThat(settings.accessToken).isEqualTo("new-refreshed-access-token");

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getParameter("url")).thenReturn("http://localhost:" + wireMockServer.port() + "/platform/machines");
        when(context.req()).thenReturn(mockRequest);

        java.lang.reflect.Method callApiMethod = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callApiMethod.setAccessible(true);
        callApiMethod.invoke(application, context);

        assertThat(settings.apiResponse).contains("Tractor 1");
    }
}
