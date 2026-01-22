package com.deere.isg.examples;

import kong.unirest.core.Config;
import kong.unirest.core.HttpMethod;
import kong.unirest.core.HttpRequestSummary;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authorization Failure Tests for Private/Restricted Transactions")
class AuthorizationFailureTest {

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
    }

    @Nested
    @DisplayName("Private Transaction Access Denied")
    class PrivateTransactionAccessDenied {

        @Test
        @DisplayName("Should throw RequestException when accessing private transaction")
        void shouldThrowExceptionForPrivateTransaction() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-123");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Access denied\",\"message\":\"This transaction is private and cannot be accessed\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("private"));
        }

        @Test
        @DisplayName("Should throw RequestException when accessing another organization's private data")
        void shouldThrowExceptionForOtherOrgPrivateData() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/other-org-123/transactions");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Access denied\",\"message\":\"You do not have access to this organization's data\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("organization"));
        }
    }

    @Nested
    @DisplayName("Restricted Organization Resources")
    class RestrictedOrganizationResources {

        @Test
        @DisplayName("Should throw RequestException when accessing restricted organization")
        void shouldThrowExceptionForRestrictedOrganization() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/restricted-org-456");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Access restricted\",\"message\":\"Organization access has not been granted\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("restricted"));
        }

        @Test
        @DisplayName("Should throw RequestException when organization connection is pending")
        void shouldThrowExceptionForPendingOrganizationConnection() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/pending-org-789/fields");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Connection pending\",\"message\":\"Organization connection has not been completed\",\"links\":[{\"rel\":\"connections\",\"uri\":\"https://connections.deere.com/setup\"}]}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Connection pending"));
        }

        @Test
        @DisplayName("Should throw RequestException when accessing restricted field data")
        void shouldThrowExceptionForRestrictedFieldData() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/fields/field-456/boundaries");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Access denied\",\"message\":\"Field boundary data is restricted\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
        }
    }

    @Nested
    @DisplayName("Comment Attempts on Restricted Transactions")
    class CommentAttemptsOnRestrictedTransactions {

        @BeforeEach
        void setUpPostMethod() {
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        }

        @Test
        @DisplayName("Should throw RequestException when posting comment to private transaction")
        void shouldThrowExceptionWhenCommentingOnPrivateTransaction() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-123/comments");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Comment not allowed\",\"message\":\"Cannot add comments to private transactions\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Comment not allowed"));
        }

        @Test
        @DisplayName("Should throw RequestException when posting comment without write permission")
        void shouldThrowExceptionWhenCommentingWithoutWritePermission() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-456/comments");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Write access denied\",\"message\":\"User does not have write permission for this resource\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Write access denied"));
        }

        @Test
        @DisplayName("Should throw RequestException when posting comment to archived transaction")
        void shouldThrowExceptionWhenCommentingOnArchivedTransaction() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/archived-tx-789/comments");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Transaction archived\",\"message\":\"Cannot modify archived transactions\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("archived"));
        }

        @Test
        @DisplayName("Should throw RequestException when posting comment to read-only organization")
        void shouldThrowExceptionWhenCommentingInReadOnlyOrg() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/readonly-org/transactions/tx-123/comments");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Read-only access\",\"message\":\"Organization is configured for read-only access\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Read-only"));
        }
    }

    @Nested
    @DisplayName("Modification Attempts on Restricted Resources")
    class ModificationAttemptsOnRestrictedResources {

        @Test
        @DisplayName("Should throw RequestException when attempting PUT on restricted resource")
        void shouldThrowExceptionOnPutToRestrictedResource() {
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.PUT);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-123");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Modification denied\",\"message\":\"Cannot modify this transaction\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
        }

        @Test
        @DisplayName("Should throw RequestException when attempting DELETE on restricted resource")
        void shouldThrowExceptionOnDeleteRestrictedResource() {
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.DELETE);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-123");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"Deletion denied\",\"message\":\"Cannot delete this transaction\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Deletion denied"));
        }
    }
}
