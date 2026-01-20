package com.deere.isg.examples;

import kong.unirest.core.Config;
import kong.unirest.core.HttpMethod;
import kong.unirest.core.HttpRequestSummary;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingInterceptor Tests")
class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;

    @Mock
    private HttpResponse<JsonNode> response;

    @Mock
    private HttpRequestSummary request;

    @Mock
    private Config config;

    @Mock
    private JsonNode jsonBody;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw RequestException for 404 Not Found - valid transaction ID format but non-existent")
        void shouldThrowExceptionFor404NotFound() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(404);
            when(response.getStatusText()).thenReturn("Not Found");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Transaction not found\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("404"));
            assertTrue(exception.getMessage().contains("Not Found"));
        }

        @Test
        @DisplayName("Should throw RequestException for 400 Bad Request - invalid transaction ID format")
        void shouldThrowExceptionFor400BadRequest() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(400);
            when(response.getStatusText()).thenReturn("Bad Request");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID format\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("Bad Request"));
        }

        @Test
        @DisplayName("Should throw RequestException for 500 Internal Server Error")
        void shouldThrowExceptionFor500InternalServerError() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(500);
            when(response.getStatusText()).thenReturn("Internal Server Error");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Server error processing transaction\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("500"));
            assertTrue(exception.getMessage().contains("Internal Server Error"));
        }

        @Test
        @DisplayName("Should throw RequestException for 503 Service Unavailable")
        void shouldThrowExceptionFor503ServiceUnavailable() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(503);
            when(response.getStatusText()).thenReturn("Service Unavailable");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Service temporarily unavailable\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("503"));
            assertTrue(exception.getMessage().contains("Service Unavailable"));
        }

        @ParameterizedTest
        @ValueSource(ints = {400, 404, 422, 500, 502, 503})
        @DisplayName("Should throw RequestException for various error status codes")
        void shouldThrowExceptionForVariousErrorCodes(int statusCode) {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(statusCode);
            when(response.getStatusText()).thenReturn("Error");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Error occurred\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains(String.valueOf(statusCode)));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should throw RequestException for 401 Unauthorized - invalid or expired token")
        void shouldThrowExceptionFor401Unauthorized() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(401);
            when(response.getStatusText()).thenReturn("Unauthorized");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Invalid or expired access token\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
            assertTrue(exception.getMessage().contains("Invalid or expired access token"));
        }

        @Test
        @DisplayName("Should throw RequestException for 403 Forbidden - accessing private transaction without permission")
        void shouldThrowExceptionFor403ForbiddenPrivateTransaction() {
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-123");
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Access denied to private transaction\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
            assertTrue(exception.getMessage().contains("Access denied to private transaction"));
        }

        @Test
        @DisplayName("Should throw RequestException for 403 Forbidden - commenting on private transaction without access")
        void shouldThrowExceptionFor403ForbiddenCommentOnPrivateTransaction() {
            when(request.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-456/comments");
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Cannot comment on private transaction without organization access\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
            assertTrue(exception.getMessage().contains("Cannot comment on private transaction"));
        }

        @Test
        @DisplayName("Should throw RequestException for 403 Forbidden - insufficient scopes")
        void shouldThrowExceptionFor403InsufficientScopes() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Insufficient scopes for this operation\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                    interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Insufficient scopes"));
        }
    }

    @Nested
    @DisplayName("Success Response Tests")
    class SuccessResponseTests {

        @Test
        @DisplayName("Should not throw exception for successful 200 response")
        void shouldNotThrowExceptionForSuccessfulResponse() {
            when(response.isSuccess()).thenReturn(true);

            assertDoesNotThrow(() -> interceptor.onResponse(response, request, config));
        }

        @Test
        @DisplayName("Should not throw exception for 201 Created response")
        void shouldNotThrowExceptionFor201Created() {
            when(response.isSuccess()).thenReturn(true);

            assertDoesNotThrow(() -> interceptor.onResponse(response, request, config));
        }
    }
}
