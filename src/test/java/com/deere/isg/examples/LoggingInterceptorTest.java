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
    @DisplayName("Should throw RequestException on 400 Bad Request")
    void shouldThrowExceptionOnBadRequest() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid request\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/invalid-id");

        assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );
    }

    @Test
    @DisplayName("Should throw RequestException on 401 Unauthorized")
    void shouldThrowExceptionOnUnauthorized() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid or expired token\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/organizations");

        RequestException exception = assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("Should throw RequestException on 403 Forbidden for private transactions")
    void shouldThrowExceptionOnForbiddenForPrivateTransactions() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Access denied to private transaction\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/private-123/comments");

        RequestException exception = assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Forbidden"));
        assertTrue(exception.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Should throw RequestException on 404 Not Found for invalid transaction ID")
    void shouldThrowExceptionOnNotFoundForInvalidTransactionId() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction not found\", \"transactionId\": \"nonexistent-456\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/nonexistent-456");

        RequestException exception = assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }

    @Test
    @DisplayName("Should throw RequestException on 500 Internal Server Error")
    void shouldThrowExceptionOnInternalServerError() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Server error\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions");

        assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );
    }

    @Test
    @DisplayName("Should not throw exception on successful response")
    void shouldNotThrowExceptionOnSuccess() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/organizations");

        assertDoesNotThrow(() -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );
    }

    @Test
    @DisplayName("Should throw RequestException on 503 Service Unavailable")
    void shouldThrowExceptionOnServiceUnavailable() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(503);
        when(mockResponse.getStatusText()).thenReturn("Service Unavailable");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Service temporarily unavailable\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions");

        RequestException exception = assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );

        assertTrue(exception.getMessage().contains("503"));
        assertTrue(exception.getMessage().contains("Service Unavailable"));
    }
}
