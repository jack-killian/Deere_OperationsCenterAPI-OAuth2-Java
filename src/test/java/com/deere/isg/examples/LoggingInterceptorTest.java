package com.deere.isg.examples;

import kong.unirest.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;
    private Config mockConfig;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
        mockConfig = mock(Config.class);
    }

    @Test
    void testOnResponseSuccess() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/test");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseSuccessWithPostMethod() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/token");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseFailure() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"invalid_token\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/protected");

        assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseFailureWith400() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusText()).thenReturn("Bad Request");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"invalid_request\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/token");

        assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseFailureWith500() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"server_error\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/data");

        assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseFailureWith404() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        JsonNode mockBody = mock(JsonNode.class);
        
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"not_found\"}");
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/missing");

        assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testPadMethodWithGet() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/test");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testPadMethodWithPost() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/test");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testPadMethodWithPut() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.PUT);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/test");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testPadMethodWithDelete() {
        HttpResponse<JsonNode> mockResponse = mock(HttpResponse.class);
        HttpRequestSummary mockRequest = mock(HttpRequestSummary.class);
        
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.DELETE);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/test");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }
}
