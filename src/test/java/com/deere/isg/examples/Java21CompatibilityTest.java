package com.deere.isg.examples;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableMap;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("Java 21 Compatibility Tests")
class Java21CompatibilityTest {

    @Nested
    @DisplayName("Unirest 4.4.0 Compatibility")
    class UnirestCompatibility {

        @Test
        @DisplayName("should create Unirest JSON objects")
        void shouldCreateUnirestJsonObjects() {
            JSONObject json = new JSONObject();
            json.put("key", "value");
            json.put("number", 42);
            json.put("nested", new JSONObject().put("inner", "data"));

            assertThat(json.getString("key")).isEqualTo("value");
            assertThat(json.getInt("number")).isEqualTo(42);
            assertThat(json.getJSONObject("nested").getString("inner")).isEqualTo("data");
        }

        @Test
        @DisplayName("should handle JSON arrays")
        void shouldHandleJsonArrays() {
            JSONObject json = new JSONObject();
            json.put("values", new kong.unirest.core.json.JSONArray()
                    .put(new JSONObject().put("id", 1))
                    .put(new JSONObject().put("id", 2)));

            assertThat(json.getJSONArray("values").length()).isEqualTo(2);
        }

        @Test
        @DisplayName("should configure Unirest without errors")
        void shouldConfigureUnirestWithoutErrors() {
            assertThatNoException().isThrownBy(() -> {
                Unirest.config().reset();
            });
        }
    }

    @Nested
    @DisplayName("Guava 33.2.1-jre Compatibility")
    class GuavaCompatibility {

        @Test
        @DisplayName("should use ImmutableMap")
        void shouldUseImmutableMap() {
            Map<String, String> map = ImmutableMap.of("key1", "value1", "key2", "value2");

            assertThat(map).hasSize(2);
            assertThat(map.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("should use Strings utility")
        void shouldUseStringsUtility() {
            assertThat(com.google.common.base.Strings.isNullOrEmpty(null)).isTrue();
            assertThat(com.google.common.base.Strings.isNullOrEmpty("")).isTrue();
            assertThat(com.google.common.base.Strings.isNullOrEmpty("test")).isFalse();
        }

        @Test
        @DisplayName("should use Throwables utility")
        void shouldUseThrowablesUtility() {
            Exception e = new RuntimeException("Test exception");
            String stackTrace = com.google.common.base.Throwables.getStackTraceAsString(e);

            assertThat(stackTrace).contains("RuntimeException");
            assertThat(stackTrace).contains("Test exception");
        }
    }

    @Nested
    @DisplayName("Mustache 0.9.14 Compatibility")
    class MustacheCompatibility {

        @Test
        @DisplayName("should compile and render templates")
        void shouldCompileAndRenderTemplates() {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(new java.io.StringReader("Hello {{name}}!"), "test");

            StringWriter writer = new StringWriter();
            mustache.execute(writer, ImmutableMap.of("name", "World"));

            assertThat(writer.toString()).isEqualTo("Hello World!");
        }

        @Test
        @DisplayName("should handle nested objects in templates")
        void shouldHandleNestedObjectsInTemplates() {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(
                    new java.io.StringReader("{{#settings}}Value: {{value}}{{/settings}}"),
                    "nested"
            );

            StringWriter writer = new StringWriter();
            mustache.execute(writer, ImmutableMap.of("settings", ImmutableMap.of("value", "test")));

            assertThat(writer.toString()).isEqualTo("Value: test");
        }

        @Test
        @DisplayName("should handle conditional sections")
        void shouldHandleConditionalSections() {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(
                    new java.io.StringReader("{{#show}}Visible{{/show}}{{^show}}Hidden{{/show}}"),
                    "conditional"
            );

            StringWriter writerTrue = new StringWriter();
            mustache.execute(writerTrue, ImmutableMap.of("show", true));
            assertThat(writerTrue.toString()).isEqualTo("Visible");

            StringWriter writerFalse = new StringWriter();
            mustache.execute(writerFalse, ImmutableMap.of("show", false));
            assertThat(writerFalse.toString()).isEqualTo("Hidden");
        }
    }

    @Nested
    @DisplayName("Java 21 Language Features")
    class Java21LanguageFeatures {

        @Test
        @DisplayName("should support var keyword")
        void shouldSupportVarKeyword() {
            var message = "Hello Java 21";
            var number = 42;
            var list = java.util.List.of("a", "b", "c");

            assertThat(message).isInstanceOf(String.class);
            assertThat(number).isInstanceOf(Integer.class);
            assertThat(list).hasSize(3);
        }

        @Test
        @DisplayName("should support text blocks")
        void shouldSupportTextBlocks() {
            String json = """
                    {
                        "name": "test",
                        "value": 123
                    }
                    """;

            assertThat(json).contains("\"name\"");
            assertThat(json).contains("\"value\"");
        }

        @Test
        @DisplayName("should support pattern matching for instanceof")
        void shouldSupportPatternMatchingForInstanceof() {
            Object obj = "Hello";

            if (obj instanceof String s) {
                assertThat(s.length()).isEqualTo(5);
            }
        }

        @Test
        @DisplayName("should support record patterns")
        void shouldSupportRecordPatterns() {
            record Point(int x, int y) {}
            Point p = new Point(10, 20);

            assertThat(p.x()).isEqualTo(10);
            assertThat(p.y()).isEqualTo(20);
        }

        @Test
        @DisplayName("should support switch expressions")
        void shouldSupportSwitchExpressions() {
            String day = "MONDAY";
            String type = switch (day) {
                case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "Weekday";
                case "SATURDAY", "SUNDAY" -> "Weekend";
                default -> "Unknown";
            };

            assertThat(type).isEqualTo("Weekday");
        }

        @Test
        @DisplayName("should support record classes")
        void shouldSupportRecordClasses() {
            record Person(String name, int age) {}
            Person person = new Person("John", 30);

            assertThat(person.name()).isEqualTo("John");
            assertThat(person.age()).isEqualTo(30);
            assertThat(person.toString()).contains("John");
            assertThat(person.toString()).contains("30");
        }

        @Test
        @DisplayName("should support virtual threads (Java 21 feature)")
        void shouldSupportVirtualThreads() throws InterruptedException {
            var result = new java.util.concurrent.atomic.AtomicBoolean(false);

            Thread vThread = Thread.ofVirtual().start(() -> {
                result.set(true);
            });

            vThread.join();
            assertThat(result.get()).isTrue();
        }

        @Test
        @DisplayName("should support sequenced collections (Java 21 feature)")
        void shouldSupportSequencedCollections() {
            java.util.SequencedCollection<String> list = new java.util.ArrayList<>();
            list.addFirst("first");
            list.addLast("last");

            assertThat(list.getFirst()).isEqualTo("first");
            assertThat(list.getLast()).isEqualTo("last");
        }
    }

    @Nested
    @DisplayName("SLF4J 2.0.13 Compatibility")
    class Slf4jCompatibility {

        @Test
        @DisplayName("should create logger without errors")
        void shouldCreateLoggerWithoutErrors() {
            assertThatNoException().isThrownBy(() -> {
                org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
                logger.info("Test message");
            });
        }

        @Test
        @DisplayName("should support parameterized logging")
        void shouldSupportParameterizedLogging() {
            assertThatNoException().isThrownBy(() -> {
                org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
                logger.info("Message with {} and {}", "param1", "param2");
            });
        }
    }
}
