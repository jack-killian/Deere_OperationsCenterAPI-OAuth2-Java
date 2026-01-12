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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApplicationRouteTest {

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
    void testIndexRouteRendersMainTemplate() throws Exception {
        Method index = Application.class.getDeclaredMethod("index", Context.class);
        index.setAccessible(true);
        index.invoke(application, mockContext);

        verify(mockContext).render(eq("main.mustache"), any());
    }

    @Test
    void testStartOIDCRouteRedirectsToAuthServer() throws Exception {
        Settings settings = getSettings();
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";

        when(mockContext.formParam("clientId")).thenReturn("test-client-id");
        when(mockContext.formParam("clientSecret")).thenReturn("test-secret");
        when(mockContext.formParam("wellKnown")).thenReturn(settings.wellKnown);
        when(mockContext.formParam("callbackUrl")).thenReturn("http://localhost:9090/callback");
        when(mockContext.formParam("scopes")).thenReturn("openid profile");
        when(mockContext.formParam("state")).thenReturn("test-state");

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.wellKnown)
                .thenReturn(wellKnownResponse.toString());

        Method startOIDC = Application.class.getDeclaredMethod("startOIDC", Context.class);
        startOIDC.setAccessible(true);
        startOIDC.invoke(application, mockContext);

        verify(mockContext).redirect(argThat(url -> 
            url.contains("https://test.auth.server/authorize") &&
            url.contains("client_id=test-client-id")
        ));
    }

    @Test
    void testCallbackRouteWithErrorParam() throws Exception {
        when(mockContext.queryParam("error")).thenReturn("access_denied");
        when(mockContext.queryParam("error_description")).thenReturn("User denied access");

        Method processCallback = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallback.setAccessible(true);
        processCallback.invoke(application, mockContext);

        verify(mockContext).render(eq("error.mustache"), any());
    }

    @Test
    void testCallbackRouteWithValidCode() throws Exception {
        Settings settings = getSettings();
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-secret";
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        when(mockContext.queryParam("error")).thenReturn(null);
        when(mockContext.queryParam("code")).thenReturn("auth-code-123");

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token-123");
        tokenResponse.put("refresh_token", "refresh-token-123");
        tokenResponse.put("id_token", "id-token-123");
        tokenResponse.put("expires_in", 3600);

        JSONObject orgResponse = new JSONObject();
        JSONArray values = new JSONArray();
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

        assertEquals("access-token-123", settings.accessToken);
        verify(mockContext).render(eq("main.mustache"), any());
    }

    @Test
    void testRefreshAccessTokenRoute() throws Exception {
        Settings settings = getSettings();
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-secret";
        settings.refreshToken = "old-refresh-token";
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";

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
        verify(mockContext).render(eq("main.mustache"), any());
    }

    @Test
    void testCallApiRoute() throws Exception {
        Settings settings = getSettings();
        settings.accessToken = "test-access-token";

        JSONObject apiResponse = new JSONObject();
        apiResponse.put("result", "success");

        setApi(mockApi);
        when(mockApi.get(anyString(), anyString())).thenReturn(apiResponse);

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockContext.req()).thenReturn(mockRequest);
        when(mockRequest.getParameter("url")).thenReturn("https://api.deere.com/test-endpoint");

        Method callTheApi = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApi.setAccessible(true);
        callTheApi.invoke(application, mockContext);

        assertNotNull(settings.apiResponse);
        assertTrue(settings.apiResponse.contains("success"));
        verify(mockContext).render(eq("main.mustache"), any());
    }

    @Test
    void testCallApiRouteWithException() throws Exception {
        Settings settings = getSettings();
        settings.accessToken = "test-access-token";

        setApi(mockApi);
        when(mockApi.get(anyString(), anyString())).thenThrow(new RuntimeException("API Error"));

        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockContext.req()).thenReturn(mockRequest);
        when(mockRequest.getParameter("url")).thenReturn("https://api.deere.com/test-endpoint");

        Method callTheApi = Application.class.getDeclaredMethod("callTheApi", Context.class);
        callTheApi.setAccessible(true);
        callTheApi.invoke(application, mockContext);

        verify(mockContext).render(eq("error.mustache"), any());
    }

    @Test
    void testCallbackWithOrganizationAccessNeeded() throws Exception {
        Settings settings = getSettings();
        settings.clientId = "test-client-id";
        settings.clientSecret = "test-secret";
        settings.wellKnown = "https://test.auth.server/.well-known/oauth-authorization-server";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.scopes = "openid profile";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        when(mockContext.queryParam("error")).thenReturn(null);
        when(mockContext.queryParam("code")).thenReturn("auth-code-123");

        JSONObject wellKnownResponse = new JSONObject();
        wellKnownResponse.put("authorization_endpoint", "https://test.auth.server/authorize");
        wellKnownResponse.put("token_endpoint", "https://test.auth.server/token");

        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "access-token-123");
        tokenResponse.put("refresh_token", "refresh-token-123");
        tokenResponse.put("expires_in", 3600);

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

        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.wellKnown)
                .thenReturn(wellKnownResponse.toString());
        mockClient.expect(kong.unirest.core.HttpMethod.POST, "https://test.auth.server/token")
                .thenReturn(tokenResponse.toString());
        mockClient.expect(kong.unirest.core.HttpMethod.GET, settings.apiUrl + "/organizations")
                .thenReturn(orgResponse.toString());

        Method processCallback = Application.class.getDeclaredMethod("processCallback", Context.class);
        processCallback.setAccessible(true);
        processCallback.invoke(application, mockContext);

        verify(mockContext).redirect(argThat(url -> url.contains("https://connections.deere.com/setup")));
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
