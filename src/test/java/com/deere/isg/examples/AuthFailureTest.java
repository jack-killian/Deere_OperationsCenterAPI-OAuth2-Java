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
@DisplayName("Authentication Failure Tests")
class AuthFailureTest {

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
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");
    }

    @Nested
    @DisplayName("Missing Access Token")
    class MissingAccessToken {

        @Test
        @DisplayName("Should throw RequestException when no authorization header is provided")
        void shouldThrowExceptionWhenNoAuthorizationHeader() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"unauthorized\",\"error_description\":\"No authorization header provided\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
        }

        @Test
        @DisplayName("Should throw RequestException when Bearer token is empty")
        void shouldThrowExceptionWhenBearerTokenEmpty() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"Bearer token is missing or empty\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("invalid_token"));
        }

        @Test
        @DisplayName("Should throw RequestException when authorization scheme is not Bearer")
        void shouldThrowExceptionWhenNotBearerScheme() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_request\",\"error_description\":\"Authorization scheme must be Bearer\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("invalid_request"));
        }
    }

    @Nested
    @DisplayName("Expired Access Token")
    class ExpiredAccessToken {

        @Test
        @DisplayName("Should throw RequestException when access token is expired")
        void shouldThrowExceptionWhenTokenExpired() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"The access token has expired\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("expired"));
        }

        @Test
        @DisplayName("Should throw RequestException with WWW-Authenticate header info for expired token")
        void shouldThrowExceptionWithWwwAuthenticateForExpiredToken() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"Token expired at 2024-01-15T10:30:00Z\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Token expired"));
        }

        @Test
        @DisplayName("Should throw RequestException when refresh token is also expired")
        void shouldThrowExceptionWhenRefreshTokenExpired() {
            when(mockRequest.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/token");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_grant\",\"error_description\":\"The refresh token has expired\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("refresh token"));
        }
    }

    @Nested
    @DisplayName("Invalid/Malformed Token")
    class InvalidMalformedToken {

        @Test
        @DisplayName("Should throw RequestException when token is malformed")
        void shouldThrowExceptionWhenTokenMalformed() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"Token is malformed or cannot be decoded\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

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
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"Token signature verification failed\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("signature"));
        }

        @Test
        @DisplayName("Should throw RequestException when token issuer is not trusted")
        void shouldThrowExceptionWhenTokenIssuerNotTrusted() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"Token issuer is not recognized\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("issuer"));
        }

        @Test
        @DisplayName("Should throw RequestException when token audience is incorrect")
        void shouldThrowExceptionWhenTokenAudienceIncorrect() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"Token audience does not match expected value\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("audience"));
        }
    }

    @Nested
    @DisplayName("Insufficient Scope")
    class InsufficientScope {

        @Test
        @DisplayName("Should throw RequestException when scope is insufficient for resource")
        void shouldThrowExceptionWhenScopeInsufficient() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"insufficient_scope\",\"error_description\":\"The request requires higher privileges than provided by the access token\",\"scope\":\"ag3\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("insufficient_scope"));
        }

        @Test
        @DisplayName("Should throw RequestException when ag1 scope is missing for field operations")
        void shouldThrowExceptionWhenAg1ScopeMissing() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/fields");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"insufficient_scope\",\"error_description\":\"ag1 scope required for field operations\",\"required_scope\":\"ag1\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("ag1"));
        }

        @Test
        @DisplayName("Should throw RequestException when eq1 scope is missing for equipment operations")
        void shouldThrowExceptionWhenEq1ScopeMissing() {
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/machines");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"insufficient_scope\",\"error_description\":\"eq1 scope required for equipment operations\",\"required_scope\":\"eq1\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("eq1"));
        }

        @Test
        @DisplayName("Should throw RequestException when offline_access scope is missing for refresh")
        void shouldThrowExceptionWhenOfflineAccessScopeMissing() {
            when(mockRequest.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/token");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_grant\",\"error_description\":\"offline_access scope was not requested during authorization\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("offline_access"));
        }

        @Test
        @DisplayName("Should throw RequestException when write scope is missing for POST operations")
        void shouldThrowExceptionWhenWriteScopeMissing() {
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-123/fields");
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(403);
            when(mockResponse.getStatusText()).thenReturn("Forbidden");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"insufficient_scope\",\"error_description\":\"Write scope required for this operation\",\"required_scope\":\"ag1:write\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Write scope"));
        }
    }

    @Nested
    @DisplayName("Token Endpoint Errors")
    class TokenEndpointErrors {

        @BeforeEach
        void setUpTokenEndpoint() {
            when(mockRequest.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/token");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        }

        @Test
        @DisplayName("Should throw RequestException when client credentials are invalid")
        void shouldThrowExceptionWhenClientCredentialsInvalid() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_client\",\"error_description\":\"Client authentication failed\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("invalid_client"));
        }

        @Test
        @DisplayName("Should throw RequestException when authorization code is invalid")
        void shouldThrowExceptionWhenAuthCodeInvalid() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_grant\",\"error_description\":\"The authorization code is invalid or has expired\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("authorization code"));
        }

        @Test
        @DisplayName("Should throw RequestException when redirect URI does not match")
        void shouldThrowExceptionWhenRedirectUriMismatch() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_grant\",\"error_description\":\"redirect_uri does not match the one used during authorization\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

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
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"unsupported_grant_type\",\"error_description\":\"The grant type is not supported\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("unsupported_grant_type"));
        }
    }

    @Nested
    @DisplayName("Revoked Token")
    class RevokedToken {

        @Test
        @DisplayName("Should throw RequestException when access token has been revoked")
        void shouldThrowExceptionWhenAccessTokenRevoked() {
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(401);
            when(mockResponse.getStatusText()).thenReturn("Unauthorized");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_token\",\"error_description\":\"The access token has been revoked\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("revoked"));
        }

        @Test
        @DisplayName("Should throw RequestException when refresh token has been revoked")
        void shouldThrowExceptionWhenRefreshTokenRevoked() {
            when(mockRequest.getUrl()).thenReturn("https://signin.johndeere.com/oauth2/token");
            when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getStatus()).thenReturn(400);
            when(mockResponse.getStatusText()).thenReturn("Bad Request");
            when(mockResponse.getBody()).thenReturn(mockJsonNode);
            when(mockJsonNode.toString()).thenReturn("{\"error\":\"invalid_grant\",\"error_description\":\"The refresh token has been revoked\"}");

            RequestException exception = assertThrows(RequestException.class, () ->
                loggingInterceptor.onResponse(mockResponse, mockRequest, mockConfig)
            );

            assertTrue(exception.getMessage().contains("400"));
            assertTrue(exception.getMessage().contains("revoked"));
        }
    }
}
