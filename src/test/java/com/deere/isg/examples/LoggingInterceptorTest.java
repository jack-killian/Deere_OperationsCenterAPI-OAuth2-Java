package com.deere.isg.examples;

import kong.unirest.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    void shouldNotThrowExceptionOnSuccessfulResponse() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/123");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void shouldThrowRequestExceptionOnNotFoundError() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction ID 12345 not found\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/12345");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
    }

    @Test
    void shouldThrowRequestExceptionOnUnauthorizedError() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid or expired access token\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/private/transactions");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    void shouldThrowRequestExceptionOnForbiddenAccessToPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"You do not have permission to comment on this private transaction\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/private-123/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Forbidden"));
        assertTrue(exception.getMessage().contains("permission"));
    }

    @Test
    void shouldThrowRequestExceptionOnServerError() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"An unexpected error occurred\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @Test
    void shouldThrowRequestExceptionOnBadRequestWithInvalidTransactionId() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID format: abc-invalid\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/abc-invalid");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("Bad Request"));
    }

    @Test
    void shouldThrowRequestExceptionOnServiceUnavailable() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(503);
        when(mockResponse.getStatusText()).thenReturn("Service Unavailable");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Service temporarily unavailable\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("503"));
        assertTrue(exception.getMessage().contains("Service Unavailable"));
    }

    @Test
    void shouldHandlePostRequestErrors() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Cannot modify transaction owned by another organization\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/transactions/org-123/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
    }
}
