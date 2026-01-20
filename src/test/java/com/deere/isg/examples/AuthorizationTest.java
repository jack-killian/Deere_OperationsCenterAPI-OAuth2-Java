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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authorization Tests")
class AuthorizationTest {

    private LoggingInterceptor interceptor;

    @Mock
    private HttpResponse<JsonNode> response;

    @Mock
    private HttpRequestSummary request;

    @Mock
    private Config config;

    @Mock
    private JsonNode body;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
    }

    @Nested
    @DisplayName("Private Transaction Access Tests")
    class PrivateTransactionAccessTests {

        @Test
        @DisplayName("Should throw exception when accessing private transaction without authorization")
        void accessPrivateTransaction_withoutAuthorization_throwsForbiddenException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Access denied to private transaction\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
            assertTrue(exception.getMessage().contains("Forbidden"));
        }

        @Test
        @DisplayName("Should throw exception when commenting on private transaction without permission")
        void commentOnPrivateTransaction_withoutPermission_throwsForbiddenException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Cannot comment on private transaction\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123/comments");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
        }

        @Test
        @DisplayName("Should throw exception when modifying private transaction without permission")
        void modifyPrivateTransaction_withoutPermission_throwsForbiddenException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Cannot modify private transaction\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.PUT);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-123");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
        }
    }

    @Nested
    @DisplayName("Token Authorization Tests")
    class TokenAuthorizationTests {

        @Test
        @DisplayName("Should throw exception when token is expired")
        void apiCall_withExpiredToken_throwsUnauthorizedException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(401);
            when(response.getStatusText()).thenReturn("Unauthorized");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Token has expired\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("401"));
            assertTrue(exception.getMessage().contains("Unauthorized"));
        }

        @Test
        @DisplayName("Should throw exception when token is invalid")
        void apiCall_withInvalidToken_throwsUnauthorizedException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(401);
            when(response.getStatusText()).thenReturn("Unauthorized");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Invalid token\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/fields");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("401"));
        }

        @Test
        @DisplayName("Should throw exception when token is missing")
        void apiCall_withMissingToken_throwsUnauthorizedException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(401);
            when(response.getStatusText()).thenReturn("Unauthorized");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Missing authorization token\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/machines");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("401"));
        }
    }

    @Nested
    @DisplayName("Organization Access Tests")
    class OrganizationAccessTests {

        @Test
        @DisplayName("Should throw exception when accessing organization without membership")
        void accessOrganization_withoutMembership_throwsForbiddenException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Not a member of this organization\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-456/fields");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
        }

        @Test
        @DisplayName("Should throw exception when accessing organization resources without proper scope")
        void accessOrganizationResources_withoutProperScope_throwsForbiddenException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Insufficient scope for this resource\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-789/equipment");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
        }

        @Test
        @DisplayName("Should succeed when accessing organization with proper authorization")
        void accessOrganization_withProperAuthorization_succeeds() {
            when(response.isSuccess()).thenReturn(true);
            when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations");

            assertDoesNotThrow(() -> interceptor.onResponse(response, request, config));
        }
    }

    @Nested
    @DisplayName("Insufficient Permissions Tests")
    class InsufficientPermissionsTests {

        @Test
        @DisplayName("Should throw exception when user has read-only access but tries to write")
        void writeOperation_withReadOnlyAccess_throwsForbiddenException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Read-only access, write not permitted\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.POST);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/fields");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
        }

        @Test
        @DisplayName("Should throw exception when user tries to delete without delete permission")
        void deleteOperation_withoutDeletePermission_throwsForbiddenException() {
            when(response.isSuccess()).thenReturn(false);
            when(response.getStatus()).thenReturn(403);
            when(response.getStatusText()).thenReturn("Forbidden");
            when(response.getBody()).thenReturn(body);
            when(body.toString()).thenReturn("{\"error\": \"Delete permission not granted\"}");
            when(request.getHttpMethod()).thenReturn(HttpMethod.DELETE);
            when(request.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-123");

            RequestException exception = assertThrows(RequestException.class,
                    () -> interceptor.onResponse(response, request, config));

            assertTrue(exception.getMessage().contains("403"));
        }
    }
}
