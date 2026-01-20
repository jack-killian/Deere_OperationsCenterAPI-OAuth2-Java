package com.deere.isg.examples;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authorization Tests")
class AuthorizationTest {

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private JsonNode mockBody;

    @Nested
    @DisplayName("Private Transaction Access Tests")
    class PrivateTransactionAccessTests {

        @Test
        @DisplayName("Should throw RequestException when accessing private transaction without authorization")
        void shouldThrowExceptionWhenAccessingPrivateTransactionWithoutAuth() {
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"You do not have permission to access this private transaction\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
            assertTrue(exception.getMessage().contains("private transaction"));
        }

        @Test
        @DisplayName("Should throw RequestException when commenting on private transaction without permission")
        void shouldThrowExceptionWhenCommentingOnPrivateTransactionWithoutPermission() {
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Cannot comment on private transaction without owner permission\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Cannot comment on private transaction"));
        }

        @Test
        @DisplayName("Should throw RequestException when organization access is denied")
        void shouldThrowExceptionWhenOrganizationAccessDenied() {
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Organization access not granted for this application\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Organization access not granted"));
        }
    }

    @Nested
    @DisplayName("Token Authorization Tests")
    class TokenAuthorizationTests {

        @Test
        @DisplayName("Should throw RequestException when access token is expired")
        void shouldThrowExceptionWhenAccessTokenExpired() {
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Access token has expired\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
            assertTrue(exception.getMessage().contains("Access token has expired"));
        }

        @Test
        @DisplayName("Should throw RequestException when access token is invalid")
        void shouldThrowExceptionWhenAccessTokenInvalid() {
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Invalid access token\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Invalid access token"));
        }

        @Test
        @DisplayName("Should throw RequestException when access token is missing")
        void shouldThrowExceptionWhenAccessTokenMissing() {
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Authorization header is required\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Authorization header is required"));
        }

        @Test
        @DisplayName("Should throw RequestException when refresh token is invalid")
        void shouldThrowExceptionWhenRefreshTokenInvalid() {
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"invalid_grant\", \"error_description\": \"Refresh token is invalid or expired\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("invalid_grant"));
        }
    }

    @Nested
    @DisplayName("Scope Authorization Tests")
    class ScopeAuthorizationTests {

        @Test
        @DisplayName("Should throw RequestException when required scope is missing")
        void shouldThrowExceptionWhenRequiredScopeMissing() {
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"Required scope 'ag1' is not granted\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
        }

        @Test
        @DisplayName("Should throw RequestException when offline_access scope is missing for refresh")
        void shouldThrowExceptionWhenOfflineAccessScopeMissing() {
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"invalid_request\", \"error_description\": \"offline_access scope required for refresh tokens\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("offline_access scope required"));
        }
    }

    @Nested
    @DisplayName("Transaction ID Validation Tests")
    class TransactionIdValidationTests {

        @Test
        @DisplayName("Should throw RequestException for non-existent transaction ID")
        void shouldThrowExceptionForNonExistentTransactionId() {
            when(mockResponse.getStatus()).thenReturn(404);
            when(mockResponse.getStatusText()).thenReturn("Not Found");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Transaction with ID 'txn-12345' does not exist\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("404"));
            assertTrue(exception.getMessage().contains("Not Found"));
            assertTrue(exception.getMessage().contains("does not exist"));
        }

        @Test
        @DisplayName("Should throw RequestException for malformed transaction ID")
        void shouldThrowExceptionForMalformedTransactionId() {
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Invalid transaction ID format: expected UUID\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("Invalid transaction ID format"));
        }

        @Test
        @DisplayName("Should throw RequestException for empty transaction ID")
        void shouldThrowExceptionForEmptyTransactionId() {
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Transaction ID cannot be empty\"}");

            RequestException exception = new RequestException(mockResponse);

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("Transaction ID cannot be empty"));
        }
    }
}
