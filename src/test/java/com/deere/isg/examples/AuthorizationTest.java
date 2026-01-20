package com.deere.isg.examples;

import kong.unirest.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authorization Tests - Private Transaction Access")
class AuthorizationTest {

    private LoggingInterceptor interceptor;

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private HttpRequestSummary mockRequest;

    @Mock
    private Config mockConfig;

    @Mock
    private JsonNode mockBody;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
    }

    @Nested
    @DisplayName("401 Unauthorized Tests")
    class UnauthorizedTests {

        @Test
        @DisplayName("Should throw RequestException for 401 when accessing private transaction without token")
        void shouldThrowExceptionFor401WithoutToken() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Missing or invalid authorization token\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
        }

        @Test
        @DisplayName("Should throw RequestException for 401 when token is expired")
        void shouldThrowExceptionFor401WithExpiredToken() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Token has expired\", \"error_description\": \"The access token provided has expired\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Token has expired"));
        }

        @Test
        @DisplayName("Should throw RequestException for 401 when token is malformed")
        void shouldThrowExceptionFor401WithMalformedToken() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"Token is malformed\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123/comments");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("invalid_token"));
        }
    }

    @Nested
    @DisplayName("403 Forbidden Tests - Private Transaction Access")
    class ForbiddenTests {

        @Test
        @DisplayName("Should throw RequestException for 403 when commenting on private transaction without permission")
        void shouldThrowExceptionFor403WhenCommentingOnPrivateTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Access denied\", \"message\": \"You do not have permission to comment on this private transaction\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123/comments");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
            assertTrue(exception.getMessage().contains("permission to comment"));
        }

        @Test
        @DisplayName("Should throw RequestException for 403 when accessing private transaction from another organization")
        void shouldThrowExceptionFor403WhenAccessingOtherOrgTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Access denied\", \"message\": \"Transaction belongs to a different organization\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/other-org/transactions/123");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("different organization"));
        }

        @Test
        @DisplayName("Should throw RequestException for 403 when user lacks required scope")
        void shouldThrowExceptionFor403WhenMissingScope() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"The request requires higher privileges than provided by the access token\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/123/comments");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
        }

        @Test
        @DisplayName("Should throw RequestException for 403 when modifying read-only transaction")
        void shouldThrowExceptionFor403WhenModifyingReadOnlyTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Access denied\", \"message\": \"Transaction is read-only and cannot be modified\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.PUT);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/readonly-123");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("read-only"));
        }

        @Test
        @DisplayName("Should throw RequestException for 403 when deleting private transaction without owner permission")
        void shouldThrowExceptionFor403WhenDeletingWithoutOwnerPermission() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Access denied\", \"message\": \"Only the owner can delete this transaction\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.DELETE);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Only the owner"));
        }
    }

    @Nested
    @DisplayName("RequestException Message Format Tests")
    class ExceptionMessageTests {

        @Test
        @DisplayName("Exception message should contain HTTP method context for authorization errors")
        void exceptionShouldContainMethodContext() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Access denied to private resource\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123/comments");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            String message = exception.getMessage();
            assertTrue(message.contains("Status: 403 Forbidden"));
            assertTrue(message.contains("Body:"));
            assertTrue(message.contains("Access denied"));
        }

        @Test
        @DisplayName("Exception should be RuntimeException for easy propagation")
        void exceptionShouldBeRuntimeException() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Unauthorized\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/123");

            RequestException exception = assertThrows(RequestException.class, () -> {
                interceptor.onResponse(mockResponse, mockRequest, mockConfig);
            });

            assertTrue(exception instanceof RuntimeException);
        }
    }
}
