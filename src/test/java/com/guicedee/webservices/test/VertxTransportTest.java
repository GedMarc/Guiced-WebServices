package com.guicedee.webservices.test;

import com.guicedee.webservices.WSContext;
import com.guicedee.webservices.transport.VertxTransportFactory;
import com.guicedee.webservices.transport.VertxDestination;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.service.model.EndpointInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Vert.x CXF transport layer (no server boot required).
 */
public class VertxTransportTest {

    private static Bus bus;

    @BeforeAll
    static void initBus() {
        bus = BusFactory.newInstance().createBus();
    }

    @Test
    void testTransportFactoryCreatesDestination() throws IOException {
        VertxTransportFactory factory = new VertxTransportFactory();
        EndpointInfo info = new EndpointInfo();
        info.setAddress("/WebServices/TestService");

        var destination = factory.getDestination(info, bus);
        assertNotNull(destination, "Factory should create a destination");
        assertInstanceOf(VertxDestination.class, destination);
    }

    @Test
    void testTransportFactoryReturnsSameDestinationForSameAddress() throws IOException {
        VertxTransportFactory factory = new VertxTransportFactory();
        EndpointInfo info = new EndpointInfo();
        info.setAddress("/WebServices/SameService");

        var dest1 = factory.getDestination(info, bus);
        var dest2 = factory.getDestination(info, bus);
        assertSame(dest1, dest2, "Same address should return the same destination instance");
    }

    @Test
    void testTransportFactoryLookupByAddress() throws IOException {
        VertxTransportFactory factory = new VertxTransportFactory();
        EndpointInfo info = new EndpointInfo();
        info.setAddress("/WebServices/LookupTest");

        factory.getDestination(info, bus);

        VertxDestination found = factory.getDestinationForAddress("/WebServices/LookupTest");
        assertNotNull(found, "Should find destination by exact address");
    }

    @Test
    void testTransportFactoryReturnsNullForUnknownAddress() {
        VertxTransportFactory factory = new VertxTransportFactory();
        assertNull(factory.getDestinationForAddress("/no/such/path"),
                "Should return null for unknown address");
    }

    @Test
    void testTransportFactoryUriPrefixes() {
        VertxTransportFactory factory = new VertxTransportFactory();
        var prefixes = factory.getUriPrefixes();
        assertTrue(prefixes.contains("http://"));
        assertTrue(prefixes.contains("https://"));
        assertTrue(prefixes.contains("/"));
    }

    @Test
    void testWSContextTransportFactoryRoundtrip() {
        VertxTransportFactory factory = new VertxTransportFactory();
        WSContext.setTransportFactory(factory);
        assertSame(factory, WSContext.getTransportFactory());
        // clean up
        WSContext.setTransportFactory(null);
    }
}

