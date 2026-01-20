package com.deere.isg.examples;

import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationTest {

    private Application application;
    private MockWebServer mockWebServer;
    private Settings settings;

    @BeforeEach
    void setUp() throws Exception {
        application = new Application();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        settings = new Settings();
        setPrivateField(application, "settings", settings);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
        Unirest.shutDown();
    }

    @Test
    void testIndex() throws Exception {
        Context mockContext = mock(Context.class);
        
        application.index(mockContext);
        
        verify(mockContext).render(eq("main.mustache"), anyMap());
    }

    @Test
    void testGetRedirectUrl() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", baseUrl + "authorize");
        wellKnownResponse.put("token_endpoint", baseUrl + "token");
        mockWebServer.enqueue(new MockResponse()
                .setBody(wellKnownResponse.toString())
                .addHeader("Content-Type", "application/json"));

        settings.wellKnown = baseUrl + ".well-known/oauth-authorization-server";
        settings.clientId = "test-client";
        settings.scopes = "openid profile";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.state = "test-state";

        Method getRedirectUrl = Application.class.getDeclaredMethod("getRedirectUrl");
        getRedirectUrl.setAccessible(true);
        String redirectUrl = (String) getRedirectUrl.invoke(application);

        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("authorize"));
        assertTrue(redirectUrl.contains("client_id=test-client"));
        assertTrue(redirectUrl.contains("response_type=code"));
        assertTrue(redirectUrl.contains("state=test-state"));
    }

    @Test
    void testProcessCallbackWithError() throws Exception {
        Context mockContext = mock(Context.class);
        when(mockContext.queryParam("error")).thenReturn("access_denied");
        when(mockContext.queryParam("error_description")).thenReturn("User denied access");

        Method processCallback = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallback.setAccessible(true);
        processCallback.invoke(application, mockContext);

        verify(mockContext).render(eq("error.mustache"), anyMap());
    }

    @Test
    void testProcessCallbackSuccess() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", baseUrl + "authorize");
        wellKnownResponse.put("token_endpoint", baseUrl + "token");
        mockWebServer.enqueue(new MockResponse()
                .setBody(wellKnownResponse.toString())
                .addHeader("Content-Type", "application/json"));

        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("expires_in", 3600);
        mockWebServer.enqueue(new MockResponse()
                .setBody(tokenResponse.toString())
                .addHeader("Content-Type", "application/json"));

        JSONObject orgResponse = new JSONObject();
        orgResponse.put("values", new JSONArray());
        mockWebServer.enqueue(new MockResponse()
                .setBody(orgResponse.toString())
                .addHeader("Content-Type", "application/json"));

        settings.wellKnown = baseUrl + ".well-known/oauth-authorization-server";
        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid";
        settings.apiUrl = baseUrl + "platform";

        Context mockContext = mock(Context.class);
        when(mockContext.queryParam("error")).thenReturn(null);
        when(mockContext.queryParam("code")).thenReturn("auth-code-123");

        Method processCallback = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallback.setAccessible(true);
        processCallback.invoke(application, mockContext);

        assertEquals("test-access-token", settings.accessToken);
        assertEquals("test-refresh-token", settings.refreshToken);
    }

    @Test
    void testRefreshAccessToken() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", baseUrl + "authorize");
        wellKnownResponse.put("token_endpoint", baseUrl + "token");
        mockWebServer.enqueue(new MockResponse()
                .setBody(wellKnownResponse.toString())
                .addHeader("Content-Type", "application/json"));

        JSONObject refreshResponse = new JSONObject();
        refreshResponse.put("access_token", "new-access-token");
        refreshResponse.put("refresh_token", "new-refresh-token");
        refreshResponse.put("expires_in", 7200);
        mockWebServer.enqueue(new MockResponse()
                .setBody(refreshResponse.toString())
                .addHeader("Content-Type", "application/json"));

        settings.wellKnown = baseUrl + ".well-known/oauth-authorization-server";
        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.refreshToken = "old-refresh-token";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid";

        Context mockContext = mock(Context.class);

        Method refreshAccessToken = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshAccessToken.setAccessible(true);
        refreshAccessToken.invoke(application, mockContext);

        assertEquals("new-access-token", settings.accessToken);
        assertEquals("new-refresh-token", settings.refreshToken);
    }

    @Test
    void testRefreshAccessTokenError() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", baseUrl + "authorize");
        wellKnownResponse.put("token_endpoint", baseUrl + "token");
        mockWebServer.enqueue(new MockResponse()
                .setBody(wellKnownResponse.toString())
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\":\"invalid_grant\"}")
                .addHeader("Content-Type", "application/json"));

        settings.wellKnown = baseUrl + ".well-known/oauth-authorization-server";
        settings.clientId = "test-client";
        settings.clientSecret = "test-secret";
        settings.refreshToken = "invalid-refresh-token";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid";

        Unirest.config().interceptor(new LoggingInterceptor());

        Context mockContext = mock(Context.class);

        Method refreshAccessToken = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshAccessToken.setAccessible(true);
        refreshAccessToken.invoke(application, mockContext);

        verify(mockContext).render(eq("error.mustache"), anyMap());
    }

    @Test
    void testNeedsOrganizationAccessNoConnections() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject org = new JSONObject();
        JSONArray links = new JSONArray();
        JSONObject selfLink = new JSONObject();
        selfLink.put("rel", "self");
        selfLink.put("uri", baseUrl + "org/1");
        links.put(selfLink);
        org.put("links", links);
        
        JSONObject orgResponse = new JSONObject();
        JSONArray values = new JSONArray();
        values.put(org);
        orgResponse.put("values", values);
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(orgResponse.toString())
                .addHeader("Content-Type", "application/json"));

        settings.accessToken = "test-token";
        settings.apiUrl = baseUrl + "platform";

        Api mockApi = mock(Api.class);
        when(mockApi.get(anyString(), anyString())).thenReturn(orgResponse);
        setPrivateField(application, "api", mockApi);

        Method needsOrganizationAccess = Application.class.getDeclaredMethod("needsOrganizationAccess");
        needsOrganizationAccess.setAccessible(true);
        String result = (String) needsOrganizationAccess.invoke(application);

        assertNull(result);
    }

    @Test
    void testNeedsOrganizationAccessWithConnections() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject org = new JSONObject();
        JSONArray links = new JSONArray();
        JSONObject connectionsLink = new JSONObject();
        connectionsLink.put("rel", "connections");
        connectionsLink.put("uri", baseUrl + "connections/setup");
        links.put(connectionsLink);
        org.put("links", links);
        
        JSONObject orgResponse = new JSONObject();
        JSONArray values = new JSONArray();
        values.put(org);
        orgResponse.put("values", values);

        settings.accessToken = "test-token";
        settings.apiUrl = baseUrl + "platform";

        Api mockApi = mock(Api.class);
        when(mockApi.get(anyString(), anyString())).thenReturn(orgResponse);
        setPrivateField(application, "api", mockApi);

        Method needsOrganizationAccess = Application.class.getDeclaredMethod("needsOrganizationAccess");
        needsOrganizationAccess.setAccessible(true);
        String result = (String) needsOrganizationAccess.invoke(application);

        assertNotNull(result);
        assertTrue(result.contains("connections/setup"));
        assertTrue(result.contains("redirect_uri"));
    }

    @Test
    void testCallTheApi() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject apiResponse = new JSONObject();
        apiResponse.put("data", "test-data");

        settings.accessToken = "test-token";

        Api mockApi = mock(Api.class);
        when(mockApi.get(eq("test-token"), anyString())).thenReturn(apiResponse);
        setPrivateField(application, "api", mockApi);

        Context mockContext = mock(Context.class);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockContext.req()).thenReturn(mockRequest);
        when(mockRequest.getParameter("url")).thenReturn(baseUrl + "organizations");

        Method callTheApi = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApi.setAccessible(true);
        callTheApi.invoke(application, mockContext);

        assertNotNull(settings.apiResponse);
        assertTrue(settings.apiResponse.contains("test-data"));
    }

    @Test
    void testCallTheApiError() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();

        settings.accessToken = "test-token";

        Api mockApi = mock(Api.class);
        when(mockApi.get(anyString(), anyString())).thenThrow(new RuntimeException("API Error"));
        setPrivateField(application, "api", mockApi);

        Context mockContext = mock(Context.class);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockContext.req()).thenReturn(mockRequest);
        when(mockRequest.getParameter("url")).thenReturn(baseUrl + "organizations");

        Method callTheApi = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApi.setAccessible(true);
        callTheApi.invoke(application, mockContext);

        verify(mockContext).render(eq("error.mustache"), anyMap());
    }

    @Test
    void testStartOIDC() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", baseUrl + "authorize");
        wellKnownResponse.put("token_endpoint", baseUrl + "token");
        mockWebServer.enqueue(new MockResponse()
                .setBody(wellKnownResponse.toString())
                .addHeader("Content-Type", "application/json"));

        settings.wellKnown = baseUrl + ".well-known/oauth-authorization-server";

        Context mockContext = mock(Context.class);
        when(mockContext.formParam("clientId")).thenReturn("test-client");
        when(mockContext.formParam("clientSecret")).thenReturn("test-secret");
        when(mockContext.formParam("wellKnown")).thenReturn(settings.wellKnown);
        when(mockContext.formParam("callbackUrl")).thenReturn("http://localhost:9090/callback");
        when(mockContext.formParam("scopes")).thenReturn("openid");
        when(mockContext.formParam("state")).thenReturn("test-state");

        Method startOIDC = Application.class.getDeclaredMethod("startOIDC", Context.class);
        startOIDC.setAccessible(true);
        startOIDC.invoke(application, mockContext);

        verify(mockContext).redirect(contains("authorize"));
    }

    @Test
    void testRenderError() throws Exception {
        Context mockContext = mock(Context.class);

        Method renderError = Application.class.getDeclaredMethod("renderError", Context.class, String.class);
        renderError.setAccessible(true);
        renderError.invoke(application, mockContext, "Test error message");

        verify(mockContext).render(eq("error.mustache"), anyMap());
    }

    @Test
    void testGetLocationFromMeta() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", baseUrl + "authorize");
        wellKnownResponse.put("token_endpoint", baseUrl + "token");
        mockWebServer.enqueue(new MockResponse()
                .setBody(wellKnownResponse.toString())
                .addHeader("Content-Type", "application/json"));

        settings.wellKnown = baseUrl + ".well-known/oauth-authorization-server";

        Method getLocationFromMeta = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMeta.setAccessible(true);
        String authEndpoint = (String) getLocationFromMeta.invoke(application, "authorization_endpoint");

        assertEquals(baseUrl + "authorize", authEndpoint);
    }

    @Test
    void testGetLocationFromMetaCached() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        
        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", baseUrl + "authorize");
        wellKnownResponse.put("token_endpoint", baseUrl + "token");
        mockWebServer.enqueue(new MockResponse()
                .setBody(wellKnownResponse.toString())
                .addHeader("Content-Type", "application/json"));

        settings.wellKnown = baseUrl + ".well-known/oauth-authorization-server";

        Method getLocationFromMeta = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMeta.setAccessible(true);
        
        String authEndpoint1 = (String) getLocationFromMeta.invoke(application, "authorization_endpoint");
        String authEndpoint2 = (String) getLocationFromMeta.invoke(application, "authorization_endpoint");

        assertEquals(authEndpoint1, authEndpoint2);
        assertEquals(1, mockWebServer.getRequestCount());
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
