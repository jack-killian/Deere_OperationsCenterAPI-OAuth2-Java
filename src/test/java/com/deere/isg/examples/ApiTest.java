package com.deere.isg.examples;

import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONObject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {

    private Api api;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        api = new Api();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        Unirest.config().reset();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
        Unirest.shutDown();
    }

    @Test
    void testGetWithValidToken() throws Exception {
        JSONObject responseBody = new JSONObject();
        responseBody.put("id", "12345");
        responseBody.put("name", "Test Organization");
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody.toString())
                .addHeader("Content-Type", "application/json"));

        String url = mockWebServer.url("/organizations").toString();
        JSONObject result = api.get("test-access-token", url);

        assertNotNull(result);
        assertEquals("12345", result.getString("id"));
        assertEquals("Test Organization", result.getString("name"));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer test-access-token", request.getHeader("authorization"));
        assertEquals("application/vnd.deere.axiom.v3+json", request.getHeader("Accept"));
    }

    @Test
    void testGetWithDifferentAccessToken() throws Exception {
        JSONObject responseBody = new JSONObject();
        responseBody.put("data", "test");
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody.toString())
                .addHeader("Content-Type", "application/json"));

        String url = mockWebServer.url("/test").toString();
        api.get("different-token-123", url);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer different-token-123", request.getHeader("authorization"));
    }

    @Test
    void testGetReturnsJsonObject() throws Exception {
        JSONObject responseBody = new JSONObject();
        responseBody.put("values", new org.json.JSONArray());
        responseBody.put("total", 0);
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody.toString())
                .addHeader("Content-Type", "application/json"));

        String url = mockWebServer.url("/data").toString();
        JSONObject result = api.get("token", url);

        assertNotNull(result);
        assertTrue(result.has("values"));
        assertTrue(result.has("total"));
    }

    @Test
    void testGetWithNestedJsonResponse() throws Exception {
        JSONObject nested = new JSONObject();
        nested.put("field1", "value1");
        nested.put("field2", 123);
        
        JSONObject responseBody = new JSONObject();
        responseBody.put("nested", nested);
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody.toString())
                .addHeader("Content-Type", "application/json"));

        String url = mockWebServer.url("/nested").toString();
        JSONObject result = api.get("token", url);

        assertNotNull(result);
        assertTrue(result.has("nested"));
        JSONObject resultNested = result.getJSONObject("nested");
        assertEquals("value1", resultNested.getString("field1"));
        assertEquals(123, resultNested.getInt("field2"));
    }

    @Test
    void testGetUsesCorrectHttpMethod() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        String url = mockWebServer.url("/test").toString();
        api.get("token", url);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
    }

    @Test
    void testGetWithEmptyResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        String url = mockWebServer.url("/empty").toString();
        JSONObject result = api.get("token", url);

        assertNotNull(result);
        assertEquals(0, result.length());
    }
}
