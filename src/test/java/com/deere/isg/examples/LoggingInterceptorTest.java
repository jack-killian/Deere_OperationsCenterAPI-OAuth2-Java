package com.deere.isg.examples;

import kong.unirest.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingInterceptor Tests - Error Handling")
class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private HttpRequestSummary mockRequest;

    @Mock
    private Config mockConfig;

    @Mock
    private JsonNode mockBody;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
    }

    @Test
    @DisplayName("Should throw RequestException for 404 Not Found - invalid transaction ID")
    void shouldThrowExceptionFor404NotFound() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction ID not found\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/invalid-id");

        RequestException exception = assertThrows(RequestException.class, () -> {
            interceptor.onResponse(mockResponse, mockRequest, mockConfig);
        });

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
    }

    @Test
    @DisplayName("Should throw RequestException for 400 Bad Request - malformed transaction ID")
    void shouldThrowExceptionFor400BadRequest() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Malformed transaction ID\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/%%%");

        RequestException exception = assertThrows(RequestException.class, () -> {
            interceptor.onResponse(mockResponse, mockRequest, mockConfig);
        });

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("Bad Request"));
    }

    @Test
    @DisplayName("Should throw RequestException for 500 Internal Server Error")
    void shouldThrowExceptionFor500InternalServerError() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Internal server error\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/123");

        RequestException exception = assertThrows(RequestException.class, () -> {
            interceptor.onResponse(mockResponse, mockRequest, mockConfig);
        });

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @Test
    @DisplayName("Should throw RequestException for 503 Service Unavailable")
    void shouldThrowExceptionFor503ServiceUnavailable() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(503);
        when(mockResponse.getStatusText()).thenReturn("Service Unavailable");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Service temporarily unavailable\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/123");

        RequestException exception = assertThrows(RequestException.class, () -> {
            interceptor.onResponse(mockResponse, mockRequest, mockConfig);
        });

        assertTrue(exception.getMessage().contains("503"));
        assertTrue(exception.getMessage().contains("Service Unavailable"));
    }

    @Test
    @DisplayName("Should not throw exception for successful response")
    void shouldNotThrowExceptionForSuccessfulResponse() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/valid-id");

        assertDoesNotThrow(() -> {
            interceptor.onResponse(mockResponse, mockRequest, mockConfig);
        });
    }

    @Test
    @DisplayName("Should throw RequestException for 422 Unprocessable Entity - validation error")
    void shouldThrowExceptionFor422UnprocessableEntity() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(422);
        when(mockResponse.getStatusText()).thenReturn("Unprocessable Entity");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"errors\": [{\"field\": \"transactionId\", \"message\": \"Invalid format\"}]}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions");

        RequestException exception = assertThrows(RequestException.class, () -> {
            interceptor.onResponse(mockResponse, mockRequest, mockConfig);
        });

        assertTrue(exception.getMessage().contains("422"));
        assertTrue(exception.getMessage().contains("Unprocessable Entity"));
    }

    @Test
    @DisplayName("Should throw RequestException for 429 Too Many Requests - rate limiting")
    void shouldThrowExceptionFor429TooManyRequests() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(429);
        when(mockResponse.getStatusText()).thenReturn("Too Many Requests");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Rate limit exceeded\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions");

        RequestException exception = assertThrows(RequestException.class, () -> {
            interceptor.onResponse(mockResponse, mockRequest, mockConfig);
        });

        assertTrue(exception.getMessage().contains("429"));
        assertTrue(exception.getMessage().contains("Too Many Requests"));
    }
}
