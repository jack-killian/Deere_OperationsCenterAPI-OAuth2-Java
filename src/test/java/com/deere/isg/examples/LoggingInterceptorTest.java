package com.deere.isg.examples;

import kong.unirest.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;

    @Mock
    private HttpResponse<JsonNode> mockResponse;

    @Mock
    private HttpRequestSummary mockRequest;

    @Mock
    private Config mockConfig;

    @Mock
    private JsonNode mockJsonNode;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
    }

    @Test
    void testOnResponseSuccessfulCall() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/resource");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseSuccessfulPostCall() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/token");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseFailedCallThrowsRequestException() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/resource");
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("{\"error\":\"Not found\"}");

        RequestException exception = assertThrows(RequestException.class, 
            () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
    }

    @Test
    void testOnResponseFailedWith401() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/token");
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("{\"error\":\"Invalid credentials\"}");

        RequestException exception = assertThrows(RequestException.class, 
            () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    void testOnResponseFailedWith500() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/organizations");
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockJsonNode);
        when(mockJsonNode.toString()).thenReturn("{\"error\":\"Server error\"}");

        RequestException exception = assertThrows(RequestException.class, 
            () -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @Test
    void testOnResponseWithDifferentHttpMethods() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/resource");

        HttpMethod[] methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE};
        
        for (HttpMethod method : methods) {
            when(mockRequest.getHttpMethod()).thenReturn(method);
            assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
        }
    }
}
