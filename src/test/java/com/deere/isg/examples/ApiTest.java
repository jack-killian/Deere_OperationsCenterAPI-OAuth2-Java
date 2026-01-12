package com.deere.isg.examples;

import kong.unirest.core.MockClient;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {

    private Api api;
    private MockClient mockClient;

    @BeforeEach
    void setUp() {
        api = new Api();
        mockClient = MockClient.register();
    }

    @AfterEach
    void tearDown() {
        Unirest.shutDown();
    }

    @Test
    void testGetWithValidResponse() {
        String accessToken = "test-access-token";
        String resourceUrl = "https://sandboxapi.deere.com/platform/organizations";

        JSONObject expectedResponse = new JSONObject();
        expectedResponse.put("total", 5);
        expectedResponse.put("values", new JSONArray());

        mockClient.expect(kong.unirest.core.HttpMethod.GET, resourceUrl)
                .thenReturn(expectedResponse.toString());

        JSONObject result = api.get(accessToken, resourceUrl);

        assertNotNull(result);
        assertEquals(5, result.getInt("total"));
    }

    @Test
    void testGetWithDifferentEndpoints() {
        String accessToken = "test-access-token";

        String organizationsUrl = "https://sandboxapi.deere.com/platform/organizations";
        JSONObject orgResponse = new JSONObject();
        orgResponse.put("type", "organizations");

        String fieldsUrl = "https://sandboxapi.deere.com/platform/organizations/123/fields";
        JSONObject fieldsResponse = new JSONObject();
        fieldsResponse.put("type", "fields");

        mockClient.expect(kong.unirest.core.HttpMethod.GET, organizationsUrl)
                .thenReturn(orgResponse.toString());
        mockClient.expect(kong.unirest.core.HttpMethod.GET, fieldsUrl)
                .thenReturn(fieldsResponse.toString());

        JSONObject orgResult = api.get(accessToken, organizationsUrl);
        assertEquals("organizations", orgResult.getString("type"));

        JSONObject fieldsResult = api.get(accessToken, fieldsUrl);
        assertEquals("fields", fieldsResult.getString("type"));
    }

    @Test
    void testGetWithEmptyResponse() {
        String accessToken = "test-access-token";
        String resourceUrl = "https://sandboxapi.deere.com/platform/empty";

        JSONObject emptyResponse = new JSONObject();

        mockClient.expect(kong.unirest.core.HttpMethod.GET, resourceUrl)
                .thenReturn(emptyResponse.toString());

        JSONObject result = api.get(accessToken, resourceUrl);

        assertNotNull(result);
        assertEquals(0, result.length());
    }

    @Test
    void testGetWithComplexResponse() {
        String accessToken = "test-access-token";
        String resourceUrl = "https://sandboxapi.deere.com/platform/organizations/123";

        JSONObject complexResponse = new JSONObject();
        complexResponse.put("id", "123");
        complexResponse.put("name", "Test Organization");
        complexResponse.put("type", "customer");

        JSONArray links = new JSONArray();
        JSONObject selfLink = new JSONObject();
        selfLink.put("rel", "self");
        selfLink.put("uri", "https://sandboxapi.deere.com/platform/organizations/123");
        links.put(selfLink);
        complexResponse.put("links", links);

        mockClient.expect(kong.unirest.core.HttpMethod.GET, resourceUrl)
                .thenReturn(complexResponse.toString());

        JSONObject result = api.get(accessToken, resourceUrl);

        assertNotNull(result);
        assertEquals("123", result.getString("id"));
        assertEquals("Test Organization", result.getString("name"));
        assertEquals("customer", result.getString("type"));
        assertTrue(result.has("links"));
    }

    @Test
    void testGetWithDifferentAccessTokens() {
        String resourceUrl = "https://sandboxapi.deere.com/platform/test";

        JSONObject response = new JSONObject();
        response.put("status", "ok");

        mockClient.expect(kong.unirest.core.HttpMethod.GET, resourceUrl)
                .thenReturn(response.toString());

        JSONObject result1 = api.get("token1", resourceUrl);
        assertNotNull(result1);

        mockClient.expect(kong.unirest.core.HttpMethod.GET, resourceUrl)
                .thenReturn(response.toString());

        JSONObject result2 = api.get("token2", resourceUrl);
        assertNotNull(result2);
    }
}
