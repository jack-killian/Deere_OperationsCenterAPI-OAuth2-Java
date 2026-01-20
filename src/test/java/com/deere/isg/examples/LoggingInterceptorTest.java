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
    private JsonNode mockBody;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
    }

    @Test
    void testOnResponseSuccessDoesNotThrow() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/test");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseSuccessWithPostMethod() {
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/token");

        assertDoesNotThrow(() -> interceptor.onResponse(mockResponse, mockRequest, mockConfig));
    }

    @Test
    void testOnResponseFailureThrowsRequestException() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/test");
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusText()).thenReturn("Unauthorized");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"invalid_token\"}");

        assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );
    }

    @Test
    void testOnResponseFailureWith404() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/notfound");
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.getStatusText()).thenReturn("Not Found");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"message\":\"Resource not found\"}");

        RequestException exception = assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Not Found"));
    }

    @Test
    void testOnResponseFailureWith500() {
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getUrl()).thenReturn("https://api.example.com/error");
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.getStatusText()).thenReturn("Internal Server Error");
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.toString()).thenReturn("{\"error\":\"server_error\"}");

        RequestException exception = assertThrows(RequestException.class, () -> 
            interceptor.onResponse(mockResponse, mockRequest, mockConfig)
        );

        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void testInterceptorImplementsInterface() {
        assertTrue(interceptor instanceof Interceptor);
    }
}
