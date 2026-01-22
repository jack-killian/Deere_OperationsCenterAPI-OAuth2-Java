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
@DisplayName("Application OAuth2 Flow Tests")
class ApplicationTest {

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
    @DisplayName("Authorization URL should contain required OAuth2 parameters")
    void getRedirectUrl_shouldContainRequiredParameters() throws Exception {
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

        settings.clientId = "test-client-id";
        settings.scopes = "openid profile";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.wellKnown = wellKnownUrl;
        settings.state = "test-state-123";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getRedirectUrlMethod = Application.class.getDeclaredMethod("getRedirectUrl");
        getRedirectUrlMethod.setAccessible(true);
        String redirectUrl = (String) getRedirectUrlMethod.invoke(application);

        assertThat(redirectUrl).contains("client_id=test-client-id");
        assertThat(redirectUrl).contains("response_type=code");
        assertThat(redirectUrl).contains("scope=openid+profile");
        assertThat(redirectUrl).contains("redirect_uri=http%3A%2F%2Flocalhost%3A9090%2Fcallback");
        assertThat(redirectUrl).contains("state=test-state-123");
    }

    @Test
    @DisplayName("OIDC discovery should correctly parse well-known endpoint response")
    void getLocationFromMeta_shouldParseWellKnownResponse() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://auth.example.com/authorize",
                                "token_endpoint": "http://auth.example.com/token",
                                "issuer": "http://auth.example.com"
                            }
                            """)));

        settings.wellKnown = wellKnownUrl;

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getLocationFromMetaMethod = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMetaMethod.setAccessible(true);
        
        String authEndpoint = (String) getLocationFromMetaMethod.invoke(application, "authorization_endpoint");
        String tokenEndpoint = (String) getLocationFromMetaMethod.invoke(application, "token_endpoint");

        assertThat(authEndpoint).isEqualTo("http://auth.example.com/authorize");
        assertThat(tokenEndpoint).isEqualTo("http://auth.example.com/token");
    }

    @Test
    @DisplayName("Token exchange should successfully process authorization code")
    void processCallback_shouldExchangeCodeForTokens() throws Exception {
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
                                "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.POstGetfAytaZS82wHcjoTyoqhMyxXiWdR7Nn7A29DNSl0EiXLdwJ6xC6AfgZWF1bOsS_TuYI3OG85AmiExREkrS6tDfTQ2B3WXlrr-wp5AokiRbz3_oB4OxG-W9KcEEbDRcZc0nH3L7LzYptiy1PtAylQGxHTWZXtGz4ht0bAecBgmpdgXMguEIcoqPJ1n3pIWk_dUZegpqx0Lka21H6XxUTxiy8OcaarA8zdnPUnV6AmNP3ecFawIFYdvJB_cm-GvpCSbr8G8y_Mllj8f4x9nBH8pQux89_6gUY618iYv7tuPWBFfEbLxtF2pZS6YC1aSfLQxeNe8djT9YjpvRZA",
                                "refresh_token": "refresh-token-123",
                                "id_token": "id-token-456",
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
                                "values": []
                            }
                            """)));

        settings.clientId = "test-client-id";
        settings.clientSecret = "test-client-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.apiUrl = "http://localhost:" + wireMockServer.port() + "/platform";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        when(context.queryParam("error")).thenReturn(null);
        when(context.queryParam("code")).thenReturn("auth-code-123");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        assertThat(settings.accessToken).isNotNull();
        assertThat(settings.refreshToken).isEqualTo("refresh-token-123");
        assertThat(settings.exp).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Token refresh should successfully exchange refresh token for new access token")
    void refreshAccessToken_shouldExchangeRefreshToken() throws Exception {
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
                                "access_token": "new-access-token-xyz",
                                "refresh_token": "new-refresh-token-abc",
                                "expires_in": 7200,
                                "token_type": "Bearer"
                            }
                            """)));

        settings.clientId = "test-client-id";
        settings.clientSecret = "test-client-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.refreshToken = "old-refresh-token";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method refreshAccessTokenMethod = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshAccessTokenMethod.setAccessible(true);
        refreshAccessTokenMethod.invoke(application, context);

        assertThat(settings.accessToken).isEqualTo("new-access-token-xyz");
        assertThat(settings.refreshToken).isEqualTo("new-refresh-token-abc");
        assertThat(settings.exp).isEqualTo(7200L);
    }

    @Test
    @DisplayName("Index page should render with settings")
    void index_shouldRenderWithSettings() {
        application.index(context);
        
        verify(context).render(eq("main.mustache"), anyMap());
    }

    @Test
    @DisplayName("OIDC metadata should be cached after first fetch")
    void getLocationFromMeta_shouldCacheMetadata() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://auth.example.com/authorize",
                                "token_endpoint": "http://auth.example.com/token"
                            }
                            """)));

        settings.wellKnown = wellKnownUrl;

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method getLocationFromMetaMethod = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMetaMethod.setAccessible(true);
        
        getLocationFromMetaMethod.invoke(application, "authorization_endpoint");
        getLocationFromMetaMethod.invoke(application, "token_endpoint");
        getLocationFromMetaMethod.invoke(application, "authorization_endpoint");

        verify(exactly(1), getRequestedFor(urlEqualTo("/.well-known/oauth-authorization-server")));
    }
}
