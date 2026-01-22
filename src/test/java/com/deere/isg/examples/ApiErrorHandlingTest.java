package com.deere.isg.examples;

import kong.unirest.core.Config;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.HttpRequestSummary;
import kong.unirest.core.HttpMethod;
import kong.unirest.core.JsonNode;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("API Error Handling Tests")
class ApiErrorHandlingTest {

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private HttpRequestSummary mockRequestSummary;

    @Mock
    private Config mockConfig;

    @Mock
    private JsonNode mockJsonNode;

    private LoggingInterceptor loggingInterceptor;

    @BeforeEach
    void setUp() {
        loggingInterceptor = new LoggingInterceptor();
    }

    @Nested
    @DisplayName("Transaction Retrieval Error Tests")
    class TransactionRetrievalErrorTests {

        @Test
        @DisplayName("Should throw RequestException when transaction returns 404 Not Found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(404);
            when(mockResponse.getStatusText()).thenReturn("Not Found");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"Transaction not found\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/invalid-id-12345");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("404"));
            assertTrue(exception.getMessage().contains("Not Found"));
        }

        @Test
        @DisplayName("Should throw RequestException when transaction returns 403 Forbidden")
        void shouldThrowExceptionWhenTransactionForbidden() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"Access denied to this transaction\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/restricted-tx-67890");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
        }

        @Test
        @DisplayName("Should throw RequestException when transaction returns 500 Internal Server Error")
        void shouldThrowExceptionWhenServerError500() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(500);
            when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"Internal server error\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-123");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("500"));
            assertTrue(exception.getMessage().contains("Internal Server Error"));
        }

        @Test
        @DisplayName("Should throw RequestException when transaction returns 502 Bad Gateway")
        void shouldThrowExceptionWhenServerError502() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(502);
            when(mockResponse.getStatusText()).thenReturn("Bad Gateway");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"Bad gateway\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-456");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("502"));
            assertTrue(exception.getMessage().contains("Bad Gateway"));
        }

        @Test
        @DisplayName("Should throw RequestException when transaction returns 503 Service Unavailable")
        void shouldThrowExceptionWhenServerError503() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(503);
            when(mockResponse.getStatusText()).thenReturn("Service Unavailable");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"Service temporarily unavailable\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-789");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("503"));
            assertTrue(exception.getMessage().contains("Service Unavailable"));
        }

        @Test
        @DisplayName("Should throw RequestException when transaction returns 504 Gateway Timeout")
        void shouldThrowExceptionWhenServerError504() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(504);
            when(mockResponse.getStatusText()).thenReturn("Gateway Timeout");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"Gateway timeout\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-timeout");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("504"));
            assertTrue(exception.getMessage().contains("Gateway Timeout"));
        }

        @Test
        @DisplayName("Should not throw exception when transaction retrieval succeeds")
        void shouldNotThrowExceptionWhenTransactionRetrievalSucceeds() {
            when(mockResponse.isSuccess()).thenReturn(true);
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/valid-tx-123");

            assertDoesNotThrow(() -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });
        }
    }

    @Nested
    @DisplayName("RequestException Message Format Tests")
    class RequestExceptionMessageFormatTests {

        @Test
        @DisplayName("Should include status code in exception message")
        void shouldIncludeStatusCodeInExceptionMessage() {
            when(mockResponse.getStatus()).thenReturn(404);
            when(mockResponse.getStatusText()).thenReturn("Not Found");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"message\": \"Resource not found\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("404"));
        }

        @Test
        @DisplayName("Should include status text in exception message")
        void shouldIncludeStatusTextInExceptionMessage() {
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"message\": \"Access denied\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("Forbidden"));
        }

        @Test
        @DisplayName("Should include response body in exception message")
        void shouldIncludeResponseBodyInExceptionMessage() {
            when(mockResponse.getStatus()).thenReturn(500);
            when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            String errorBody = "{\"error\": \"Database connection failed\"}";
            when(mockJsonNode.toString()).thenReturn(errorBody);

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("Database connection failed"));
        }
    }
}
