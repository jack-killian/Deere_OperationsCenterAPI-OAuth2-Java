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
    void testExceptionMessageContainsStatusAndBody() {
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"invalid_token\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("Request Error!"));
        assertTrue(message.contains("Status: 401 Unauthorized"));
        assertTrue(message.contains("Body: {\"error\":\"invalid_token\"}"));
    }

    @Test
    void testExceptionMessageWith404NotFound() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"message\":\"Resource not found\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("Status: 404 Not Found"));
        assertTrue(message.contains("{\"message\":\"Resource not found\"}"));
    }

    @Test
    void testExceptionMessageWith500InternalServerError() {
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"server_error\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("Status: 500 Internal Server Error"));
    }

    @Test
    void testExceptionIsRuntimeException() {
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception instanceof RuntimeException);
    }
}
