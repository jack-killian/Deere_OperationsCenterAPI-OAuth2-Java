package com.deere.isg.examples;

import kong.unirest.core.Config;
import kong.unirest.core.HttpMethod;
import kong.unirest.core.HttpRequestSummary;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingInterceptorTest {

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

    @Test
    void onResponse_withSuccessfulResponse_doesNotThrowException() {
        when(response.isSuccess()).thenReturn(true);
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://api.example.com/resource");

        interceptor.onResponse(response, request, config);
    }

    @Test
    void onResponse_withNotFoundError_throwsRequestException() {
        when(response.isSuccess()).thenReturn(false);
        when(response.getStatus()).thenReturn(404);
        when(response.getStatusText()).thenReturn("Not Found");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Transaction not found\"}");
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://api.example.com/transactions/12345");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(response, request, config));

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
    }

    @Test
    void onResponse_withUnauthorizedError_throwsRequestException() {
        when(response.isSuccess()).thenReturn(false);
        when(response.getStatus()).thenReturn(401);
        when(response.getStatusText()).thenReturn("Unauthorized");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Invalid or expired token\"}");
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://api.example.com/private/resource");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(response, request, config));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    void onResponse_withForbiddenError_throwsRequestException() {
        when(response.isSuccess()).thenReturn(false);
        when(response.getStatus()).thenReturn(403);
        when(response.getStatusText()).thenReturn("Forbidden");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Access denied\"}");
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://api.example.com/private/transactions");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(response, request, config));

        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains("Forbidden"));
    }

    @Test
    void onResponse_withInternalServerError_throwsRequestException() {
        when(response.isSuccess()).thenReturn(false);
        when(response.getStatus()).thenReturn(500);
        when(response.getStatusText()).thenReturn("Internal Server Error");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Server error\"}");
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://api.example.com/resource");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(response, request, config));

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @Test
    void onResponse_withBadGatewayError_throwsRequestException() {
        when(response.isSuccess()).thenReturn(false);
        when(response.getStatus()).thenReturn(502);
        when(response.getStatusText()).thenReturn("Bad Gateway");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Bad gateway\"}");
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://api.example.com/resource");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(response, request, config));

        assertTrue(exception.getMessage().contains("502"));
        assertTrue(exception.getMessage().contains("Bad Gateway"));
    }

    @Test
    void onResponse_withServiceUnavailableError_throwsRequestException() {
        when(response.isSuccess()).thenReturn(false);
        when(response.getStatus()).thenReturn(503);
        when(response.getStatusText()).thenReturn("Service Unavailable");
        when(response.getBody()).thenReturn(body);
        when(body.toString()).thenReturn("{\"error\": \"Service unavailable\"}");
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(request.getUrl()).thenReturn("https://api.example.com/resource");

        RequestException exception = assertThrows(RequestException.class,
                () -> interceptor.onResponse(response, request, config));

        assertTrue(exception.getMessage().contains("503"));
        assertTrue(exception.getMessage().contains("Service Unavailable"));
    }
}
