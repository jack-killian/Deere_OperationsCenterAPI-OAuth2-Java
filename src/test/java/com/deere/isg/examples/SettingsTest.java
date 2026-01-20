package com.deere.isg.examples;

import io.javalin.http.Context;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsTest {

    private Settings settings;

    @Mock
    private Context mockContext;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    void shouldHaveDefaultValues() {
        assertThat(settings.clientId).isEmpty();
        assertThat(settings.clientSecret).isEmpty();
        assertThat(settings.wellKnown).isEqualTo("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/.well-known/oauth-authorization-server");
        assertThat(settings.apiUrl).isEqualTo("https://sandboxapi.deere.com/platform");
        assertThat(settings.callbackUrl).isEqualTo("http://localhost:9090/callback");
        assertThat(settings.scopes).isEqualTo("openid profile offline_access ag1 eq1");
        assertThat(settings.state).isNotNull().isNotEmpty();
    }

    @Test
    void shouldPopulateFromContext() {
        when(mockContext.formParam("clientId")).thenReturn("test-client-id");
        when(mockContext.formParam("clientSecret")).thenReturn("test-client-secret");
        when(mockContext.formParam("wellKnown")).thenReturn("https://example.com/.well-known");
        when(mockContext.formParam("callbackUrl")).thenReturn("http://localhost:8080/callback");
        when(mockContext.formParam("scopes")).thenReturn("openid profile");
        when(mockContext.formParam("state")).thenReturn("test-state-123");

        settings.populate(mockContext);

        assertThat(settings.clientId).isEqualTo("test-client-id");
        assertThat(settings.clientSecret).isEqualTo("test-client-secret");
        assertThat(settings.wellKnown).isEqualTo("https://example.com/.well-known");
        assertThat(settings.callbackUrl).isEqualTo("http://localhost:8080/callback");
        assertThat(settings.scopes).isEqualTo("openid profile");
        assertThat(settings.state).isEqualTo("test-state-123");
    }

    @Test
    void shouldGenerateBasicAuthHeader() {
        settings.clientId = "myClientId";
        settings.clientSecret = "myClientSecret";

        String authHeader = settings.getBasicAuthHeader();

        String expectedHeader = Base64.getEncoder().encodeToString("myClientId:myClientSecret".getBytes());
        assertThat(authHeader).isEqualTo(expectedHeader);
    }

    @Test
    void shouldGenerateBasicAuthHeaderWithSpecialCharacters() {
        settings.clientId = "client:with:colons";
        settings.clientSecret = "secret/with/slashes";

        String authHeader = settings.getBasicAuthHeader();

        String expectedHeader = Base64.getEncoder().encodeToString("client:with:colons:secret/with/slashes".getBytes());
        assertThat(authHeader).isEqualTo(expectedHeader);
    }

    @Test
    void shouldUpdateTokenInfoFromJsonObject() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        tokenResponse.put("expires_in", 3600L);

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.idToken).isEqualTo("test-id-token");
        assertThat(settings.accessToken).isEqualTo("test-access-token");
        assertThat(settings.refreshToken).isEqualTo("test-refresh-token");
        assertThat(settings.exp).isEqualTo(3600L);
    }

    @Test
    void shouldHandleMissingTokenFields() {
        JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("access_token", "only-access-token");

        settings.updateTokenInfo(tokenResponse);

        assertThat(settings.accessToken).isEqualTo("only-access-token");
        assertThat(settings.idToken).isEmpty();
        assertThat(settings.refreshToken).isEmpty();
        assertThat(settings.exp).isEqualTo(0L);
    }

    @Test
    void shouldReturnNullExpirationWhenExpIsNull() {
        settings.exp = null;

        String expiration = settings.getExpiration();

        assertThat(expiration).isNull();
    }

    @Test
    void shouldReturnExpirationDateTime() {
        settings.exp = 3600L;

        String expiration = settings.getExpiration();

        assertThat(expiration).isNotNull();
        assertThat(expiration).contains("T");
    }

    @Test
    void shouldReturnNullAccessTokenDetailsWhenTokenIsNull() {
        settings.accessToken = null;

        String details = settings.getAccessTokenDetails();

        assertThat(details).isNull();
    }

    @Test
    void shouldReturnNullAccessTokenDetailsWhenTokenIsEmpty() {
        settings.accessToken = "";

        String details = settings.getAccessTokenDetails();

        assertThat(details).isNull();
    }

    @Test
    void shouldDecodeAccessTokenDetails() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"user123\",\"exp\":1234567890}".getBytes());
        String signature = "signature";
        settings.accessToken = header + "." + payload + "." + signature;

        String details = settings.getAccessTokenDetails();

        assertThat(details).isNotNull();
        assertThat(details).contains("user123");
        assertThat(details).contains("1234567890");
    }

    @Test
    void shouldHaveServerUrlConstant() {
        assertThat(Settings.SERVER_URL).isEqualTo("http://localhost:9090");
    }

    @Test
    void shouldGenerateUniqueStateForEachInstance() {
        Settings settings1 = new Settings();
        Settings settings2 = new Settings();

        assertThat(settings1.state).isNotEqualTo(settings2.state);
    }
}
