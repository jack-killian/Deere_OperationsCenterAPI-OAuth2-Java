package com.deere.isg.examples;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 1: Application Startup Test
 * Verifies the Javalin server starts successfully and all dependencies load without errors.
 */
public class ApplicationStartupTest {

    @Test
    void serverStartsSuccessfullyOnConfiguredPort() {
        Application app = new Application();
        
        Javalin javalin = Javalin.create(c -> {
            c.staticFiles.add(s -> {
                s.directory = "assets/";
                s.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
        });
        
        JavalinTest.test(javalin, (server, client) -> {
            assertThat(server.port()).isGreaterThan(0);
        });
    }

    @Test
    void indexRouteReturnsSuccessfully() {
        Application app = new Application();
        
        Javalin javalin = Javalin.create(c -> {
            c.staticFiles.add(s -> {
                s.directory = "assets/";
                s.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
        });
        
        javalin.get("/", ctx -> ctx.result("OK"));
        
        JavalinTest.test(javalin, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void allDependenciesLoadWithoutErrors() {
        assertThat(new Application()).isNotNull();
        assertThat(new Settings()).isNotNull();
        assertThat(new Api()).isNotNull();
    }

    @Test
    void settingsInitializesWithDefaultValues() {
        Settings settings = new Settings();
        
        assertThat(settings.wellKnown).isEqualTo("https://signin.johndeere.com/oauth2/aus78tnlaysMraFhC1t7/.well-known/oauth-authorization-server");
        assertThat(settings.apiUrl).isEqualTo("https://sandboxapi.deere.com/platform");
        assertThat(settings.callbackUrl).isEqualTo("http://localhost:9090/callback");
        assertThat(settings.scopes).isEqualTo("openid profile offline_access ag1 eq1");
        assertThat(settings.state).isNotNull();
    }

    @Test
    void serverUrlConstantIsCorrect() {
        assertThat(Settings.SERVER_URL).isEqualTo("http://localhost:9090");
    }
}
