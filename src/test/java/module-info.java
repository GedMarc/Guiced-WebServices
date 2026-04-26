module com.guicedee.webservices.test {
    requires com.guicedee.webservices;

    requires java.net.http;

    requires org.junit.jupiter.api;
    requires static lombok;

    requires com.google.guice;
    requires com.guicedee.client;
    requires org.apache.cxf;
    requires jakarta.xml.ws;
    requires io.vertx.core;

    exports com.guicedee.webservices.test;

    opens com.guicedee.webservices.test to org.junit.platform.commons, com.google.guice, com.fasterxml.jackson.databind, io.github.classgraph, org.apache.cxf;
}

