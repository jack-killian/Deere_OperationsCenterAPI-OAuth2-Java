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
@DisplayName("Authorization Tests")
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
    @DisplayName("Private Transaction Access Tests")
    class PrivateTransactionAccessTests {

        @Test
        @DisplayName("Should throw RequestException when commenting on private transaction without authorization")
        void shouldThrowExceptionWhenCommentingOnPrivateTransactionWithoutAuth() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Access denied\", \"message\": \"You do not have permission to comment on this private transaction\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-txn-123/comments");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
            assertTrue(exception.getMessage().contains("do not have permission"));
        }

        @Test
        @DisplayName("Should throw RequestException when viewing private transaction without organization access")
        void shouldThrowExceptionWhenViewingPrivateTransactionWithoutOrgAccess() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Organization access required\", \"message\": \"You must have organization access to view this transaction\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-456/transactions/txn-789");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Organization access required"));
        }

        @Test
        @DisplayName("Should throw RequestException when modifying private transaction without write permission")
        void shouldThrowExceptionWhenModifyingPrivateTransactionWithoutWritePermission() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"Write access denied\", \"message\": \"Read-only access does not permit modifications to private transactions\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.PUT);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-txn-123");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Write access denied"));
        }
    }

    @Nested
    @DisplayName("Token Authorization Tests")
    class TokenAuthorizationTests {

        @Test
        @DisplayName("Should throw RequestException when access token is expired")
        void shouldThrowExceptionWhenAccessTokenExpired() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"The access token has expired\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
            assertTrue(exception.getMessage().contains("access token has expired"));
        }

        @Test
        @DisplayName("Should throw RequestException when access token is invalid")
        void shouldThrowExceptionWhenAccessTokenInvalid() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"The access token is malformed or invalid\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("malformed or invalid"));
        }

        @Test
        @DisplayName("Should throw RequestException when no authorization header provided")
        void shouldThrowExceptionWhenNoAuthorizationHeader() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"missing_authorization\", \"error_description\": \"Authorization header is required\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Authorization header is required"));
        }

        @Test
        @DisplayName("Should throw RequestException when token has insufficient scopes")
        void shouldThrowExceptionWhenTokenHasInsufficientScopes() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"The token does not have the required scope: ag1\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/fields");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
            assertTrue(exception.getMessage().contains("ag1"));
        }
    }

    @Nested
    @DisplayName("Organization Access Tests")
    class OrganizationAccessTests {

        @Test
        @DisplayName("Should throw RequestException when accessing organization without membership")
        void shouldThrowExceptionWhenAccessingOrgWithoutMembership() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"access_denied\", \"message\": \"User is not a member of this organization\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-not-member");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("not a member"));
        }

        @Test
        @DisplayName("Should throw RequestException when OAuth app connection not completed")
        void shouldThrowExceptionWhenOAuthAppConnectionNotCompleted() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockBody);
            when(mockBody.toString()).thenReturn("{\"error\": \"connection_required\", \"message\": \"OAuth application must complete organization connection setup\"}");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/fields");

            RequestException exception = assertThrows(RequestException.class, () ->
                interceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("connection_required"));
        }
    }
}
