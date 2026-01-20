package com.deere.isg.examples;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestExceptionTest {

    @Mock
    private HttpResponse<JsonNode> response;

    @Mock
    private JsonNode body;

    @Test
    void constructor_withNotFoundResponse_containsStatusAndBody() {
        when(response.getStatus()).thenReturn(404);
        when(response.getStatusText()).thenReturn("Not Found");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Transaction not found\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }

    @Test
    void constructor_withUnauthorizedResponse_containsStatusAndBody() {
        when(response.getStatus()).thenReturn(401);
        when(response.getStatusText()).thenReturn("Unauthorized");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Invalid or expired token\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
        assertTrue(exception.getMessage().contains("Invalid or expired token"));
    }

    @Test
    void constructor_withForbiddenResponse_containsStatusAndBody() {
        when(response.getStatus()).thenReturn(403);
        when(response.getStatusText()).thenReturn("Forbidden");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Access denied to private transaction\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Forbidden"));
        assertTrue(exception.getMessage().contains("Access denied to private transaction"));
    }

    @Test
    void constructor_withInternalServerError_containsStatusAndBody() {
        when(response.getStatus()).thenReturn(500);
        when(response.getStatusText()).thenReturn("Internal Server Error");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Server error processing request\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
        assertTrue(exception.getMessage().contains("Server error processing request"));
    }

    @Test
    void constructor_withBadRequestResponse_containsStatusAndBody() {
        when(response.getStatus()).thenReturn(400);
        when(response.getStatusText()).thenReturn("Bad Request");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Invalid transaction ID format\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("Bad Request"));
        assertTrue(exception.getMessage().contains("Invalid transaction ID format"));
    }

    @Test
    void constructor_withConflictResponse_containsStatusAndBody() {
        when(response.getStatus()).thenReturn(409);
        when(response.getStatusText()).thenReturn("Conflict");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Transaction already exists\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().contains("409"));
        assertTrue(exception.getMessage().contains("Conflict"));
        assertTrue(exception.getMessage().contains("Transaction already exists"));
    }

    @Test
    void constructor_withGoneResponse_containsStatusAndBody() {
        when(response.getStatus()).thenReturn(410);
        when(response.getStatusText()).thenReturn("Gone");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Transaction has been deleted\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().contains("410"));
        assertTrue(exception.getMessage().contains("Gone"));
        assertTrue(exception.getMessage().contains("Transaction has been deleted"));
    }

    @Test
    void exceptionMessage_hasCorrectFormat() {
        when(response.getStatus()).thenReturn(404);
        when(response.getStatusText()).thenReturn("Not Found");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"message\": \"Resource not found\"}");

        RequestException exception = new RequestException(response);

        assertTrue(exception.getMessage().startsWith("Request Error!"));
        assertTrue(exception.getMessage().contains("Status: 404 Not Found"));
        assertTrue(exception.getMessage().contains("Body:"));
    }

    @Test
    void exception_isRuntimeException() {
        when(response.getStatus()).thenReturn(500);
        when(response.getStatusText()).thenReturn("Internal Server Error");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{}");

        RequestException exception = new RequestException(response);

        assertTrue(exception instanceof RuntimeException);
    }
}
