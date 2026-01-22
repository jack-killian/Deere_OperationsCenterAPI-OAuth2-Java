package com.deere.isg.examples;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API Client Negative Tests - Error Handling")
class ApiNegativeTest {

    private WireMockServer wireMockServer;
    private Api api;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        
        Unirest.config().reset();
        Unirest.config().interceptor(new LoggingInterceptor());
        
        api = new Api();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        Unirest.shutDown();
    }

    @Test
    @DisplayName("GET request should throw exception on 401 Unauthorized")
    void get_shouldThrowExceptionOn401Unauthorized() {
        stubFor(get(urlEqualTo("/api/protected"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "unauthorized",
                                "error_description": "Invalid or expired access token"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/protected";
        
        assertThatThrownBy(() -> api.get("expired-token", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("GET request should throw exception on 403 Forbidden")
    void get_shouldThrowExceptionOn403Forbidden() {
        stubFor(get(urlEqualTo("/api/admin"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "forbidden",
                                "error_description": "Insufficient permissions to access this resource"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/admin";
        
        assertThatThrownBy(() -> api.get("valid-token-no-permission", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("403");
    }

    @Test
    @DisplayName("GET request should throw exception on 404 Not Found")
    void get_shouldThrowExceptionOn404NotFound() {
        stubFor(get(urlEqualTo("/api/nonexistent"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "not_found",
                                "error_description": "Resource not found"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/nonexistent";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("GET request should throw exception on 429 Rate Limited")
    void get_shouldThrowExceptionOn429RateLimited() {
        stubFor(get(urlEqualTo("/api/rate-limited"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "60")
                        .withBody("""
                            {
                                "error": "too_many_requests",
                                "error_description": "Rate limit exceeded. Please retry after 60 seconds."
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/rate-limited";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("429");
    }

    @Test
    @DisplayName("GET request should throw exception on 500 Internal Server Error")
    void get_shouldThrowExceptionOn500InternalServerError() {
        stubFor(get(urlEqualTo("/api/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "internal_server_error",
                                "error_description": "An unexpected error occurred"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/error";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("GET request should throw exception on 502 Bad Gateway")
    void get_shouldThrowExceptionOn502BadGateway() {
        stubFor(get(urlEqualTo("/api/gateway"))
                .willReturn(aResponse()
                        .withStatus(502)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "bad_gateway",
                                "error_description": "Bad gateway error"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/gateway";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("502");
    }

    @Test
    @DisplayName("GET request should throw exception on 503 Service Unavailable")
    void get_shouldThrowExceptionOn503ServiceUnavailable() {
        stubFor(get(urlEqualTo("/api/unavailable"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "service_unavailable",
                                "error_description": "Service temporarily unavailable"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/unavailable";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("503");
    }

    @Test
    @DisplayName("GET request should throw exception on malformed JSON response")
    void get_shouldThrowExceptionOnMalformedJsonResponse() {
        stubFor(get(urlEqualTo("/api/malformed"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not valid json {")));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/malformed";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("GET request should throw exception when endpoint is unreachable")
    void get_shouldThrowExceptionWhenEndpointUnreachable() {
        String resourceUrl = "http://localhost:99999/api/unreachable";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("GET request should throw exception on empty response body with error status")
    void get_shouldThrowExceptionOnEmptyResponseBodyWithErrorStatus() {
        stubFor(get(urlEqualTo("/api/empty-error"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/empty-error";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .hasCauseInstanceOf(RequestException.class)
                .hasMessageContaining("400");
    }

    @Test
    @DisplayName("GET request should handle null access token")
    void get_shouldHandleNullAccessToken() {
        stubFor(get(urlEqualTo("/api/resource"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "unauthorized",
                                "error_description": "Missing authorization header"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/resource";
        
        assertThatThrownBy(() -> api.get(null, resourceUrl))
                .hasCauseInstanceOf(RequestException.class);
    }

    @Test
    @DisplayName("GET request should handle empty access token")
    void get_shouldHandleEmptyAccessToken() {
        stubFor(get(urlEqualTo("/api/resource"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "unauthorized",
                                "error_description": "Invalid authorization header"
                            }
                            """)));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/resource";
        
        assertThatThrownBy(() -> api.get("", resourceUrl))
                .hasCauseInstanceOf(RequestException.class);
    }

    @Test
    @DisplayName("GET request should throw exception on connection timeout")
    void get_shouldThrowExceptionOnConnectionTimeout() {
        Unirest.config().connectTimeout(100);
        
        stubFor(get(urlEqualTo("/api/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000)
                        .withBody("{}")));

        String resourceUrl = "http://localhost:" + wireMockServer.port() + "/api/slow";
        
        assertThatThrownBy(() -> api.get("valid-token", resourceUrl))
                .isInstanceOf(Exception.class);
    }
}
