package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.MockClient;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApplicationTest {

    private Application application;
    private MockClient mockClient;

    @Mock
    private Context mockContext;

    @Mock
    private Api mockApi;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        application = new Application();
        mockClient = MockClient.register();
    }

    @AfterEach
    void tearDown() {
        Unirest.shutDown();
    }

    @Test
    void testGetRedirectUrl() throws Exception {
        Settings settings = getSettings();
        settings.clientId = "test-client-id";
        settings.scopes = "openid profile";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.state = "test-state";
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.wellKnown)
                .thenReturn(wellKnownResponse.toString());

        Method getRedirectUrl = Application.class.getDeclaredMethod("getRedirectUrl");
        getRedirectUrl.setAccessible(true);
        String redirectUrl = (String) getRedirectUrl.invoke(application);

        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("https://test.auth.server/authorize"));
        assertTrue(redirectUrl.contains("client_id=test-client-id"));
        assertTrue(redirectUrl.contains("response_type=code"));
        assertTrue(redirectUrl.contains("scope=openid+profile"));
        assertTrue(redirectUrl.contains("state=test-state"));
    }

    @Test
    void testProcessCallbackWithError() throws Exception {
        when(mockContext.queryParam("error")).thenReturn("access_denied");
        when(mockContext.queryParam("error_description")).thenReturn("User denied access");

        Method processCallback = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallback.setAccessible(true);
        processCallback.invoke(application, mockContext);

        verify(mockContext).render(eq("error.mustache"), any(Map.class));
    }

    @Test
    void testProcessCallbackSuccess() throws Exception {
        Settings settings = getSettings();
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-client-secret";
        settings.scopes = "openid profile";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        when(mockContext.queryParam("error")).thenReturn(null);
        when(mockContext.queryParam("code")).thenReturn("test-auth-code");

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("expires_in", 3600);

        JSONObject orgResponse = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject org = new JSONObject();
        JSONArray links = new JSONArray();
        JSONObject link = new JSONObject();
        link.put("rel", "self");
        link.put("uri", "https://api.deere.com/organizations/123");
        links.put(link);
        org.put("links", links);
        values.put(org);
        orgResponse.put("values", values);

        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.wellKnown)
                .thenReturn(wellKnownResponse.toString());
        mockClient.expect(kong.unirest.core.HttpMethod.POST, "https://test.auth.server/token")
                .thenReturn(tokenResponse.toString());
        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.apiUrl + "/organizations")
                .thenReturn(orgResponse.toString());

        Method processCallback = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallback.setAccessible(true);
        processCallback.invoke(application, mockContext);

        assertEquals("test-access-token", settings.accessToken);
        assertEquals("test-refresh-token", settings.refreshToken);
    }

    @Test
    void testRefreshAccessToken() throws Exception {
        Settings settings = getSettings();
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-client-secret";
        settings.refreshToken = "test-refresh-token";
        settings.scopes = "openid profile";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        JSONObject refreshResponse = new JSONObject();
        refreshResponse.put("access_token", "new-access-token");
        refreshResponse.put("refresh_token", "new-refresh-token");
        refreshResponse.put("expires_in", 3600);

        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.wellKnown)
                .thenReturn(wellKnownResponse.toString());
        mockClient.expect(kong.unirest.core.HttpMethod.POST, "https://test.auth.server/token")
                .thenReturn(refreshResponse.toString());

        Method refreshAccessToken = Application.class.getDeclaredMethod("refreshAccessToken", Context.class);
        refreshAccessToken.setAccessible(true);
        refreshAccessToken.invoke(application, mockContext);

        assertEquals("new-access-token", settings.accessToken);
        assertEquals("new-refresh-token", settings.refreshToken);
    }

    @Test
    void testIndex() throws Exception {
        Method index = Application.class.getDeclaredMethod("index", Context.class);
        index.setAccessible(true);
        index.invoke(application, mockContext);

        verify(mockContext).render(eq("main.mustache"), any(Map.class));
    }

    @Test
    void testNeedsOrganizationAccessWithConnectionsRel() throws Exception {
        Settings settings = getSettings();
        settings.accessToken = "test-access-token";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        JSONObject orgResponse = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject org = new JSONObject();
        JSONArray links = new JSONArray();
        JSONObject connectionLink = new JSONObject();
        connectionLink.put("rel", "connections");
        connectionLink.put("uri", "https://connections.deere.com/setup");
        links.put(connectionLink);
        org.put("links", links);
        values.put(org);
        orgResponse.put("values", values);

        setApi(mockApi);
        when(mockApi.get(anyString(), anyString())).thenReturn(orgResponse);

        Method needsOrganizationAccess = Application.class.getDeclaredMethod("needsOrganizationAccess");
        needsOrganizationAccess.setAccessible(true);
        String result = (String) needsOrganizationAccess.invoke(application);

        assertNotNull(result);
        assertTrue(result.contains("https://connections.deere.com/setup"));
    }

    @Test
    void testNeedsOrganizationAccessWithoutConnectionsRel() throws Exception {
        Settings settings = getSettings();
        settings.accessToken = "test-access-token";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        JSONObject orgResponse = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject org = new JSONObject();
        JSONArray links = new JSONArray();
        JSONObject selfLink = new JSONObject();
        selfLink.put("rel", "self");
        selfLink.put("uri", "https://api.deere.com/organizations/123");
        links.put(selfLink);
        org.put("links", links);
        values.put(org);
        orgResponse.put("values", values);

        setApi(mockApi);
        when(mockApi.get(anyString(), anyString())).thenReturn(orgResponse);

        Method needsOrganizationAccess = Application.class.getDeclaredMethod("needsOrganizationAccess");
        needsOrganizationAccess.setAccessible(true);
        String result = (String) needsOrganizationAccess.invoke(application);

        assertNull(result);
    }

    @Test
    void testCallTheApi() throws Exception {
        Settings settings = getSettings();
        settings.accessToken = "test-access-token";

        JSONObject apiResponse = new JSONObject();
        apiResponse.put("data", "test-data");

        setApi(mockApi);
        when(mockApi.get(anyString(), anyString())).thenReturn(apiResponse);

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockContext.req()).thenReturn(mockRequest);
        when(mockRequest.getParameter("url")).thenReturn("https://api.deere.com/test");

        Method callTheApi = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApi.setAccessible(true);
        callTheApi.invoke(application, mockContext);

        assertNotNull(settings.apiResponse);
        assertTrue(settings.apiResponse.contains("test-data"));
    }

    @Test
    void testRenderError() throws Exception {
        Method renderError = Application.class.getDeclaredMethod("renderError", Context.class, String.class);
        renderError.setAccessible(true);
        renderError.invoke(application, mockContext, "Test error message");

        verify(mockContext).render(eq("error.mustache"), any(Map.class));
    }

    @Test
    void testGetLocationFromMeta() throws Exception {
        Settings settings = getSettings();
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.wellKnown)
                .thenReturn(wellKnownResponse.toString());

        Method getLocationFromMeta = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMeta.setAccessible(true);
        String tokenEndpoint = (String) getLocationFromMeta.invoke(application, "token_endpoint");

        assertEquals("https://test.auth.server/token", tokenEndpoint);
    }

    @Test
    void testMetaInfoCaching() throws Exception {
        Settings settings = getSettings();
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.wellKnown)
                .thenReturn(wellKnownResponse.toString());

        Method getLocationFromMeta = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        getLocationFromMeta.setAccessible(true);

        String tokenEndpoint1 = (String) getLocationFromMeta.invoke(application, "token_endpoint");
        String tokenEndpoint2 = (String) getLocationFromMeta.invoke(application, "token_endpoint");

        assertEquals(tokenEndpoint1, tokenEndpoint2);
    }

    private Settings getSettings() throws Exception {
        Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        return (Settings) settingsField.get(application);
    }

    private void setApi(Api api) throws Exception {
        Field apiField = Application.class.getDeclaredField("api");
        apiField.setAccessible(true);
        apiField.set(application, api);
    }
}
