package com.deere.isg.examples;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestExceptionTest {

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private JsonNode mockBody;

    @Test
    void shouldCreateExceptionWithNotFoundStatus() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction not found\"}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }

    @Test
    void shouldCreateExceptionWithUnauthorizedStatus() {
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid or expired token\"}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
        assertTrue(exception.getMessage().contains("Invalid or expired token"));
    }

    @Test
    void shouldCreateExceptionWithForbiddenStatus() {
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Access denied to private transaction\"}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Forbidden"));
        assertTrue(exception.getMessage().contains("Access denied to private transaction"));
    }

    @Test
    void shouldCreateExceptionWithServerErrorStatus() {
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Server error processing transaction\"}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
        assertTrue(exception.getMessage().contains("Server error processing transaction"));
    }

    @Test
    void shouldCreateExceptionWithBadRequestStatus() {
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID format\"}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("Bad Request"));
        assertTrue(exception.getMessage().contains("Invalid transaction ID format"));
    }

    @Test
    void exceptionMessageShouldContainRequestErrorPrefix() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().startsWith("Request Error!"));
    }

    @Test
    void exceptionShouldBeRuntimeException() {
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertInstanceOf(RuntimeException.class, exception);
    }
}
