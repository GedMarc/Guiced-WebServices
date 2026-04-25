package com.guicedee.webservices.services;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.webservices.WSContext;
import io.github.classgraph.ClassInfo;
import io.vertx.core.Future;
import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.BusFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-startup hook that discovers @WebService annotated classes via classpath scanning
 * and publishes them as CXF endpoints on the Vert.x web server.
 * <p>
 * Endpoints are published using CXF's {@link Endpoint#publish(String, Object)} API
 * with the configured base URL from {@link WSContext#baseWSUrl}.
 */
@Slf4j
public class WebServicePostStartup implements IGuicePostStartup<WebServicePostStartup> {

    @Override
    public List<Future<Boolean>> postLoad() {
        List<Future<Boolean>> futures = new ArrayList<>();
        futures.add(Future.future(promise -> {
            try {
                publishEndpoints();
                promise.complete(true);
            } catch (Exception e) {
                log.error("Failed to publish SOAP web service endpoints", e);
                promise.fail(e);
            }
        }));
        return futures;
    }

    private void publishEndpoints() {
        // Initialize the CXF Bus
        var bus = BusFactory.newInstance().createBus();
        BusFactory.setDefaultBus(bus);

        // Discover @WebService annotated classes from classpath scan
        var scanResult = IGuiceContext.instance().getScanResult();
        var webServiceClasses = scanResult.getClassesWithAnnotation(WebService.class.getCanonicalName());

        int publishedCount = 0;
        for (ClassInfo classInfo : webServiceClasses) {
            if (classInfo.isInterface() || classInfo.isAbstract()) {
                log.debug("Skipping interface/abstract class: {}", classInfo.getName());
                continue;
            }

            Class<?> endpointClass = classInfo.loadClass();
            WebService annotation = endpointClass.getAnnotation(WebService.class);
            if (annotation == null) {
                continue;
            }

            try {
                Object instance = IGuiceContext.get(endpointClass);
                String servicePath = annotation.name().isEmpty()
                        ? "/" + endpointClass.getSimpleName()
                        : "/" + annotation.name();

                String fullPath = WSContext.cleanPath(WSContext.baseWSUrl) + servicePath.replaceFirst("^/", "");
                fullPath = fullPath.replace("//", "/");

                Endpoint.publish(fullPath, instance);
                publishedCount++;
                log.info("Published SOAP endpoint: {} -> {}", endpointClass.getSimpleName(), fullPath);
            } catch (Exception e) {
                log.error("Failed to publish web service for: {}", endpointClass.getCanonicalName(), e);
            }
        }

        // Also publish manually registered endpoints
        for (Class<?> endpointClass : WSContext.getRegisteredEndpoints()) {
            WebService annotation = endpointClass.getAnnotation(WebService.class);
            if (annotation == null) {
                log.warn("Manually registered class {} does not have @WebService annotation, skipping", endpointClass.getName());
                continue;
            }

            try {
                Object instance = IGuiceContext.get(endpointClass);
                String servicePath = annotation.name().isEmpty()
                        ? "/" + endpointClass.getSimpleName()
                        : "/" + annotation.name();

                String fullPath = WSContext.cleanPath(WSContext.baseWSUrl) + servicePath.replaceFirst("^/", "");
                fullPath = fullPath.replace("//", "/");

                Endpoint.publish(fullPath, instance);
                publishedCount++;
                log.info("Published manually registered SOAP endpoint: {} -> {}", endpointClass.getSimpleName(), fullPath);
            } catch (Exception e) {
                log.error("Failed to publish manually registered web service for: {}", endpointClass.getCanonicalName(), e);
            }
        }

        log.info("Published {} SOAP web service endpoint(s) under {}", publishedCount, WSContext.baseWSUrl);
    }

    @Override
    public Integer sortOrder() {
        // Run after the web server is started
        return Integer.MIN_VALUE + 600;
    }
}

