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

        String message = exception.getMessage();
        assertTrue(message.contains("404"));
        assertTrue(message.contains("Not Found"));
        assertTrue(message.contains("Transaction not found"));
    }

    @Test
    @DisplayName("Should create exception with 400 Bad Request status for malformed transaction ID")
    void shouldCreateExceptionWith400Status() {
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID format\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("400"));
        assertTrue(message.contains("Bad Request"));
        assertTrue(message.contains("Invalid transaction ID format"));
    }

    @Test
    @DisplayName("Should create exception with 500 Internal Server Error status")
    void shouldCreateExceptionWith500Status() {
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Server error processing transaction\"}");

        RequestException exception = new RequestException(mockResponse);

        String message = exception.getMessage();
        assertTrue(message.contains("500"));
        assertTrue(message.contains("Internal Server Error"));
        assertTrue(message.contains("Server error processing transaction"));
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void shouldBeRuntimeException() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should include Request Error prefix in message")
    void shouldIncludeRequestErrorPrefix() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().startsWith("Request Error!"));
    }
}
