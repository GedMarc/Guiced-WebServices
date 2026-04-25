import com.guicedee.client.services.lifecycle.IGuiceConfigurator;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.webservices.implementations.WebServiceRouterConfigurator;
import com.guicedee.webservices.implementations.WebServiceScannerConfig;
import com.guicedee.webservices.services.WebServicePostStartup;

module com.guicedee.webservices {
    requires transitive com.guicedee.vertx.web;
    requires transitive org.apache.cxf;

    requires org.apache.logging.log4j;
    requires static lombok;

    exports com.guicedee.webservices;
    exports com.guicedee.webservices.implementations;
    exports com.guicedee.webservices.services;

    provides IGuiceConfigurator with WebServiceScannerConfig;
    provides IGuicePostStartup with WebServicePostStartup;
    provides com.guicedee.vertx.web.spi.VertxRouterConfigurator with WebServiceRouterConfigurator;

    opens com.guicedee.webservices to com.google.guice;
    opens com.guicedee.webservices.implementations to com.google.guice;
    opens com.guicedee.webservices.services to com.google.guice;
}
