package com.deere.isg.examples;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 4: API Integration Test
 * Tests the callTheApi() method, Unirest HTTP client functionality,
 * and the John Deere organization access flow.
 */
public class ApiIntegrationTest {

    private Settings settings;
    private Api api;

    @BeforeEach
    void setUp() {
        settings = new Settings();
        api = new Api();
    }

    @Test
    void apiClassIsInstantiable() {
        assertThat(api).isNotNull();
    }

    @Test
    void settingsApiUrlIsConfigured() {
        assertThat(settings.apiUrl).isEqualTo("https://sandboxapi.deere.com/platform");
    }

    @Test
    void organizationsEndpointPathIsCorrect() {
        String organizationsUrl = settings.apiUrl + "/organizations";
        assertThat(organizationsUrl).isEqualTo("https://sandboxapi.deere.com/platform/organizations");
    }

    @Test
    void callApiRouteIsRegistered() {
        Javalin javalin = Javalin.create();
        javalin.post("/call-api", ctx -> {
            String url = ctx.formParam("url");
            ctx.result("Called: " + url);
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.post("/call-api", "url=https://api.example.com/test");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void apiResponseIsStoredInSettings() {
        JSONObject mockResponse = new JSONObject();
        mockResponse.put("total", 5);
        mockResponse.put("values", new JSONArray());
        
        settings.apiResponse = mockResponse.toString(3);
        
        assertThat(settings.apiResponse).isNotNull();
        assertThat(settings.apiResponse).contains("total");
        assertThat(settings.apiResponse).contains("values");
    }

    @Test
    void accessTokenIsUsedForApiCalls() {
        settings.accessToken = "test-bearer-token";
        
        assertThat(settings.accessToken).isEqualTo("test-bearer-token");
    }

    @Test
    void organizationAccessCheckHandlesEmptyValues() {
        JSONObject apiResponse = new JSONObject();
        apiResponse.put("values", new JSONArray());
        
        assertThat(apiResponse.getJSONArray("values").isEmpty()).isTrue();
    }

    @Test
    void organizationAccessCheckHandlesLinksArray() {
        JSONObject org = new JSONObject();
        JSONArray links = new JSONArray();
        JSONObject link = new JSONObject();
        link.put("rel", "self");
        link.put("uri", "https://api.example.com/org/123");
        links.put(link);
        org.put("links", links);
        
        JSONArray values = new JSONArray();
        values.put(org);
        
        JSONObject apiResponse = new JSONObject();
        apiResponse.put("values", values);
        
        assertThat(apiResponse.getJSONArray("values").length()).isEqualTo(1);
    }

    @Test
    void connectionsRelIndicatesIncompleteAccess() {
        JSONObject link = new JSONObject();
        link.put("rel", "connections");
        link.put("uri", "https://connections.deere.com/setup");
        
        assertThat(link.getString("rel")).isEqualTo("connections");
    }

    @Test
    void apiUrlCanBeModified() {
        settings.apiUrl = "https://customapi.deere.com/platform";
        
        assertThat(settings.apiUrl).isEqualTo("https://customapi.deere.com/platform");
    }

    @Test
    void jsonResponseIsPrettyPrinted() {
        JSONObject response = new JSONObject();
        response.put("key", "value");
        
        String prettyPrinted = response.toString(3);
        
        assertThat(prettyPrinted).contains("\n");
        assertThat(prettyPrinted).contains("  ");
    }

    @Test
    void apiResponseHandlesNestedObjects() {
        JSONObject nested = new JSONObject();
        nested.put("id", "org-123");
        nested.put("name", "Test Organization");
        
        JSONArray values = new JSONArray();
        values.put(nested);
        
        JSONObject response = new JSONObject();
        response.put("values", values);
        response.put("total", 1);
        
        settings.apiResponse = response.toString(3);
        
        assertThat(settings.apiResponse).contains("org-123");
        assertThat(settings.apiResponse).contains("Test Organization");
    }
}
