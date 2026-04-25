package com.guicedee.webservices.test;

import com.guicedee.webservices.WSContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WSContext configuration utilities (no server needed).
 */
public class WSContextTest {

    @Test
    void testCleanPathAddsLeadingSlash() {
        assertEquals("/test/", WSContext.cleanPath("test"));
    }

    @Test
    void testCleanPathAddsTrailingSlash() {
        assertEquals("/test/", WSContext.cleanPath("/test"));
    }

    @Test
    void testCleanPathAlreadyClean() {
        assertEquals("/test/", WSContext.cleanPath("/test/"));
    }

    @Test
    void testCleanPathBare() {
        assertEquals("/foo/", WSContext.cleanPath("foo"));
    }

    @Test
    void testDefaultBaseUrl() {
        assertEquals("/WebServices", WSContext.baseWSUrl);
    }

    @Test
    void testRegisterEndpoint() {
        int before = WSContext.getRegisteredEndpoints().size();
        WSContext.registerEndpoint(HelloServiceImpl.class);
        assertTrue(WSContext.getRegisteredEndpoints().contains(HelloServiceImpl.class));
        // Clean up
        WSContext.getRegisteredEndpoints().remove(HelloServiceImpl.class);
    }
}

