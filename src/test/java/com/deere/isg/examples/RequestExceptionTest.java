package com.deere.isg.examples;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestException Tests")
class RequestExceptionTest {

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private JsonNode mockBody;

    @Test
    @DisplayName("Should create exception with 404 Not Found status for invalid transaction ID")
    void shouldCreateExceptionWith404Status() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction not found\"}");

        RequestException exception = new RequestException(mockResponse);

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }

    @Test
    @DisplayName("Should create exception with 400 Bad Request status for malformed transaction ID")
    void shouldCreateExceptionWith400Status() {
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID format\"}");

        RequestException exception = new RequestException(mockResponse);

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("Bad Request"));
        assertTrue(exception.getMessage().contains("Invalid transaction ID format"));
    }

    @Test
    @DisplayName("Should create exception with 500 Internal Server Error status")
    void shouldCreateExceptionWith500Status() {
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Server error processing transaction\"}");

        RequestException exception = new RequestException(mockResponse);

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @Test
    @DisplayName("Should create exception with 503 Service Unavailable status")
    void shouldCreateExceptionWith503Status() {
        when(mockResponse.getStatus()).thenReturn(503);
        when(mockResponse.getStatusText()).thenReturn("Service Unavailable");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"API temporarily unavailable\"}");

        RequestException exception = new RequestException(mockResponse);

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("503"));
        assertTrue(exception.getMessage().contains("Service Unavailable"));
    }

    @Test
    @DisplayName("Exception message should contain status, status text, and body")
    void exceptionMessageShouldContainAllDetails() {
        when(mockResponse.getStatus()).thenReturn(422);
        when(mockResponse.getStatusText()).thenReturn("Unprocessable Entity");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"errors\": [{\"field\": \"transactionId\", \"message\": \"Invalid format\"}]}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("Request Error!"));
        assertTrue(message.contains("Status: 422 Unprocessable Entity"));
        assertTrue(message.contains("Body:"));
        assertTrue(message.contains("Invalid format"));
    }
}
