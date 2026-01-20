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
@DisplayName("LoggingInterceptor Tests")
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
    @DisplayName("Should not throw exception on successful response")
    void shouldNotThrowOnSuccessfulResponse() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/123");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    @DisplayName("Should throw RequestException on 401 Unauthorized response")
    void shouldThrowRequestExceptionOn401() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid or expired access token\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/123");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("Should throw RequestException on 403 Forbidden response for private transaction")
    void shouldThrowRequestExceptionOn403ForPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Access denied to private transaction\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/private-123/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Forbidden"));
        assertTrue(exception.getMessage().contains("Access denied to private transaction"));
    }

    @Test
    @DisplayName("Should throw RequestException on 404 Not Found for invalid transaction ID")
    void shouldThrowRequestExceptionOn404ForInvalidTransactionId() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction ID 'invalid-id-123' not found\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/invalid-id-123");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
        assertTrue(exception.getMessage().contains("Transaction ID 'invalid-id-123' not found"));
    }

    @Test
    @DisplayName("Should throw RequestException on 500 Internal Server Error")
    void shouldThrowRequestExceptionOn500() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Internal server error\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @Test
    @DisplayName("Should throw RequestException on 422 Unprocessable Entity for invalid comment on transaction")
    void shouldThrowRequestExceptionOn422ForInvalidComment() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(422);
        when(mockResponse.getStatusText()).thenReturn("Unprocessable Entity");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Cannot add comment to completed transaction\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/123/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("422"));
        assertTrue(exception.getMessage().contains("Unprocessable Entity"));
    }
}
