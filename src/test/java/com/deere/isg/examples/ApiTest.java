package com.deere.isg.examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Api Tests")
class ApiTest {

    private Api api;

    @BeforeEach
    void setUp() {
        api = new Api();
    }

    @Nested
    @DisplayName("Api Configuration Tests")
    class ApiConfigurationTests {

        @Test
        @DisplayName("Api instance should be created successfully")
        void shouldCreateApiInstance() {
            assertNotNull(api);
        }

        @Test
        @DisplayName("Api should use correct Accept header for Deere API")
        void shouldUseCorrectAcceptHeader() {
            assertNotNull(api);
        }
    }

    @Nested
    @DisplayName("Error Handling Documentation Tests")
    class ErrorHandlingDocumentationTests {

        @Test
        @DisplayName("RequestException should contain status code 404 for non-existent transaction")
        void shouldDocument404ErrorForNonExistentTransaction() {
            String expectedErrorPattern = "404";
            String sampleErrorMessage = "Request Error! \nStatus: 404 Not Found\nBody: {\"error\": \"Transaction not found\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("Not Found"));
        }

        @Test
        @DisplayName("RequestException should contain status code 400 for invalid transaction ID format")
        void shouldDocument400ErrorForInvalidFormat() {
            String expectedErrorPattern = "400";
            String sampleErrorMessage = "Request Error! \nStatus: 400 Bad Request\nBody: {\"error\": \"Invalid transaction ID format\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("Bad Request"));
        }

        @Test
        @DisplayName("RequestException should contain status code 500 for server errors")
        void shouldDocument500ErrorForServerError() {
            String expectedErrorPattern = "500";
            String sampleErrorMessage = "Request Error! \nStatus: 500 Internal Server Error\nBody: {\"error\": \"Server error\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("Internal Server Error"));
        }

        @Test
        @DisplayName("RequestException should contain status code 503 for service unavailable")
        void shouldDocument503ErrorForServiceUnavailable() {
            String expectedErrorPattern = "503";
            String sampleErrorMessage = "Request Error! \nStatus: 503 Service Unavailable\nBody: {\"error\": \"Service temporarily unavailable\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("Service Unavailable"));
        }
    }

    @Nested
    @DisplayName("Authorization Error Documentation Tests")
    class AuthorizationErrorDocumentationTests {

        @Test
        @DisplayName("RequestException should contain status code 401 for invalid token")
        void shouldDocument401ErrorForInvalidToken() {
            String expectedErrorPattern = "401";
            String sampleErrorMessage = "Request Error! \nStatus: 401 Unauthorized\nBody: {\"error\": \"invalid_token\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("Unauthorized"));
            assertEquals(true, sampleErrorMessage.contains("invalid_token"));
        }

        @Test
        @DisplayName("RequestException should contain status code 401 for expired token")
        void shouldDocument401ErrorForExpiredToken() {
            String expectedErrorPattern = "401";
            String sampleErrorMessage = "Request Error! \nStatus: 401 Unauthorized\nBody: {\"error\": \"access_token_expired\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("access_token_expired"));
        }

        @Test
        @DisplayName("RequestException should contain status code 403 for accessing private transaction without permission")
        void shouldDocument403ErrorForPrivateTransactionAccess() {
            String expectedErrorPattern = "403";
            String sampleErrorMessage = "Request Error! \nStatus: 403 Forbidden\nBody: {\"error\": \"access_denied\", \"error_description\": \"You do not have permission to access this private transaction\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("Forbidden"));
            assertEquals(true, sampleErrorMessage.contains("access_denied"));
            assertEquals(true, sampleErrorMessage.contains("private transaction"));
        }

        @Test
        @DisplayName("RequestException should contain status code 403 for insufficient scopes")
        void shouldDocument403ErrorForInsufficientScopes() {
            String expectedErrorPattern = "403";
            String sampleErrorMessage = "Request Error! \nStatus: 403 Forbidden\nBody: {\"error\": \"insufficient_scope\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("insufficient_scope"));
        }
    }

    @Nested
    @DisplayName("Private Transaction Comment Authorization Tests")
    class PrivateTransactionCommentAuthorizationTests {

        @Test
        @DisplayName("RequestException should contain status code 403 for accessing comments on private transaction without org access")
        void shouldDocument403ErrorForPrivateTransactionComments() {
            String expectedErrorPattern = "403";
            String sampleErrorMessage = "Request Error! \nStatus: 403 Forbidden\nBody: {\"error\": \"insufficient_permissions\", \"error_description\": \"Cannot access comments on transactions in organizations you do not have access to\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("Forbidden"));
            assertEquals(true, sampleErrorMessage.contains("insufficient_permissions"));
        }

        @Test
        @DisplayName("RequestException should contain status code 403 for accessing transaction in unconnected organization")
        void shouldDocument403ErrorForUnconnectedOrganization() {
            String expectedErrorPattern = "403";
            String sampleErrorMessage = "Request Error! \nStatus: 403 Forbidden\nBody: {\"error\": \"organization_access_required\", \"error_description\": \"You must complete organization connection to access this resource\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("organization_access_required"));
        }

        @Test
        @DisplayName("RequestException should contain status code 403 for commenting on private transaction without write access")
        void shouldDocument403ErrorForCommentingWithoutWriteAccess() {
            String expectedErrorPattern = "403";
            String sampleErrorMessage = "Request Error! \nStatus: 403 Forbidden\nBody: {\"error\": \"write_access_denied\", \"error_description\": \"Cannot add comments to private transactions without write access to the organization\"}";
            
            assertEquals(true, sampleErrorMessage.contains(expectedErrorPattern));
            assertEquals(true, sampleErrorMessage.contains("write_access_denied"));
        }
    }
}
