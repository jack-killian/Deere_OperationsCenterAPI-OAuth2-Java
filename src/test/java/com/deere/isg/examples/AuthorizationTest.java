package com.deere.isg.examples;

import kong.unirest.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Test
    void shouldRejectUnauthorizedAccessToPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Authentication required to access private transaction\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-123");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    void shouldRejectCommentOnPrivateTransactionWithoutPermission() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"You do not have permission to comment on this private transaction\", \"transactionId\": \"tx-456\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-456/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Forbidden"));
        assertTrue(exception.getMessage().contains("permission"));
    }

    @Test
    void shouldRejectAccessToTransactionFromDifferentOrganization() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Transaction belongs to a different organization\", \"organizationId\": \"org-789\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/organizations/org-789/transactions/tx-123");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("organization"));
    }

    @Test
    void shouldRejectExpiredTokenAccessToPrivateResource() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Access token has expired\", \"error_description\": \"The access token expired\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/private/data");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    void shouldRejectInvalidTokenForPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"The access token is invalid\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-789/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    void shouldRejectInsufficientScopesForPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"insufficient_scope\", \"error_description\": \"The request requires higher privileges than provided by the access token\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-private/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("scope"));
    }

    @Test
    void shouldRejectReadOnlyAccessAttemptingToComment() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Read-only access does not permit commenting on transactions\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-readonly/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Read-only"));
    }

    @Test
    void shouldRejectCommentOnArchivedPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.getStatusText()).thenReturn("Forbidden");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Cannot comment on archived transaction\", \"transactionStatus\": \"archived\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/archived-tx/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("archived"));
    }

    @Test
    void shouldAllowAuthorizedAccessToPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx-authorized");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void shouldAllowAuthorizedCommentOnPrivateTransaction() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/private-tx/comments");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void shouldRejectMissingAuthorizationHeader() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"Missing Authorization header\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/private/transactions");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Authorization"));
    }

    @Test
    void shouldRejectMalformedBearerToken() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\": \"invalid_token\", \"error_description\": \"Malformed bearer token\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://sandboxapi.deere.com/platform/transactions/tx-123/comments");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Malformed"));
    }
}
