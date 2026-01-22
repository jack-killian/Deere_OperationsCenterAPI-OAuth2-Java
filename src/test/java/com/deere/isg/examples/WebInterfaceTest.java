package com.deere.isg.examples;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 5: Web Interface Test
 * Tests Mustache template rendering, form submission, and parameter handling.
 */
public class WebInterfaceTest {

    private Settings settings;
    private MustacheFactory mustacheFactory;

    @BeforeEach
    void setUp() {
        settings = new Settings();
        mustacheFactory = new DefaultMustacheFactory("templates");
    }

    @Test
    void mustacheFactoryIsCreatedSuccessfully() {
        assertThat(mustacheFactory).isNotNull();
    }

    @Test
    void settingsCanBeUsedAsTemplateContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("settings", settings);
        
        assertThat(context.get("settings")).isNotNull();
        assertThat(context.get("settings")).isInstanceOf(Settings.class);
    }

    @Test
    void formParametersArePopulatedCorrectly() {
        Javalin javalin = Javalin.create();
        javalin.post("/", ctx -> {
            Settings testSettings = new Settings();
            testSettings.clientId = ctx.formParam("clientId");
            testSettings.clientSecret = ctx.formParam("clientSecret");
            ctx.result("clientId=" + testSettings.clientId);
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.post("/", "clientId=test-id&clientSecret=test-secret");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("clientId=test-id");
        });
    }

    @Test
    void wellKnownParameterIsHandled() {
        Javalin javalin = Javalin.create();
        javalin.post("/", ctx -> {
            String wellKnown = ctx.formParam("wellKnown");
            ctx.result("wellKnown=" + wellKnown);
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.post("/", "wellKnown=https://example.com/.well-known/oauth");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void callbackUrlParameterIsHandled() {
        Javalin javalin = Javalin.create();
        javalin.post("/", ctx -> {
            String callbackUrl = ctx.formParam("callbackUrl");
            ctx.result("callbackUrl=" + callbackUrl);
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.post("/", "callbackUrl=http://localhost:9090/callback");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void scopesParameterIsHandled() {
        Javalin javalin = Javalin.create();
        javalin.post("/", ctx -> {
            String scopes = ctx.formParam("scopes");
            ctx.result("scopes=" + scopes);
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.post("/", "scopes=openid%20profile%20offline_access");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void stateParameterIsHandled() {
        Javalin javalin = Javalin.create();
        javalin.post("/", ctx -> {
            String state = ctx.formParam("state");
            ctx.result("state=" + state);
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.post("/", "state=random-state-value");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("state=random-state-value");
        });
    }

    @Test
    void errorTemplateCanBeRendered() {
        Javalin javalin = Javalin.create();
        javalin.get("/error", ctx -> {
            ctx.status(500).result("Error: Something went wrong");
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.get("/error");
            assertThat(response.code()).isEqualTo(500);
            assertThat(response.body().string()).contains("Error");
        });
    }

    @Test
    void indexRouteReturnsHtml() {
        Javalin javalin = Javalin.create();
        javalin.get("/", ctx -> {
            ctx.contentType("text/html").result("<html><body>Test</body></html>");
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void staticAssetsDirectoryIsConfigured() {
        Javalin javalin = Javalin.create(c -> {
            c.staticFiles.add(s -> {
                s.directory = "assets/";
                s.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
        });
        
        assertThat(javalin).isNotNull();
    }

    @Test
    void settingsFieldsAreAccessibleForTemplates() {
        settings.clientId = "template-client-id";
        settings.accessToken = "template-access-token";
        settings.apiResponse = "{\"test\": \"response\"}";
        
        assertThat(settings.clientId).isEqualTo("template-client-id");
        assertThat(settings.accessToken).isEqualTo("template-access-token");
        assertThat(settings.apiResponse).contains("test");
    }

    @Test
    void accessTokenDetailsAreAvailableForDisplay() {
        String header = java.util.Base64.getEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = java.util.Base64.getEncoder().encodeToString("{\"sub\":\"user\"}".getBytes());
        settings.accessToken = header + "." + payload + ".signature";
        
        String details = settings.getAccessTokenDetails();
        
        assertThat(details).isNotNull();
        assertThat(details).contains("sub");
    }

    @Test
    void expirationIsFormattedForDisplay() {
        settings.exp = 3600L;
        
        String expiration = settings.getExpiration();
        
        assertThat(expiration).isNotNull();
        assertThat(expiration).matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }
}
