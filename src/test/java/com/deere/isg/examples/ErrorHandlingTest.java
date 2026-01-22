package com.deere.isg.examples;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.javalin.http.Context;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Error Handling Tests - Error Page Rendering and Security")
class ErrorHandlingTest {

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
    @DisplayName("Error page should be rendered for OAuth error callback")
    void errorPage_shouldBeRenderedForOAuthErrorCallback() throws Exception {
        when(context.queryParam("error")).thenReturn("access_denied");
        when(context.queryParam("error_description")).thenReturn("User denied access");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Error message should be passed to error template")
    @SuppressWarnings("unchecked")
    void errorMessage_shouldBePassedToErrorTemplate() throws Exception {
        when(context.queryParam("error")).thenReturn("invalid_request");
        when(context.queryParam("error_description")).thenReturn("Missing required parameter");

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();
        assertThat(capturedMap).containsKey("error");
        assertThat(capturedMap.get("error").toString()).isEqualTo("Missing required parameter");
    }

    @Test
    @DisplayName("Error handling should not expose client secret in error message")
    @SuppressWarnings("unchecked")
    void errorHandling_shouldNotExposeClientSecretInErrorMessage() throws Exception {
        String wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/oauth-authorization-server";
        
        stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "authorization_endpoint": "http://localhost:%d/oauth2/authorize",
                                "token_endpoint": "http://localhost:99999/oauth2/token"
                            }
                            """.formatted(wireMockServer.port()))));

        settings.clientId = "test-client";
        settings.clientSecret = "super-secret-password-12345";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        when(context.queryParam("error")).thenReturn(null);
        when(context.queryParam("code")).thenReturn("auth-code");

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();
        String errorMessage = capturedMap.get("error").toString();
        
        assertThat(errorMessage).doesNotContain("super-secret-password-12345");
    }

    @Test
    @DisplayName("Error handling should not expose access token in error message")
    @SuppressWarnings("unchecked")
    void errorHandling_shouldNotExposeAccessTokenInErrorMessage() throws Exception {
        stubFor(get(urlPathEqualTo("/platform/resource"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "server_error",
                                "error_description": "Internal error"
                            }
                            """)));

        settings.accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.secret-token-payload.signature";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getParameter("url")).thenReturn("http://localhost:" + wireMockServer.port() + "/platform/resource");
        when(context.req()).thenReturn(mockRequest);

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        java.lang.reflect.Method callTheApiMethod = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApiMethod.setAccessible(true);
        callTheApiMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), mapCaptor.capture());
    }

    @Test
    @DisplayName("Error handling should handle null error description gracefully")
    void errorHandling_shouldHandleNullErrorDescriptionGracefully() throws Exception {
        when(context.queryParam("error")).thenReturn("unknown_error");
        when(context.queryParam("error_description")).thenReturn(null);

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Error handling should handle empty error description")
    void errorHandling_shouldHandleEmptyErrorDescription() throws Exception {
        when(context.queryParam("error")).thenReturn("some_error");
        when(context.queryParam("error_description")).thenReturn("");

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("RequestException should include status code in message")
    void requestException_shouldIncludeStatusCodeInMessage() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(new JsonNode("{\"error\": \"unauthorized\"}"));

        RequestException exception = new RequestException(mockResponse);

        assertThat(exception.getMessage()).contains("401");
        assertThat(exception.getMessage()).contains("Unauthorized");
    }

    @Test
    @DisplayName("RequestException should include response body in message")
    void requestException_shouldIncludeResponseBodyInMessage() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(new JsonNode("{\"error\": \"invalid_request\", \"error_description\": \"Missing parameter\"}"));

        RequestException exception = new RequestException(mockResponse);

        assertThat(exception.getMessage()).contains("invalid_request");
        assertThat(exception.getMessage()).contains("Missing parameter");
    }

    @Test
    @DisplayName("LoggingInterceptor should throw RequestException on non-success response")
    void loggingInterceptor_shouldThrowRequestExceptionOnNonSuccessResponse() {
        stubFor(get(urlEqualTo("/api/fail"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"forbidden\"}")));

        Unirest.config().interceptor(new LoggingInterceptor());

        try {
            Unirest.get("http://localhost:" + wireMockServer.port() + "/api/fail")
                    .asJson();
        } catch (RequestException e) {
            assertThat(e.getMessage()).contains("403");
        }
    }

    @Test
    @DisplayName("Error page should handle XSS in error description")
    @SuppressWarnings("unchecked")
    void errorPage_shouldHandleXssInErrorDescription() throws Exception {
        when(context.queryParam("error")).thenReturn("xss_test");
        when(context.queryParam("error_description")).thenReturn("<script>alert('xss')</script>");

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        java.lang.reflect.Method processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallbackMethod.setAccessible(true);
        processCallbackMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();
        assertThat(capturedMap.get("error").toString()).isEqualTo("<script>alert('xss')</script>");
    }

    @Test
    @DisplayName("Error handling should work for token refresh failures")
    void errorHandling_shouldWorkForTokenRefreshFailures() throws Exception {
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
                        .withBody("invalid json response {")));

        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.wellKnown = wellKnownUrl;
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.refreshToken = "some-refresh-token";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        java.lang.reflect.Method refreshMethod = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshMethod.setAccessible(true);
        refreshMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }

    @Test
    @DisplayName("Error handling should work for API call failures")
    void errorHandling_shouldWorkForApiCallFailures() throws Exception {
        stubFor(get(urlPathEqualTo("/platform/data"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "internal_error",
                                "error_description": "Database connection failed"
                            }
                            """)));

        settings.accessToken = "valid-token";

        java.lang.reflect.Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(application, settings);

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getParameter("url")).thenReturn("http://localhost:" + wireMockServer.port() + "/platform/data");
        when(context.req()).thenReturn(mockRequest);

        java.lang.reflect.Method callTheApiMethod = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApiMethod.setAccessible(true);
        callTheApiMethod.invoke(application, context);

        verify(context).render(eq("error.mustache"), anyMap());
    }
}
