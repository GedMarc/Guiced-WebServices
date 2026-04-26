package com.guicedee.webservices;

import com.guicedee.webservices.transport.VertxTransportFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration context for SOAP web services.
 * Controls the base URL path under which CXF endpoints are published
 * and holds a reference to the Vert.x transport factory.
 */
public class WSContext {

    /**
     * The base URL path for all SOAP web service endpoints.
     * e.g. http://localhost:8080/WebServices/helloworld
     */
    public static String baseWSUrl = "/WebServices";

    private static final Set<Class<?>> registeredEndpoints = new HashSet<>();
    private static VertxTransportFactory transportFactory;

    /**
     * Manually register an endpoint implementation class for publishing.
     * If not called, endpoints are discovered automatically via classpath scanning.
     *
     * @param endpointClass the @WebService annotated implementation class
     */
    public static void registerEndpoint(Class<?> endpointClass) {
        registeredEndpoints.add(endpointClass);
    }

    /**
     * Returns manually registered endpoint classes.
     *
     * @return set of registered endpoint classes
     */
    public static Set<Class<?>> getRegisteredEndpoints() {
        return registeredEndpoints;
    }

    /**
     * Sets the Vert.x transport factory (called during post-startup).
     */
    public static void setTransportFactory(VertxTransportFactory factory) {
        transportFactory = factory;
    }

    /**
     * Returns the Vert.x transport factory for routing SOAP requests.
     */
    public static VertxTransportFactory getTransportFactory() {
        return transportFactory;
    }

    /**
     * Ensures a path starts and ends with '/'.
     *
     * @param path the path to clean
     * @return the cleaned path
     */
    public static String cleanPath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return path;
    }
}
