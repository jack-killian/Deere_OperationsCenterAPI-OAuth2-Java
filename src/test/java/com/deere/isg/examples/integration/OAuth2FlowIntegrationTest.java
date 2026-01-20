package com.deere.isg.examples.integration;

import com.deere.isg.examples.LoggingInterceptor;
import com.deere.isg.examples.Settings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2FlowIntegrationTest {

    private static WireMockServer wireMockServer;
    private Settings settings;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        settings = new Settings();
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-client-secret";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile offline_access";
        settings.wellKnown = wireMockServer.baseUrl() + "/.well-known/oauth-authorization-server";
        wireMockServer.resetAll();
        Unirest.config().reset();
        Unirest.config().interceptor(new LoggingInterceptor());
    }

    @AfterEach
    void tearDown() {
        Unirest.config().reset();
    }

    @Test
    void shouldFetchOAuthMetadataFromWellKnownEndpoint() {
        String wellKnownResponse = """
            {
                "issuer": "https://signin.johndeere.com",
                "authorization_endpoint": "%s/authorize",
                "token_endpoint": "%s/token",
                "userinfo_endpoint": "%s/userinfo",
                "jwks_uri": "%s/.well-known/jwks.json",
                "scopes_supported": ["openid", "profile", "offline_access", "ag1", "eq1"],
                "response_types_supported": ["code"],
                "grant_types_supported": ["authorization_code", "refresh_token"]
            }
            """.formatted(wireMockServer.baseUrl(), wireMockServer.baseUrl(), 
                         wireMockServer.baseUrl(), wireMockServer.baseUrl());

        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(wellKnownResponse)));

        JSONObject metadata = Unirest.get(settings.wellKnown)
                .asJson()
                .getBody()
                .getObject();

        assertThat(metadata.getString("authorization_endpoint")).isEqualTo(wireMockServer.baseUrl() + "/authorize");
        assertThat(metadata.getString("token_endpoint")).isEqualTo(wireMockServer.baseUrl() + "/token");
        assertThat(metadata.getJSONArray("scopes_supported")).contains("openid", "profile", "offline_access");
    }

    @Test
    void shouldExchangeAuthorizationCodeForTokens() {
        String authCode = "test-authorization-code";
        String tokenResponse = """
            {
                "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNzA0MDY3MjAwfQ.signature",
                "token_type": "Bearer",
                "expires_in": 3600,
                "refresh_token": "test-refresh-token",
                "id_token": "test-id-token",
                "scope": "openid profile offline_access"
            }
            """;

        String basicAuth = Base64.getEncoder().encodeToString(
                (settings.clientId + ":" + settings.clientSecret).getBytes());

        stubFor(post(urlEqualTo("/token"))
                .withHeader("authorization", equalTo("Basic " + basicAuth))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("code=" + authCode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(tokenResponse)));

        JSONObject response = Unirest.post(wireMockServer.baseUrl() + "/token")
                .header("authorization", "Basic " + settings.getBasicAuthHeader())
                .accept("application/json")
                .field("grant_type", "authorization_code")
                .field("redirect_uri", settings.callbackUrl)
                .field("code", authCode)
                .field("scope", settings.scopes)
                .contentType("application/x-www-form-urlencoded")
                .asJson()
                .getBody()
                .getObject();

        settings.updateTokenInfo(response);

        assertThat(settings.accessToken).startsWith("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");
        assertThat(settings.refreshToken).isEqualTo("test-refresh-token");
        assertThat(settings.idToken).isEqualTo("test-id-token");
        assertThat(settings.exp).isEqualTo(3600L);
    }

    @Test
    void shouldRefreshAccessTokenUsingRefreshToken() {
        settings.refreshToken = "existing-refresh-token";
        
        String tokenResponse = """
            {
                "access_token": "new-access-token",
                "token_type": "Bearer",
                "expires_in": 3600,
                "refresh_token": "new-refresh-token",
                "scope": "openid profile offline_access"
            }
            """;

        String basicAuth = Base64.getEncoder().encodeToString(
                (settings.clientId + ":" + settings.clientSecret).getBytes());

        stubFor(post(urlEqualTo("/token"))
                .withHeader("authorization", equalTo("Basic " + basicAuth))
                .withRequestBody(containing("grant_type=refresh_token"))
                .withRequestBody(containing("refresh_token=" + settings.refreshToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(tokenResponse)));

        JSONObject response = Unirest.post(wireMockServer.baseUrl() + "/token")
                .header("authorization", "Basic " + settings.getBasicAuthHeader())
                .accept("application/json")
                .field("grant_type", "refresh_token")
                .field("redirect_uri", settings.callbackUrl)
                .field("refresh_token", settings.refreshToken)
                .field("scope", settings.scopes)
                .contentType("application/x-www-form-urlencoded")
                .asJson()
                .getBody()
                .getObject();

        settings.updateTokenInfo(response);

        assertThat(settings.accessToken).isEqualTo("new-access-token");
        assertThat(settings.refreshToken).isEqualTo("new-refresh-token");
        assertThat(settings.exp).isEqualTo(3600L);
    }

    @Test
    void shouldHandleTokenExchangeErrorResponse() {
        String authCode = "invalid-code";
        String errorResponse = """
            {
                "error": "invalid_grant",
                "error_description": "The authorization code has expired or is invalid"
            }
            """;

        stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withStatusMessage("Bad Request")
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorResponse)));

        assertThatThrownBy(() -> Unirest.post(wireMockServer.baseUrl() + "/token")
                .header("authorization", "Basic " + settings.getBasicAuthHeader())
                .accept("application/json")
                .field("grant_type", "authorization_code")
                .field("code", authCode)
                .contentType("application/x-www-form-urlencoded")
                .asJson()
                .getBody()
                .getObject())
                .hasMessageContaining("400");
    }

    @Test
    void shouldHandleExpiredRefreshToken() {
        settings.refreshToken = "expired-refresh-token";
        
        String errorResponse = """
            {
                "error": "invalid_grant",
                "error_description": "The refresh token has expired"
            }
            """;

        stubFor(post(urlEqualTo("/token"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withStatusMessage("Bad Request")
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorResponse)));

        assertThatThrownBy(() -> Unirest.post(wireMockServer.baseUrl() + "/token")
                .header("authorization", "Basic " + settings.getBasicAuthHeader())
                .accept("application/json")
                .field("grant_type", "refresh_token")
                .field("refresh_token", settings.refreshToken)
                .contentType("application/x-www-form-urlencoded")
                .asJson()
                .getBody()
                .getObject())
                .hasMessageContaining("400");
    }

    @Test
    void shouldMakeAuthenticatedApiCallWithAccessToken() {
        settings.accessToken = "valid-access-token";
        settings.apiUrl = wireMockServer.baseUrl() + "/platform";

        String organizationsResponse = """
            {
                "values": [
                    {
                        "id": "org-123",
                        "name": "Test Farm",
                        "links": [
                            {"rel": "self", "uri": "%s/platform/organizations/org-123"}
                        ]
                    }
                ],
                "links": [
                    {"rel": "self", "uri": "%s/platform/organizations"}
                ]
            }
            """.formatted(wireMockServer.baseUrl(), wireMockServer.baseUrl());

        stubFor(get(urlEqualTo("/platform/organizations"))
                .withHeader("authorization", equalTo("Bearer " + settings.accessToken))
                .withHeader("Accept", equalTo("application/vnd.deere.axiom.v3+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(organizationsResponse)));

        JSONObject response = Unirest.get(settings.apiUrl + "/organizations")
                .header("authorization", "Bearer " + settings.accessToken)
                .accept("application/vnd.deere.axiom.v3+json")
                .asJson()
                .getBody()
                .getObject();

        assertThat(response.getJSONArray("values")).hasSize(1);
        assertThat(response.getJSONArray("values").getJSONObject(0).getString("name")).isEqualTo("Test Farm");
    }

    @Test
    void shouldDetectOrganizationNeedingConnectionsSetup() {
        settings.accessToken = "valid-access-token";
        settings.apiUrl = wireMockServer.baseUrl() + "/platform";

        String organizationsResponse = """
            {
                "values": [
                    {
                        "id": "org-456",
                        "name": "New Farm",
                        "links": [
                            {"rel": "self", "uri": "%s/platform/organizations/org-456"},
                            {"rel": "connections", "uri": "https://connections.deere.com/setup/org-456"}
                        ]
                    }
                ]
            }
            """.formatted(wireMockServer.baseUrl());

        stubFor(get(urlEqualTo("/platform/organizations"))
                .withHeader("authorization", equalTo("Bearer " + settings.accessToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(organizationsResponse)));

        JSONObject response = Unirest.get(settings.apiUrl + "/organizations")
                .header("authorization", "Bearer " + settings.accessToken)
                .accept("application/vnd.deere.axiom.v3+json")
                .asJson()
                .getBody()
                .getObject();

        boolean hasConnectionsRel = false;
        for (int i = 0; i < response.getJSONArray("values").length(); i++) {
            JSONObject org = response.getJSONArray("values").getJSONObject(i);
            for (int j = 0; j < org.getJSONArray("links").length(); j++) {
                JSONObject link = org.getJSONArray("links").getJSONObject(j);
                if ("connections".equals(link.getString("rel"))) {
                    hasConnectionsRel = true;
                    break;
                }
            }
        }

        assertThat(hasConnectionsRel).isTrue();
    }

    @Test
    void shouldHandleUnauthorizedApiCall() {
        settings.accessToken = "expired-access-token";
        settings.apiUrl = wireMockServer.baseUrl() + "/platform";

        stubFor(get(urlEqualTo("/platform/organizations"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withStatusMessage("Unauthorized")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Token expired\"}")));

        assertThatThrownBy(() -> Unirest.get(settings.apiUrl + "/organizations")
                .header("authorization", "Bearer " + settings.accessToken)
                .accept("application/vnd.deere.axiom.v3+json")
                .asJson()
                .getBody()
                .getObject())
                .hasMessageContaining("401");
    }

    @Test
    void shouldCompleteFullOAuth2FlowFromMetadataToApiCall() {
        String wellKnownResponse = """
            {
                "authorization_endpoint": "%s/authorize",
                "token_endpoint": "%s/token"
            }
            """.formatted(wireMockServer.baseUrl(), wireMockServer.baseUrl());

        String tokenResponse = """
            {
                "access_token": "full-flow-access-token",
                "refresh_token": "full-flow-refresh-token",
                "expires_in": 3600
            }
            """;

        String apiResponse = """
            {
                "values": [{"id": "org-1", "name": "Full Flow Farm", "links": []}]
            }
            """;

        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(wellKnownResponse)));

        stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(tokenResponse)));

        stubFor(get(urlEqualTo("/platform/organizations"))
                .withHeader("authorization", equalTo("Bearer full-flow-access-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(apiResponse)));

        JSONObject metadata = Unirest.get(settings.wellKnown)
                .asJson()
                .getBody()
                .getObject();
        String tokenEndpoint = metadata.getString("token_endpoint");

        JSONObject tokens = Unirest.post(tokenEndpoint)
                .header("authorization", "Basic " + settings.getBasicAuthHeader())
                .field("grant_type", "authorization_code")
                .field("code", "test-code")
                .contentType("application/x-www-form-urlencoded")
                .asJson()
                .getBody()
                .getObject();
        settings.updateTokenInfo(tokens);

        JSONObject organizations = Unirest.get(wireMockServer.baseUrl() + "/platform/organizations")
                .header("authorization", "Bearer " + settings.accessToken)
                .accept("application/vnd.deere.axiom.v3+json")
                .asJson()
                .getBody()
                .getObject();

        assertThat(settings.accessToken).isEqualTo("full-flow-access-token");
        assertThat(settings.refreshToken).isEqualTo("full-flow-refresh-token");
        assertThat(organizations.getJSONArray("values").getJSONObject(0).getString("name")).isEqualTo("Full Flow Farm");

        verify(getRequestedFor(urlEqualTo("/.well-known/oauth-authorization-server")));
        verify(postRequestedFor(urlEqualTo("/token")));
        verify(getRequestedFor(urlEqualTo("/platform/organizations")));
    }
}
