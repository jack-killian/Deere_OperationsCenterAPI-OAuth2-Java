package com.deere.isg.examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {

    private Api api;

    @BeforeEach
    void setUp() {
        api = new Api();
    }

    @Test
    void testApiInstantiation() {
        assertNotNull(api);
    }

    @Test
    void testGetMethodExists() {
        assertNotNull(api.getClass().getMethods());
        boolean hasGetMethod = false;
        for (var method : api.getClass().getMethods()) {
            if (method.getName().equals("get") && method.getParameterCount() == 2) {
                hasGetMethod = true;
                break;
            }
        }
        assertTrue(hasGetMethod, "Api class should have a get method with 2 parameters");
    }

    @Test
    void testGetMethodSignature() throws NoSuchMethodException {
        var getMethod = api.getClass().getMethod("get", String.class, String.class);
        assertNotNull(getMethod);
        assertEquals("JSONObject", getMethod.getReturnType().getSimpleName());
    }
}
