package com.deere.isg.examples;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API Client Tests")
class ApiTest {

    private WireMockServer wireMockServer;
    private Api api;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        
        Unirest.config().reset();
        
        api = new Api();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        Unirest.shutDown();
    }

    @Test
    @DisplayName("GET request should include Bearer token in Authorization header")
    void get_shouldIncludeBearerToken() {
        stubFor(get(urlEqualTo("/api/resource"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\": \"test\"}")));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/resource";
        api.get("test-access-token", resourceUrl);

        verify(getRequestedFor(urlEqualTo("/api/resource"))
                .withHeader("authorization", equalTo("Bearer test-access-token")));
    }

    @Test
    @DisplayName("GET request should include correct Accept header")
    void get_shouldIncludeCorrectAcceptHeader() {
        stubFor(get(urlEqualTo("/api/resource"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\": \"test\"}")));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/resource";
        api.get("test-token", resourceUrl);

        verify(getRequestedFor(urlEqualTo("/api/resource"))
                .withHeader("Accept", equalTo("application/vnd.deere.axiom.v3+json")));
    }

    @Test
    @DisplayName("GET request should return parsed JSON response")
    void get_shouldReturnParsedJsonResponse() {
        stubFor(get(urlEqualTo("/api/organizations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "values": [
                                    {"id": "org1", "name": "Organization 1"},
                                    {"id": "org2", "name": "Organization 2"}
                                ],
                                "total": 2
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/organizations";
        JSONObject response = api.get("valid-token", resourceUrl);

        assertThat(response.getInt("total")).isEqualTo(2);
        assertThat(response.getJSONArray("values")).hasSize(2);
    }

    @Test
    @DisplayName("GET request should handle nested JSON structures")
    void get_shouldHandleNestedJsonStructures() {
        stubFor(get(urlEqualTo("/api/fields"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "field": {
                                    "id": "field-123",
                                    "boundaries": {
                                        "type": "Polygon",
                                        "coordinates": [[0, 0], [1, 1]]
                                    }
                                }
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/fields";
        JSONObject response = api.get("valid-token", resourceUrl);

        assertThat(response.getJSONObject("field").getString("id")).isEqualTo("field-123");
        assertThat(response.getJSONObject("field").getJSONObject("boundaries").getString("type")).isEqualTo("Polygon");
    }

    @Test
    @DisplayName("GET request should handle empty response body")
    void get_shouldHandleEmptyArrayResponse() {
        stubFor(get(urlEqualTo("/api/empty"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"values\": []}")));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/empty";
        JSONObject response = api.get("valid-token", resourceUrl);

        assertThat(response.getJSONArray("values")).isEmpty();
    }

    @Test
    @DisplayName("GET request should handle URLs with query parameters")
    void get_shouldHandleUrlsWithQueryParameters() {
        stubFor(get(urlPathEqualTo("/api/search"))
                .withQueryParam("q", equalTo("test"))
                .withQueryParam("limit", equalTo("10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\": []}")));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/search?q=test&limit=10";
        JSONObject response = api.get("valid-token", resourceUrl);

        assertThat(response.has("results")).isTrue();
    }

    @Test
    @DisplayName("GET request should handle special characters in token")
    void get_shouldHandleSpecialCharactersInToken() {
        stubFor(get(urlEqualTo("/api/resource"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"ok\"}")));

        String specialToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature";
        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/resource";
        api.get(specialToken, resourceUrl);

        verify(getRequestedFor(urlEqualTo("/api/resource"))
                .withHeader("authorization", equalTo("Bearer " + specialToken)));
    }
}
