package com.deere.isg.examples;

import com.google.common.collect.ImmutableMap;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationTest {

    private Application application;

    @Mock
    private Context mockContext;

    @BeforeEach
    void setUp() {
        application = new Application();
    }

    @Test
    void testApplicationInstantiation() {
        assertNotNull(application);
    }

    @Test
    void testIndexRendersMainTemplate() {
        application.index(mockContext);
        verify(mockContext).render(eq("main.mustache"), any(ImmutableMap.class));
    }

    @Test
    void testApplicationHasStartMethod() throws NoSuchMethodException {
        var startMethod = application.getClass().getMethod("start");
        assertNotNull(startMethod);
        assertEquals(void.class, startMethod.getReturnType());
    }

    @Test
    void testApplicationHasIndexMethod() throws NoSuchMethodException {
        var indexMethod = application.getClass().getMethod("index", Context.class);
        assertNotNull(indexMethod);
        assertEquals(void.class, indexMethod.getReturnType());
    }
}
