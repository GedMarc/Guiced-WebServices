package com.guicedee.webservices.implementations;

import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import com.guicedee.webservices.WSContext;
import io.vertx.ext.web.Router;
import lombok.extern.log4j.Log4j2;

/**
 * Configures the Vert.x router to proxy SOAP requests to the embedded CXF HTTP server.
 * Routes matching the WSContext base path are forwarded to CXF's local transport.
 */
@Log4j2
public class WebServiceRouterConfigurator implements VertxRouterConfigurator<WebServiceRouterConfigurator> {

    @Override
    public Router builder(Router router) {
        String basePath = WSContext.cleanPath(WSContext.baseWSUrl);
        log.info("Configuring SOAP Web Services router at path: {}", basePath);

        // Route all requests under the WS base path to the CXF handler
        router.route(basePath + "*").handler(ctx -> {
            log.debug("SOAP Request received: {} {}", ctx.request().method(), ctx.request().path());
            // Delegate to the CXF Vert.x handler registered during post-startup
            ctx.next();
        });

        return router;
    }

    @Override
    public Integer sortOrder() {
        return 200;
    }
}

