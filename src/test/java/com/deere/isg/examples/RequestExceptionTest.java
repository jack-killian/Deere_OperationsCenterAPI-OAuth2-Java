package com.deere.isg.examples;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestException Tests")
class RequestExceptionTest {

    @Mock
    private HttpResponse<JsonNode> response;

    @Mock
    private JsonNode jsonBody;

    @Nested
    @DisplayName("Error Message Formatting Tests")
    class ErrorMessageFormattingTests {

        @Test
        @DisplayName("Should include status code in exception message for 404 Not Found")
        void shouldIncludeStatusCodeFor404() {
            when(response.getStatus()).thenReturn(404);
            when(response.getStatusText()).thenReturn("Not Found");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Transaction ID abc-123 not found\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("404"));
            assertTrue(exception.getMessage().contains("Not Found"));
            assertTrue(exception.getMessage().contains("Transaction ID abc-123 not found"));
        }

        @Test
        @DisplayName("Should include status code in exception message for 400 Bad Request")
        void shouldIncludeStatusCodeFor400() {
            when(response.getStatus()).thenReturn(400);
            when(response.getStatusText()).thenReturn("Bad Request");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID format\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("Bad Request"));
            assertTrue(exception.getMessage().contains("Invalid transaction ID format"));
        }

        @ParameterizedTest
        @CsvSource({
                "400, Bad Request, Invalid request parameters",
                "401, Unauthorized, Invalid or expired access token",
                "403, Forbidden, Access denied to private resource",
                "404, Not Found, Resource not found",
                "422, Unprocessable Entity, Validation failed",
                "500, Internal Server Error, Server error occurred",
                "502, Bad Gateway, Upstream server error",
                "503, Service Unavailable, Service temporarily unavailable"
        })
        @DisplayName("Should format error message correctly for various HTTP error codes")
        void shouldFormatErrorMessageCorrectly(int statusCode, String statusText, String errorBody) {
            when(response.getStatus()).thenReturn(statusCode);
            when(response.getStatusText()).thenReturn(statusText);
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"" + errorBody + "\"}");

            RequestException exception = new RequestException(response);

            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains(String.valueOf(statusCode)));
            assertTrue(exception.getMessage().contains(statusText));
            assertTrue(exception.getMessage().contains(errorBody));
        }
    }

    @Nested
    @DisplayName("Authorization Error Tests")
    class AuthorizationErrorTests {

        @Test
        @DisplayName("Should capture 401 Unauthorized error details for expired token")
        void shouldCapture401UnauthorizedForExpiredToken() {
            when(response.getStatus()).thenReturn(401);
            when(response.getStatusText()).thenReturn("Unauthorized");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"access_token_expired\", \"error_description\": \"The access token has expired\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
            assertTrue(exception.getMessage().contains("access_token_expired"));
        }

        @Test
        @DisplayName("Should capture 401 Unauthorized error details for invalid token")
        void shouldCapture401UnauthorizedForInvalidToken() {
            when(response.getStatus()).thenReturn(401);
            when(response.getStatusText()).thenReturn("Unauthorized");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"The access token is invalid\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("invalid_token"));
        }

        @Test
        @DisplayName("Should capture 403 Forbidden error for accessing private transaction")
        void shouldCapture403ForbiddenForPrivateTransaction() {
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"access_denied\", \"error_description\": \"You do not have permission to access this private transaction\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
            assertTrue(exception.getMessage().contains("access_denied"));
            assertTrue(exception.getMessage().contains("private transaction"));
        }

        @Test
        @DisplayName("Should capture 403 Forbidden error for commenting on private transaction without access")
        void shouldCapture403ForbiddenForCommentingOnPrivateTransaction() {
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"insufficient_permissions\", \"error_description\": \"Cannot add comments to transactions in organizations you do not have access to\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
            assertTrue(exception.getMessage().contains("insufficient_permissions"));
            assertTrue(exception.getMessage().contains("Cannot add comments"));
        }

        @Test
        @DisplayName("Should capture 403 Forbidden error for insufficient OAuth scopes")
        void shouldCapture403ForbiddenForInsufficientScopes() {
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"The request requires higher privileges than provided by the access token\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
        }
    }

    @Nested
    @DisplayName("Transaction Error Tests")
    class TransactionErrorTests {

        @Test
        @DisplayName("Should capture error for valid transaction ID format but non-existent transaction")
        void shouldCaptureErrorForNonExistentTransaction() {
            when(response.getStatus()).thenReturn(404);
            when(response.getStatusText()).thenReturn("Not Found");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"not_found\", \"error_description\": \"Transaction with ID 'valid-uuid-format-1234' does not exist\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("404"));
            assertTrue(exception.getMessage().contains("not_found"));
            assertTrue(exception.getMessage().contains("valid-uuid-format-1234"));
        }

        @Test
        @DisplayName("Should capture error for transaction that has been deleted")
        void shouldCaptureErrorForDeletedTransaction() {
            when(response.getStatus()).thenReturn(410);
            when(response.getStatusText()).thenReturn("Gone");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{\"error\": \"resource_gone\", \"error_description\": \"Transaction has been deleted and is no longer available\"}");

            RequestException exception = new RequestException(response);

            assertTrue(exception.getMessage().contains("410"));
            assertTrue(exception.getMessage().contains("Gone"));
            assertTrue(exception.getMessage().contains("deleted"));
        }
    }

    @Nested
    @DisplayName("Exception Inheritance Tests")
    class ExceptionInheritanceTests {

        @Test
        @DisplayName("RequestException should extend RuntimeException")
        void shouldExtendRuntimeException() {
            when(response.getStatus()).thenReturn(500);
            when(response.getStatusText()).thenReturn("Internal Server Error");
            when(response.getBody()).thenReturn(jsonBody);
            when(jsonBody.toString()).thenReturn("{}");

            RequestException exception = new RequestException(response);

            assertTrue(exception instanceof RuntimeException);
        }
    }
}
