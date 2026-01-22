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
@DisplayName("Authentication Failure Tests")
class AuthenticationFailureTest {

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
    @DisplayName("Missing Token Tests")
    class MissingTokenTests {

        @Test
        @DisplayName("Should throw RequestException when no authorization token is provided")
        void shouldThrowExceptionWhenNoTokenProvided() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"missing_token\", \"error_description\": \"No authorization token was provided\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
        }

        @Test
        @DisplayName("Should throw RequestException when authorization header is empty")
        void shouldThrowExceptionWhenAuthorizationHeaderEmpty() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_request\", \"error_description\": \"Authorization header is empty\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("invalid_request"));
        }

        @Test
        @DisplayName("Should throw RequestException when Bearer prefix is missing")
        void shouldThrowExceptionWhenBearerPrefixMissing() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_request\", \"error_description\": \"Invalid authorization header format. Expected Bearer token\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Invalid authorization header format"));
        }
    }

    @Nested
    @DisplayName("Expired Token Tests")
    class ExpiredTokenTests {

        @Test
        @DisplayName("Should throw RequestException when access token is expired")
        void shouldThrowExceptionWhenAccessTokenExpired() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"The access token has expired\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("access token has expired"));
        }

        @Test
        @DisplayName("Should throw RequestException when refresh token is expired")
        void shouldThrowExceptionWhenRefreshTokenExpired() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_grant\", \"error_description\": \"The refresh token has expired\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/v1/token");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("refresh token has expired"));
        }

        @Test
        @DisplayName("Should throw RequestException when token has been revoked")
        void shouldThrowExceptionWhenTokenRevoked() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"The access token has been revoked\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("token has been revoked"));
        }
    }

    @Nested
    @DisplayName("Invalid Token Tests")
    class InvalidTokenTests {

        @Test
        @DisplayName("Should throw RequestException when token is malformed")
        void shouldThrowExceptionWhenTokenMalformed() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"The access token is malformed\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("malformed"));
        }

        @Test
        @DisplayName("Should throw RequestException when token signature is invalid")
        void shouldThrowExceptionWhenTokenSignatureInvalid() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"Token signature verification failed\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("signature verification failed"));
        }

        @Test
        @DisplayName("Should throw RequestException when token is from wrong issuer")
        void shouldThrowExceptionWhenTokenFromWrongIssuer() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"Token issuer does not match expected issuer\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("issuer does not match"));
        }
    }

    @Nested
    @DisplayName("Insufficient Scope Tests")
    class InsufficientScopeTests {

        @Test
        @DisplayName("Should throw RequestException when token lacks required ag1 scope")
        void shouldThrowExceptionWhenMissingAg1Scope() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"The access token does not have the required 'ag1' scope\", \"scope\": \"ag1\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/fields");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
            assertTrue(exception.getMessage().contains("ag1"));
        }

        @Test
        @DisplayName("Should throw RequestException when token lacks required eq1 scope")
        void shouldThrowExceptionWhenMissingEq1Scope() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"The access token does not have the required 'eq1' scope\", \"scope\": \"eq1\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/machines");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
            assertTrue(exception.getMessage().contains("eq1"));
        }

        @Test
        @DisplayName("Should throw RequestException when token lacks offline_access scope for refresh")
        void shouldThrowExceptionWhenMissingOfflineAccessScope() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_grant\", \"error_description\": \"Refresh token was not issued because offline_access scope was not requested\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/v1/token");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("offline_access"));
        }

        @Test
        @DisplayName("Should throw RequestException when token lacks write scope for POST operation")
        void shouldThrowExceptionWhenMissingWriteScope() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"Write operations require additional scope permissions\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/fields");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
        }

        @Test
        @DisplayName("Should throw RequestException when multiple required scopes are missing")
        void shouldThrowExceptionWhenMultipleScopesMissing() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"The access token is missing required scopes\", \"required_scopes\": \"ag1 eq1\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(mockRequestSummary.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/operations");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
        }
    }

    @Nested
    @DisplayName("OAuth Token Exchange Failure Tests")
    class OAuthTokenExchangeFailureTests {

        @Test
        @DisplayName("Should throw RequestException when authorization code is invalid")
        void shouldThrowExceptionWhenAuthorizationCodeInvalid() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_grant\", \"error_description\": \"The authorization code is invalid or has expired\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/v1/token");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("invalid_grant"));
        }

        @Test
        @DisplayName("Should throw RequestException when client credentials are invalid")
        void shouldThrowExceptionWhenClientCredentialsInvalid() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_client\", \"error_description\": \"Client authentication failed\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/v1/token");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("invalid_client"));
        }

        @Test
        @DisplayName("Should throw RequestException when redirect URI does not match")
        void shouldThrowExceptionWhenRedirectUriMismatch() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"invalid_grant\", \"error_description\": \"The redirect_uri does not match the one used in the authorization request\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/v1/token");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("redirect_uri"));
        }

        @Test
        @DisplayName("Should throw RequestException when grant type is unsupported")
        void shouldThrowExceptionWhenGrantTypeUnsupported() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\": \"unsupported_grant_type\", \"error_description\": \"The grant type is not supported by the authorization server\"}");
            when(mockRequestSummary.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequestSummary.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/v1/token");

            RequestException exception = assertThrows(RequestException.class, () -> {
                loggingInterceptor.onResponse(mockResponse, mockRequestSummary, mockConfig);
            });

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("unsupported_grant_type"));
        }
    }
}
