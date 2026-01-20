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
    private JsonNode mockJsonNode;

    @Test
    void testExceptionMessageFormat() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("{\"error\":\"Resource not found\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("Request Error!"));
        assertTrue(message.contains("Status: 404 Not Found"));
        assertTrue(message.contains("Body: {\"error\":\"Resource not found\"}"));
    }

    @Test
    void testExceptionWith401Unauthorized() {
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("{\"error\":\"Invalid token\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("401 Unauthorized"));
        assertTrue(message.contains("Invalid token"));
    }

    @Test
    void testExceptionWith500InternalServerError() {
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("{\"error\":\"Server error occurred\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("500 Internal Server Error"));
        assertTrue(message.contains("Server error occurred"));
    }

    @Test
    void testExceptionIsRuntimeException() {
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testExceptionWithEmptyBody() {
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("403 Forbidden"));
        assertTrue(message.contains("Body:"));
    }
}
