package com.deere.isg.examples;

import kong.unirest.core.Config;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.HttpRequestSummary;
import kong.unirest.core.HttpMethod;
import kong.unirest.core.JsonNode;
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
@DisplayName("Authorization Failure Tests for Private/Restricted Transactions")
class AuthorizationFailureTest {

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
    @DisplayName("Private Transaction Access Tests")
    class PrivateTransactionAccessTests {

        @Test
        @DisplayName("Should throw RequestException when accessing private transaction without permission")
        void shouldThrowExceptionWhenAccessingPrivateTransactionWithoutPermission() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"insufficient_permissions\", \"message\": \"User does not have access to this private transaction\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-001");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
        }

        @Test
        @DisplayName("Should throw RequestException when accessing restricted organization transaction")
        void shouldThrowExceptionWhenAccessingRestrictedOrgTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"organization_access_denied\", \"message\": \"User is not a member of the organization that owns this transaction\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/transactions/tx-456");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("organization_access_denied"));
        }

        @Test
        @DisplayName("Should throw RequestException when accessing transaction from unlinked organization")
        void shouldThrowExceptionWhenAccessingUnlinkedOrgTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"connection_required\", \"message\": \"OAuth application must be connected to the organization\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/unlinked-org/transactions/tx-789");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("connection_required"));
        }
    }

    @Nested
    @DisplayName("Comment Attempt Authorization Tests")
    class CommentAttemptAuthorizationTests {

        @Test
        @DisplayName("Should throw RequestException when posting comment to private transaction without permission")
        void shouldThrowExceptionWhenPostingCommentToPrivateTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"comment_not_allowed\", \"message\": \"Cannot add comments to private transactions without proper authorization\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-001/comments");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("comment_not_allowed"));
        }

        @Test
        @DisplayName("Should throw RequestException when posting comment to restricted transaction")
        void shouldThrowExceptionWhenPostingCommentToRestrictedTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"write_access_denied\", \"message\": \"User has read-only access to this transaction\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/restricted-tx-002/comments");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("write_access_denied"));
        }

        @Test
        @DisplayName("Should throw RequestException when posting comment to non-existent transaction")
        void shouldThrowExceptionWhenPostingCommentToNonExistentTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(404);
            when(mockResponse.getStatusText()).thenReturn("Not Found");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"transaction_not_found\", \"message\": \"The specified transaction does not exist\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/nonexistent-tx/comments");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("404"));
            assertTrue(exception.getMessage().contains("Not Found"));
        }

        @Test
        @DisplayName("Should throw RequestException when updating comment on restricted transaction")
        void shouldThrowExceptionWhenUpdatingCommentOnRestrictedTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"update_not_allowed\", \"message\": \"Cannot update comments on restricted transactions\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.PUT);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/restricted-tx-003/comments/comment-001");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("update_not_allowed"));
        }

        @Test
        @DisplayName("Should throw RequestException when deleting comment on restricted transaction")
        void shouldThrowExceptionWhenDeletingCommentOnRestrictedTransaction() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"delete_not_allowed\", \"message\": \"Cannot delete comments on restricted transactions\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.DELETE);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/restricted-tx-003/comments/comment-001");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("delete_not_allowed"));
        }
    }

    @Nested
    @DisplayName("Restricted Resource Access Tests")
    class RestrictedResourceAccessTests {

        @Test
        @DisplayName("Should throw RequestException when accessing restricted field data")
        void shouldThrowExceptionWhenAccessingRestrictedFieldData() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"field_access_denied\", \"message\": \"User does not have permission to access field data for this transaction\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-001/fields");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("field_access_denied"));
        }

        @Test
        @DisplayName("Should throw RequestException when accessing restricted equipment data")
        void shouldThrowExceptionWhenAccessingRestrictedEquipmentData() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"equipment_access_denied\", \"message\": \"User does not have permission to access equipment data\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-001/equipment");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("equipment_access_denied"));
        }

        @Test
        @DisplayName("Should throw RequestException when accessing transaction with revoked permissions")
        void shouldThrowExceptionWhenAccessingTransactionWithRevokedPermissions() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"permissions_revoked\", \"message\": \"Access permissions for this transaction have been revoked\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/revoked-tx-001");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("permissions_revoked"));
        }
    }
}
