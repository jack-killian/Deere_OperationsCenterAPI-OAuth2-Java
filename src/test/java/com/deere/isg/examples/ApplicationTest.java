package com.deere.isg.examples;

import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationTest {

    private Application application;
    private Context mockContext;

    @BeforeEach
    void setUp() {
        application = new Application();
        mockContext = mock(Context.class);
    }

    @Test
    void testIndexRendersMainTemplate() {
        application.index(mockContext);
        
        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> modelCaptor = ArgumentCaptor.forClass(Map.class);
        
        verify(mockContext).render(templateCaptor.capture(), modelCaptor.capture());
        
        assertEquals("main.mustache", templateCaptor.getValue());
        assertTrue(modelCaptor.getValue().containsKey("settings"));
    }

    @Test
    void testCallbackWithErrorQueryParam() {
        when(mockContext.queryParam("error")).thenReturn("access_denied");
        when(mockContext.queryParam("error_description")).thenReturn("User denied access");
        
        java.lang.reflect.Method processCallbackMethod;
        try {
            processCallbackMethod = Application.class.getDeclaredMethod("processCallback", Context.class);
            processCallbackMethod.setAccessible(true);
            processCallbackMethod.invoke(application, mockContext);
        } catch (Exception e) {
            fail("Failed to invoke processCallback: " + e.getMessage());
        }
        
        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> modelCaptor = ArgumentCaptor.forClass(Map.class);
        
        verify(mockContext).render(templateCaptor.capture(), modelCaptor.capture());
        
        assertEquals("error.mustache", templateCaptor.getValue());
        assertTrue(modelCaptor.getValue().containsKey("error"));
    }

    @Test
    void testRenderErrorMethod() throws Exception {
        java.lang.reflect.Method renderErrorMethod = Application.class.getDeclaredMethod("renderError", Context.class, String.class);
        renderErrorMethod.setAccessible(true);
        
        String testErrorMessage = "Test error message";
        renderErrorMethod.invoke(application, mockContext, testErrorMessage);
        
        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> modelCaptor = ArgumentCaptor.forClass(Map.class);
        
        verify(mockContext).render(templateCaptor.capture(), modelCaptor.capture());
        
        assertEquals("error.mustache", templateCaptor.getValue());
        Map<String, String> model = modelCaptor.getValue();
        assertEquals(testErrorMessage, model.get("error"));
    }

    @Test
    void testRenderErrorWithNullMessage() throws Exception {
        java.lang.reflect.Method renderErrorMethod = Application.class.getDeclaredMethod("renderError", Context.class, String.class);
        renderErrorMethod.setAccessible(true);
        
        renderErrorMethod.invoke(application, mockContext, null);
        
        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> modelCaptor = ArgumentCaptor.forClass(Map.class);
        
        verify(mockContext).render(templateCaptor.capture(), modelCaptor.capture());
        
        assertEquals("error.mustache", templateCaptor.getValue());
        Map<String, String> model = modelCaptor.getValue();
        assertEquals("", model.get("error"));
    }
}
