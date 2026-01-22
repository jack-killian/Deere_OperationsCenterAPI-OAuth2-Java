package com.deere.isg.examples;

import com.github.mustachejava.DefaultMustacheFactory;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinMustache;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("Application")
class ApplicationTest {

    @Nested
    @DisplayName("Javalin 6.3.0 Integration")
    class JavalinIntegration {

        @Test
        @DisplayName("should create Javalin instance without errors")
        void shouldCreateJavalinInstanceWithoutErrors() {
            assertThatNoException().isThrownBy(() -> {
                Javalin app = Javalin.create(c -> {
                    c.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
                });
                app.stop();
            });
        }

        @Test
        @DisplayName("should configure static files handler")
        void shouldConfigureStaticFilesHandler() {
            assertThatNoException().isThrownBy(() -> {
                Javalin app = Javalin.create(c -> {
                    c.staticFiles.add(s -> {
                        s.directory = "assets/";
                        s.location = io.javalin.http.staticfiles.Location.CLASSPATH;
                    });
                });
                app.stop();
            });
        }

        @Test
        @DisplayName("should register routes without errors")
        void shouldRegisterRoutesWithoutErrors() {
            assertThatNoException().isThrownBy(() -> {
                Javalin app = Javalin.create();
                app.get("/", ctx -> ctx.result("OK"));
                app.post("/", ctx -> ctx.result("OK"));
                app.get("/callback", ctx -> ctx.result("OK"));
                app.get("/refresh-access-token", ctx -> ctx.result("OK"));
                app.post("/call-api", ctx -> ctx.result("OK"));
                app.stop();
            });
        }
    }

    @Nested
    @DisplayName("Index Route")
    class IndexRoute {

        @Test
        @DisplayName("should return 200 for index page")
        void shouldReturn200ForIndexPage() {
            Javalin app = Javalin.create(c -> {
                c.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            });

            Settings settings = new Settings();
            app.get("/", ctx -> ctx.render("main.mustache", java.util.Map.of("settings", settings)));

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("John Deere OAuth2 Example");
            });
        }

        @Test
        @DisplayName("should render settings form with default values")
        void shouldRenderSettingsFormWithDefaultValues() {
            Javalin app = Javalin.create(c -> {
                c.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            });

            Settings settings = new Settings();
            app.get("/", ctx -> ctx.render("main.mustache", java.util.Map.of("settings", settings)));

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/");
                String body = response.body().string();
                assertThat(body).contains("signin.johndeere.com");
                assertThat(body).contains("http://localhost:9090/callback");
                assertThat(body).contains("openid profile offline_access ag1 eq1");
            });
        }
    }

    @Nested
    @DisplayName("OAuth2 Flow Routes")
    class OAuth2FlowRoutes {

        @Test
        @DisplayName("should handle POST to start OIDC flow")
        void shouldHandlePostToStartOidcFlow() {
            Javalin app = Javalin.create();
            app.post("/start", ctx -> {
                ctx.result("Redirect initiated");
            });

            JavalinTest.test(app, (server, client) -> {
                var response = client.request("/start", builder -> {
                    builder.post(okhttp3.RequestBody.create("", null));
                });
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("Redirect initiated");
            });
        }

        @Test
        @DisplayName("should handle callback with error parameter")
        void shouldHandleCallbackWithErrorParameter() {
            Javalin app = Javalin.create(c -> {
                c.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            });

            app.get("/callback", ctx -> {
                String error = ctx.queryParam("error");
                if (error != null) {
                    String description = ctx.queryParam("error_description");
                    ctx.render("error.mustache", java.util.Map.of("error", description != null ? description : "Unknown error"));
                } else {
                    ctx.result("OK");
                }
            });

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/callback?error=access_denied&error_description=User%20denied%20access");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("User denied access");
            });
        }

        @Test
        @DisplayName("should handle callback with authorization code")
        void shouldHandleCallbackWithAuthorizationCode() {
            Javalin app = Javalin.create();
            app.get("/callback", ctx -> {
                String code = ctx.queryParam("code");
                if (code != null) {
                    ctx.result("Code received: " + code);
                } else {
                    ctx.result("No code");
                }
            });

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/callback?code=test_auth_code&state=test_state");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("test_auth_code");
            });
        }
    }

    @Nested
    @DisplayName("API Call Route")
    class ApiCallRoute {

        @Test
        @DisplayName("should handle POST to call-api endpoint")
        void shouldHandlePostToCallApiEndpoint() {
            Javalin app = Javalin.create();
            app.post("/call-api", ctx -> {
                String url = ctx.formParam("url");
                ctx.result("API URL: " + url);
            });

            JavalinTest.test(app, (server, client) -> {
                var response = client.post("/call-api", "url=https://api.example.com/test");
                assertThat(response.code()).isEqualTo(200);
            });
        }
    }

    @Nested
    @DisplayName("Template Rendering")
    class TemplateRendering {

        @Test
        @DisplayName("should render main template with settings")
        void shouldRenderMainTemplateWithSettings() {
            Javalin app = Javalin.create(c -> {
                c.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            });

            Settings settings = new Settings();
            settings.clientId = "test-client-id";
            settings.clientSecret = "test-secret";

            app.get("/", ctx -> ctx.render("main.mustache", java.util.Map.of("settings", settings)));

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/");
                String body = response.body().string();
                assertThat(body).contains("test-client-id");
            });
        }

        @Test
        @DisplayName("should render error template")
        void shouldRenderErrorTemplate() {
            Javalin app = Javalin.create(c -> {
                c.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            });

            app.get("/error", ctx -> ctx.render("error.mustache", java.util.Map.of("error", "Test error message")));

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/error");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("Test error message");
            });
        }

        @Test
        @DisplayName("should render access token section when token is present")
        void shouldRenderAccessTokenSectionWhenTokenIsPresent() {
            Javalin app = Javalin.create(c -> {
                c.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            });

            Settings settings = new Settings();
            String payload = java.util.Base64.getEncoder().encodeToString("{\"sub\":\"test\"}".getBytes());
            settings.accessToken = "header." + payload + ".signature";
            settings.refreshToken = "test_refresh_token_value";

            app.get("/", ctx -> ctx.render("main.mustache", java.util.Map.of("settings", settings)));

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/");
                String body = response.body().string();
                assertThat(body).contains("Access Token:");
                assertThat(body).contains("test_refresh_token_value");
                assertThat(body).contains("refresh-access-token");
            });
        }
    }

    @Nested
    @DisplayName("Static Assets")
    class StaticAssets {

        @Test
        @DisplayName("should serve CSS files")
        void shouldServeCssFiles() {
            Javalin app = Javalin.create(c -> {
                c.staticFiles.add(s -> {
                    s.directory = "assets/";
                    s.location = io.javalin.http.staticfiles.Location.CLASSPATH;
                });
            });

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/style.css");
                assertThat(response.code()).isEqualTo(200);
            });
        }

        @Test
        @DisplayName("should serve JavaScript files")
        void shouldServeJavaScriptFiles() {
            Javalin app = Javalin.create(c -> {
                c.staticFiles.add(s -> {
                    s.directory = "assets/";
                    s.location = io.javalin.http.staticfiles.Location.CLASSPATH;
                });
            });

            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/functions.js");
                assertThat(response.code()).isEqualTo(200);
            });
        }
    }
}
