package com.deere.isg.examples;

import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Settings")
class SettingsTest {

    private Settings settings;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("should have correct server URL")
        void shouldHaveCorrectServerUrl() {
            assertThat(Settings.SERVER_URL).isEqualTo("http://localhost:9090");
        }

        @Test
        @DisplayName("should have default well-known URL")
        void shouldHaveDefaultWellKnownUrl() {
            assertThat(settings.wellKnown)
                    .isEqualTo("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/.well-known/oauth-authorization-server");
        }

        @Test
        @DisplayName("should have default API URL")
        void shouldHaveDefaultApiUrl() {
            assertThat(settings.apiUrl).isEqualTo("https://sandboxapi.deere.com/platform");
        }

        @Test
        @DisplayName("should have default callback URL")
        void shouldHaveDefaultCallbackUrl() {
            assertThat(settings.callbackUrl).isEqualTo("http://localhost:9090/callback");
        }

        @Test
        @DisplayName("should have default scopes")
        void shouldHaveDefaultScopes() {
            assertThat(settings.scopes).isEqualTo("openid profile offline_access ag1 eq1");
        }

        @Test
        @DisplayName("should generate random state UUID")
        void shouldGenerateRandomStateUuid() {
            assertThat(settings.state).isNotNull();
            assertThat(settings.state).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
        }
    }

    @Nested
    @DisplayName("getBasicAuthHeader")
    class GetBasicAuthHeader {

        @Test
        @DisplayName("should encode client credentials correctly")
        void shouldEncodeClientCredentialsCorrectly() {
            settings.clientId = "testClientId";
            settings.clientSecret = "testClientSecret";

            String header = settings.getBasicAuthHeader();

            String decoded = new String(Base64.getDecoder().decode(header));
            assertThat(decoded).isEqualTo("testClientId:testClientSecret");
        }

        @Test
        @DisplayName("should handle empty credentials")
        void shouldHandleEmptyCredentials() {
            settings.clientId = "";
            settings.clientSecret = "";

            String header = settings.getBasicAuthHeader();

            String decoded = new String(Base64.getDecoder().decode(header));
            assertThat(decoded).isEqualTo(":");
        }
    }

    @Nested
    @DisplayName("updateTokenInfo")
    class UpdateTokenInfo {

        @Test
        @DisplayName("should update all token fields from JSON response")
        void shouldUpdateAllTokenFieldsFromJsonResponse() {
            JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("access_token", "test_access_token");
            tokenResponse.put("refresh_token", "test_refresh_token");
            tokenResponse.put("id_token", "test_id_token");
            tokenResponse.put("expires_in", 3600L);

            settings.updateTokenInfo(tokenResponse);

            assertThat(settings.accessToken).isEqualTo("test_access_token");
            assertThat(settings.refreshToken).isEqualTo("test_refresh_token");
            assertThat(settings.idToken).isEqualTo("test_id_token");
            assertThat(settings.exp).isEqualTo(3600L);
        }

        @Test
        @DisplayName("should handle missing optional fields")
        void shouldHandleMissingOptionalFields() {
            JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("access_token", "test_access_token");

            settings.updateTokenInfo(tokenResponse);

            assertThat(settings.accessToken).isEqualTo("test_access_token");
            assertThat(settings.refreshToken).isEmpty();
            assertThat(settings.idToken).isEmpty();
        }
    }

    @Nested
    @DisplayName("getExpiration")
    class GetExpiration {

        @Test
        @DisplayName("should return null when exp is null")
        void shouldReturnNullWhenExpIsNull() {
            settings.exp = null;

            assertThat(settings.getExpiration()).isNull();
        }

        @Test
        @DisplayName("should return formatted expiration time")
        void shouldReturnFormattedExpirationTime() {
            settings.exp = 3600L;

            String expiration = settings.getExpiration();

            assertThat(expiration).isNotNull();
            assertThat(expiration).containsPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}");
        }
    }

    @Nested
    @DisplayName("getAccessTokenDetails")
    class GetAccessTokenDetails {

        @Test
        @DisplayName("should return null when access token is null")
        void shouldReturnNullWhenAccessTokenIsNull() {
            settings.accessToken = null;

            assertThat(settings.getAccessTokenDetails()).isNull();
        }

        @Test
        @DisplayName("should return null when access token is empty")
        void shouldReturnNullWhenAccessTokenIsEmpty() {
            settings.accessToken = "";

            assertThat(settings.getAccessTokenDetails()).isNull();
        }

        @Test
        @DisplayName("should decode JWT payload correctly")
        void shouldDecodeJwtPayloadCorrectly() {
            String payload = "{\"sub\":\"1234567890\",\"name\":\"Test User\"}";
            String encodedPayload = Base64.getEncoder().encodeToString(payload.getBytes());
            settings.accessToken = "header." + encodedPayload + ".signature";

            String details = settings.getAccessTokenDetails();

            assertThat(details).contains("\"sub\"");
            assertThat(details).contains("1234567890");
            assertThat(details).contains("\"name\"");
            assertThat(details).contains("Test User");
        }
    }
}
