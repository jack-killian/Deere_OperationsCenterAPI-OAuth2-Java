package com.deere.isg.examples;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiTest {

    private static WireMockServer wireMockServer;
    private Api api;

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
        api = new Api();
        wireMockServer.resetAll();
        Unirest.config().reset();
        Unirest.config().interceptor(new LoggingInterceptor());
    }

    @AfterEach
    void tearDown() {
        Unirest.config().reset();
    }

    @Test
    void shouldMakeGetRequestWithBearerToken() {
        String accessToken = "test-access-token";
        String responseBody = "{\"id\": \"123\", \"name\": \"Test Organization\"}";

        stubFor(get(urlEqualTo("/organizations"))
                .withHeader("authorization", equalTo("Bearer " + accessToken))
                .withHeader("Accept", equalTo("application/vnd.deere.axiom.v3+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        JSONObject result = api.get(accessToken, wireMockServer.baseUrl() + "/organizations");

        assertThat(result.getString("id")).isEqualTo("123");
        assertThat(result.getString("name")).isEqualTo("Test Organization");

        verify(getRequestedFor(urlEqualTo("/organizations"))
                .withHeader("authorization", equalTo("Bearer " + accessToken)));
    }

    @Test
    void shouldIncludeCorrectAcceptHeader() {
        String accessToken = "test-token";
        String responseBody = "{\"data\": \"test\"}";

        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        api.get(accessToken, wireMockServer.baseUrl() + "/test");

        verify(getRequestedFor(urlEqualTo("/test"))
                .withHeader("Accept", equalTo("application/vnd.deere.axiom.v3+json")));
    }

    @Test
    void shouldThrowRequestExceptionOn401Unauthorized() {
        String accessToken = "invalid-token";

        stubFor(get(urlEqualTo("/protected"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withStatusMessage("Unauthorized")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Invalid token\"}")));

        assertThatThrownBy(() -> api.get(accessToken, wireMockServer.baseUrl() + "/protected"))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("401");
    }

    @Test
    void shouldThrowRequestExceptionOn403Forbidden() {
        String accessToken = "valid-token";

        stubFor(get(urlEqualTo("/forbidden"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withStatusMessage("Forbidden")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Access denied\"}")));

        assertThatThrownBy(() -> api.get(accessToken, wireMockServer.baseUrl() + "/forbidden"))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("403");
    }

    @Test
    void shouldThrowRequestExceptionOn404NotFound() {
        String accessToken = "valid-token";

        stubFor(get(urlEqualTo("/nonexistent"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withStatusMessage("Not Found")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Resource not found\"}")));

        assertThatThrownBy(() -> api.get(accessToken, wireMockServer.baseUrl() + "/nonexistent"))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("404");
    }

    @Test
    void shouldThrowRequestExceptionOn500ServerError() {
        String accessToken = "valid-token";

        stubFor(get(urlEqualTo("/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withStatusMessage("Internal Server Error")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Server error\"}")));

        assertThatThrownBy(() -> api.get(accessToken, wireMockServer.baseUrl() + "/error"))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("500");
    }

    @Test
    void shouldHandleJsonArrayResponse() {
        String accessToken = "test-token";
        String responseBody = "{\"values\": [{\"id\": \"1\"}, {\"id\": \"2\"}]}";

        stubFor(get(urlEqualTo("/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        JSONObject result = api.get(accessToken, wireMockServer.baseUrl() + "/list");

        assertThat(result.getJSONArray("values")).hasSize(2);
    }

    @Test
    void shouldHandleNestedJsonResponse() {
        String accessToken = "test-token";
        String responseBody = "{\"organization\": {\"id\": \"123\", \"details\": {\"name\": \"Test Org\"}}}";

        stubFor(get(urlEqualTo("/nested"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        JSONObject result = api.get(accessToken, wireMockServer.baseUrl() + "/nested");

        assertThat(result.getJSONObject("organization").getString("id")).isEqualTo("123");
        assertThat(result.getJSONObject("organization").getJSONObject("details").getString("name")).isEqualTo("Test Org");
    }

    @Test
    void shouldHandleEmptyJsonResponse() {
        String accessToken = "test-token";
        String responseBody = "{}";

        stubFor(get(urlEqualTo("/empty"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        JSONObject result = api.get(accessToken, wireMockServer.baseUrl() + "/empty");

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void shouldHandleUrlWithQueryParameters() {
        String accessToken = "test-token";
        String responseBody = "{\"filtered\": true}";

        stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("type", equalTo("equipment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        JSONObject result = api.get(accessToken, wireMockServer.baseUrl() + "/search?type=equipment");

        assertThat(result.getBoolean("filtered")).isTrue();
    }
}
