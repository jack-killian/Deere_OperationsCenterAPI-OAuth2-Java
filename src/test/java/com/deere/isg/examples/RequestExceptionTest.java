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
    @DisplayName("Should create exception with 400 Bad Request status")
    void shouldCreateExceptionWithBadRequestStatus() {
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID\"}");

        RequestException exception = new RequestException(mockResponse);

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("Bad Request"));
        assertTrue(exception.getMessage().contains("Invalid transaction ID"));
    }

    @Test
    @DisplayName("Should create exception with 404 Not Found status for invalid transaction ID")
    void shouldCreateExceptionWithNotFoundStatusForInvalidTransactionId() {
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction not found\", \"transactionId\": \"invalid-123\"}");

        RequestException exception = new RequestException(mockResponse);

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }

    @Test
    @DisplayName("Should create exception with 500 Internal Server Error status")
    void shouldCreateExceptionWithInternalServerErrorStatus() {
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
    @DisplayName("Should create exception with 422 Unprocessable Entity for validation errors")
    void shouldCreateExceptionWithUnprocessableEntityStatus() {
        when(mockResponse.getStatus()).thenReturn(422);
        when(mockResponse.getStatusText()).thenReturn("Unprocessable Entity");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Validation failed\", \"details\": \"Transaction ID format invalid\"}");

        RequestException exception = new RequestException(mockResponse);

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("422"));
        assertTrue(exception.getMessage().contains("Unprocessable Entity"));
        assertTrue(exception.getMessage().contains("Validation failed"));
    }

    @Test
    @DisplayName("Should extend RuntimeException")
    void shouldExtendRuntimeException() {
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{}");

        RequestException exception = new RequestException(mockResponse);

        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should include full error body in exception message")
    void shouldIncludeFullErrorBodyInMessage() {
        String errorBody = "{\"error\": \"Transaction error\", \"code\": \"TXN_001\", \"message\": \"The specified transaction ID does not exist or has been deleted\"}";
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn(errorBody);

        RequestException exception = new RequestException(mockResponse);

        assertTrue(exception.getMessage().contains("TXN_001"));
        assertTrue(exception.getMessage().contains("does not exist or has been deleted"));
    }
}
