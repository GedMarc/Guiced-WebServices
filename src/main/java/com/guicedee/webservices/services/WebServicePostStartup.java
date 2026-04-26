package com.guicedee.webservices.services;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.webservices.WSContext;
import com.guicedee.webservices.transport.VertxTransportFactory;
import io.github.classgraph.ClassInfo;
import io.smallrye.mutiny.Uni;
import jakarta.jws.WebService;
import lombok.extern.log4j.Log4j2;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.DestinationFactoryManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-startup hook that discovers @WebService annotated classes via classpath scanning
 * and publishes them as CXF endpoints on the Vert.x web server using a custom
 * Vert.x transport (no servlet dependency).
 * <p>
 * Endpoints are published using CXF's {@link JaxWsServerFactoryBean} API
 * with the {@link VertxTransportFactory} registered on the CXF Bus.
 */
@Log4j2
public class WebServicePostStartup implements IGuicePostStartup<WebServicePostStartup> {

    @Override
    public List<Uni<Boolean>> postLoad() {
        List<Uni<Boolean>> unis = new ArrayList<>();
        unis.add(Uni.createFrom().item(() -> {
            try {
                publishEndpoints();
                return true;
            } catch (Exception e) {
                log.error("Failed to publish SOAP web service endpoints", e);
                throw new RuntimeException(e);
            }
        }));
        return unis;
    }

    private void publishEndpoints() {
        // Initialize the CXF Bus with Vert.x transport
        Bus bus = BusFactory.newInstance().createBus();
        BusFactory.setDefaultBus(bus);

        // Register our Vert.x transport factory
        VertxTransportFactory transportFactory = new VertxTransportFactory();
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        dfm.registerDestinationFactory(VertxTransportFactory.TRANSPORT_ID, transportFactory);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http", transportFactory);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http/configuration", transportFactory);

        // Store the factory in WSContext for the router configurator to access
        WSContext.setTransportFactory(transportFactory);

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
                publishedCount += publishEndpoint(bus, endpointClass, annotation);
            } catch (Exception e) {
                log.error("Failed to publish web service for: {}", endpointClass.getCanonicalName(), e);
            }
        }

        // Also publish manually registered endpoints
        for (Class<?> endpointClass : WSContext.getRegisteredEndpoints()) {
            WebService annotation = endpointClass.getAnnotation(WebService.class);
            if (annotation == null) {
                log.warn("Manually registered class {} does not have @WebService annotation, skipping",
                        endpointClass.getName());
                continue;
            }

            try {
                publishedCount += publishEndpoint(bus, endpointClass, annotation);
            } catch (Exception e) {
                log.error("Failed to publish manually registered web service for: {}",
                        endpointClass.getCanonicalName(), e);
            }
        }

        log.info("Published {} SOAP web service endpoint(s) under {}", publishedCount, WSContext.baseWSUrl);
    }

    private int publishEndpoint(Bus bus, Class<?> endpointClass, WebService annotation) {
        Object instance = IGuiceContext.get(endpointClass);
        String servicePath = annotation.name().isEmpty()
                ? "/" + endpointClass.getSimpleName()
                : "/" + annotation.name();

        String fullPath = WSContext.cleanPath(WSContext.baseWSUrl) + servicePath.replaceFirst("^/", "");
        fullPath = fullPath.replace("//", "/");

        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);

        factory.setServiceBean(instance);
        factory.setAddress(fullPath);
        factory.setTransportId(VertxTransportFactory.TRANSPORT_ID);

        factory.create();

        log.info("Published SOAP endpoint: {} -> {}", endpointClass.getSimpleName(), fullPath);
        return 1;
    }

    @Override
    public Integer sortOrder() {
        return Integer.MIN_VALUE + 600;
    }
}
