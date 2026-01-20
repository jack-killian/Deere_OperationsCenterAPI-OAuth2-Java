package com.deere.isg.examples;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestExceptionTest {

    @Test
    void testRequestExceptionMessage() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"invalid_token\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("Request Error!"));
        assertTrue(message.contains("401"));
        assertTrue(message.contains("Unauthorized"));
        assertTrue(message.contains("{\"error\":\"invalid_token\"}"));
    }

    @Test
    void testRequestExceptionWith400() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"invalid_request\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("400"));
        assertTrue(message.contains("Bad Request"));
    }

    @Test
    void testRequestExceptionWith500() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"server_error\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("500"));
        assertTrue(message.contains("Internal Server Error"));
    }

    @Test
    void testRequestExceptionWith404() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"resource_not_found\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("404"));
        assertTrue(message.contains("Not Found"));
    }

    @Test
    void testRequestExceptionIsRuntimeException() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testRequestExceptionWithEmptyBody() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("403"));
        assertTrue(message.contains("Forbidden"));
    }

    @Test
    void testRequestExceptionMessageFormat() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"message\":\"Token expired\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.startsWith("Request Error!"));
        assertTrue(message.contains("Status:"));
        assertTrue(message.contains("Body:"));
    }
}
