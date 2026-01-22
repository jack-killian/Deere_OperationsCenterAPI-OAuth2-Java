package com.deere.isg.examples;

import kong.unirest.core.Config;
import kong.unirest.core.HttpMethod;
import kong.unirest.core.HttpRequestSummary;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Retrieval Error Tests")
class TransactionRetrievalErrorTest {

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private HttpRequestSummary mockRequest;

    @Mock
    private Config mockConfig;

    @Mock
    private JsonNode mockJsonNode;

    private LoggingInterceptor loggingInterceptor;

    @BeforeEach
    void setUp() {
        loggingInterceptor = new LoggingInterceptor();
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/12345");
    }

    @Nested
    @DisplayName("404 Not Found Errors")
    class NotFoundErrors {

        @Test
        @DisplayName("Should throw RequestException when transaction returns 404")
        void shouldThrowExceptionWhenTransactionNotFound() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(404);
            when(mockResponse.getStatusText()).thenReturn("Not Found");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Transaction not found\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("404"));
            assertTrue(exception.getMessage().contains("Not Found"));
        }

        @Test
        @DisplayName("Should throw RequestException for valid-looking but non-existent transaction ID")
        void shouldThrowExceptionForValidLookingNonExistentTransactionId() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/abc123-def456-ghi789");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(404);
            when(mockResponse.getStatusText()).thenReturn("Not Found");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Resource not found\",\"message\":\"No transaction exists with the specified ID\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("404"));
        }
    }

    @Nested
    @DisplayName("403 Forbidden Errors")
    class ForbiddenErrors {

        @Test
        @DisplayName("Should throw RequestException when transaction access is forbidden")
        void shouldThrowExceptionWhenTransactionForbidden() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Access denied to this transaction\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
        }

        @Test
        @DisplayName("Should throw RequestException when user lacks permission for transaction")
        void shouldThrowExceptionWhenUserLacksPermission() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Insufficient permissions\",\"required_scope\":\"ag3\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Insufficient permissions"));
        }
    }

    @Nested
    @DisplayName("5xx Server Errors")
    class ServerErrors {

        @Test
        @DisplayName("Should throw RequestException when server returns 500 Internal Server Error")
        void shouldThrowExceptionOn500Error() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(500);
            when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Internal server error\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("500"));
            assertTrue(exception.getMessage().contains("Internal Server Error"));
        }

        @Test
        @DisplayName("Should throw RequestException when server returns 502 Bad Gateway")
        void shouldThrowExceptionOn502Error() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(502);
            when(mockResponse.getStatusText()).thenReturn("Bad Gateway");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Bad gateway\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("502"));
            assertTrue(exception.getMessage().contains("Bad Gateway"));
        }

        @Test
        @DisplayName("Should throw RequestException when server returns 503 Service Unavailable")
        void shouldThrowExceptionOn503Error() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(503);
            when(mockResponse.getStatusText()).thenReturn("Service Unavailable");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Service temporarily unavailable\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("503"));
            assertTrue(exception.getMessage().contains("Service Unavailable"));
        }

        @Test
        @DisplayName("Should throw RequestException when server returns 504 Gateway Timeout")
        void shouldThrowExceptionOn504Error() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(504);
            when(mockResponse.getStatusText()).thenReturn("Gateway Timeout");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Gateway timeout\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("504"));
            assertTrue(exception.getMessage().contains("Gateway Timeout"));
        }
    }

    @Nested
    @DisplayName("RequestException Message Format")
    class RequestExceptionFormat {

        @Test
        @DisplayName("Should include status code in exception message")
        void shouldIncludeStatusCodeInMessage() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(404);
            when(mockResponse.getStatusText()).thenReturn("Not Found");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Not found\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("Status: 404"));
        }

        @Test
        @DisplayName("Should include response body in exception message")
        void shouldIncludeResponseBodyInMessage() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(500);
            when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            String errorBody = "{\"error\":\"Database connection failed\",\"code\":\"DB_ERROR\"}";
            when(mockJsonNode.toString()).thenReturn(errorBody);

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("Body:"));
            assertTrue(exception.getMessage().contains("Database connection failed"));
        }
    }
}
