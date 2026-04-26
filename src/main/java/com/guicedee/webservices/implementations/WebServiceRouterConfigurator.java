package com.guicedee.webservices.implementations;

import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import com.guicedee.webservices.WSContext;
import com.guicedee.webservices.transport.VertxDestination;
import com.guicedee.webservices.transport.VertxTransportFactory;
import io.vertx.ext.web.Router;
import lombok.extern.log4j.Log4j2;

/**
 * Configures the Vert.x router to dispatch SOAP requests to the CXF engine
 * via the {@link VertxTransportFactory} — no servlet container required.
 * <p>
 * Routes matching the WSContext base path are forwarded to the appropriate
 * {@link VertxDestination} which processes them through CXF's interceptor chain.
 */
@Log4j2
public class WebServiceRouterConfigurator implements VertxRouterConfigurator<WebServiceRouterConfigurator> {

    @Override
    public Router builder(Router router) {
        String basePath = WSContext.cleanPath(WSContext.baseWSUrl);
        log.info("Configuring SOAP Web Services router at path: {}", basePath);

        // Route all requests under the WS base path to the CXF Vert.x transport
        router.route(basePath + "*").handler(ctx -> {
            VertxTransportFactory factory = WSContext.getTransportFactory();
            if (factory == null) {
                log.warn("SOAP transport not yet initialized, returning 503");
                ctx.response().setStatusCode(503).end("SOAP services not yet available");
                return;
            }

            String requestPath = ctx.request().path();
            log.debug("SOAP Request received: {} {}", ctx.request().method(), requestPath);

            // Find the matching CXF destination for this path
            VertxDestination destination = factory.getDestinationForAddress(requestPath);
            if (destination == null) {
                log.warn("No SOAP endpoint registered for path: {}", requestPath);
                ctx.response().setStatusCode(404).end("No SOAP endpoint at " + requestPath);
                return;
            }

            // Handle ?wsdl requests
            String query = ctx.request().query();
            if (query != null && query.toLowerCase().contains("wsdl")) {
                destination.handleWsdlRequest(ctx);
                return;
            }

            // Handle SOAP request
            destination.handleRequest(ctx);
        });

        return router;
    }

    @Override
    public Integer sortOrder() {
        return 200;
    }
}
