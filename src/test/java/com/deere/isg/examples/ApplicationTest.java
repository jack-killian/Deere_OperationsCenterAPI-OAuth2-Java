package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationTest {

    private Application application;

    @Mock
    private Context mockContext;

    @Mock
    private Api mockApi;

    private Settings settings;

    @BeforeEach
    void setUp() throws Exception {
        application = new Application();
        settings = new Settings();
        setPrivateField(application, "settings", settings);
        setPrivateField(application, "api", mockApi);
    }

    @Test
    void shouldBuildRedirectUrlWithAllParameters() throws Exception {
        settings.clientId = "test-client-id";
        settings.scopes = "openid profile";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.state = "test-state-123";
        settings.wellKnown = "https://example.com/.well-known";

        JSONObject metaData = new JSONObject();
        metaData.put("authorization_endpoint", "https://auth.example.com/authorize");
        metaData.put("token_endpoint", "https://auth.example.com/token");
        
        Map<String, JSONObject> metaInfo = new HashMap<>();
        metaInfo.put(settings.wellKnown, metaData);
        setPrivateField(application, "metaInfo", metaInfo);

        String redirectUrl = invokePrivateMethod(application, "getRedirectUrl");

        assertThat(redirectUrl).contains("https://auth.example.com/authorize");
        assertThat(redirectUrl).contains("client_id=test-client-id");
        assertThat(redirectUrl).contains("response_type=code");
        assertThat(redirectUrl).contains("scope=openid");
        assertThat(redirectUrl).contains("redirect_uri=http");
        assertThat(redirectUrl).contains("state=test-state-123");
    }

    @Test
    void shouldEncodeSpecialCharactersInRedirectUrl() throws Exception {
        settings.clientId = "client with spaces";
        settings.scopes = "openid profile offline_access";
        settings.callbackUrl = "http://localhost:9090/callback";
        settings.state = "state-123";
        settings.wellKnown = "https://example.com/.well-known";

        JSONObject metaData = new JSONObject();
        metaData.put("authorization_endpoint", "https://auth.example.com/authorize");
        metaData.put("token_endpoint", "https://auth.example.com/token");
        
        Map<String, JSONObject> metaInfo = new HashMap<>();
        metaInfo.put(settings.wellKnown, metaData);
        setPrivateField(application, "metaInfo", metaInfo);

        String redirectUrl = invokePrivateMethod(application, "getRedirectUrl");

        assertThat(redirectUrl).contains("client_id=client");
        assertThat(redirectUrl).contains("scope=openid");
    }

    @Test
    void shouldReturnNullWhenNoOrganizationNeedsAccess() throws Exception {
        settings.accessToken = "test-access-token";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        JSONObject org1 = new JSONObject();
        JSONArray links1 = new JSONArray();
        JSONObject selfLink = new JSONObject();
        selfLink.put("rel", "self");
        selfLink.put("uri", "https://api.example.com/org/1");
        links1.put(selfLink);
        org1.put("links", links1);

        JSONArray values = new JSONArray();
        values.put(org1);

        JSONObject apiResponse = new JSONObject();
        apiResponse.put("values", values);

        when(mockApi.get(settings.accessToken, settings.apiUrl + "/organizations")).thenReturn(apiResponse);

        String result = invokePrivateMethod(application, "needsOrganizationAccess");

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnRedirectUrlWhenOrganizationNeedsAccess() throws Exception {
        settings.accessToken = "test-access-token";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        JSONObject org1 = new JSONObject();
        JSONArray links1 = new JSONArray();
        JSONObject connectionsLink = new JSONObject();
        connectionsLink.put("rel", "connections");
        connectionsLink.put("uri", "https://connections.deere.com/setup");
        links1.put(connectionsLink);
        org1.put("links", links1);

        JSONArray values = new JSONArray();
        values.put(org1);

        JSONObject apiResponse = new JSONObject();
        apiResponse.put("values", values);

        when(mockApi.get(settings.accessToken, settings.apiUrl + "/organizations")).thenReturn(apiResponse);

        String result = invokePrivateMethod(application, "needsOrganizationAccess");

        assertThat(result).isNotNull();
        assertThat(result).contains("https://connections.deere.com/setup");
        assertThat(result).contains("redirect_uri=");
        assertThat(result).contains("localhost");
        assertThat(result).contains("9090");
    }

    @Test
    void shouldReturnNullWhenOrganizationsListIsEmpty() throws Exception {
        settings.accessToken = "test-access-token";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        JSONArray values = new JSONArray();
        JSONObject apiResponse = new JSONObject();
        apiResponse.put("values", values);

        when(mockApi.get(settings.accessToken, settings.apiUrl + "/organizations")).thenReturn(apiResponse);

        String result = invokePrivateMethod(application, "needsOrganizationAccess");

        assertThat(result).isNull();
    }

    @Test
    void shouldCheckMultipleOrganizationsForConnectionsRel() throws Exception {
        settings.accessToken = "test-access-token";
        settings.apiUrl = "https://sandboxapi.deere.com/platform";

        JSONObject org1 = new JSONObject();
        JSONArray links1 = new JSONArray();
        JSONObject selfLink1 = new JSONObject();
        selfLink1.put("rel", "self");
        selfLink1.put("uri", "https://api.example.com/org/1");
        links1.put(selfLink1);
        org1.put("links", links1);

        JSONObject org2 = new JSONObject();
        JSONArray links2 = new JSONArray();
        JSONObject connectionsLink = new JSONObject();
        connectionsLink.put("rel", "connections");
        connectionsLink.put("uri", "https://connections.deere.com/setup/org2");
        links2.put(connectionsLink);
        org2.put("links", links2);

        JSONArray values = new JSONArray();
        values.put(org1);
        values.put(org2);

        JSONObject apiResponse = new JSONObject();
        apiResponse.put("values", values);

        when(mockApi.get(settings.accessToken, settings.apiUrl + "/organizations")).thenReturn(apiResponse);

        String result = invokePrivateMethod(application, "needsOrganizationAccess");

        assertThat(result).isNotNull();
        assertThat(result).contains("https://connections.deere.com/setup/org2");
    }

    @Test
    void shouldGetLocationFromMetaAndCacheIt() throws Exception {
        settings.wellKnown = "https://example.com/.well-known";

        JSONObject metaData = new JSONObject();
        metaData.put("authorization_endpoint", "https://auth.example.com/authorize");
        metaData.put("token_endpoint", "https://auth.example.com/token");
        
        Map<String, JSONObject> metaInfo = new HashMap<>();
        metaInfo.put(settings.wellKnown, metaData);
        setPrivateField(application, "metaInfo", metaInfo);

        Method method = Application.class.getDeclaredMethod("getLocationFromMeta", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(application, "authorization_endpoint");

        assertThat(result).isEqualTo("https://auth.example.com/authorize");
    }

    @Test
    void shouldHaveDefaultSettingsInstance() throws Exception {
        Application newApp = new Application();
        Field settingsField = Application.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        Settings appSettings = (Settings) settingsField.get(newApp);

        assertThat(appSettings).isNotNull();
    }

    @Test
    void shouldHaveDefaultApiInstance() throws Exception {
        Application newApp = new Application();
        Field apiField = Application.class.getDeclaredField("api");
        apiField.setAccessible(true);
        Api appApi = (Api) apiField.get(newApp);

        assertThat(appApi).isNotNull();
    }

    @Test
    void shouldHaveEmptyMetaInfoMapInitially() throws Exception {
        Application newApp = new Application();
        Field metaInfoField = Application.class.getDeclaredField("metaInfo");
        metaInfoField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JSONObject> metaInfo = (Map<String, JSONObject>) metaInfoField.get(newApp);

        assertThat(metaInfo).isNotNull();
        assertThat(metaInfo).isEmpty();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (T) method.invoke(target);
    }
}
