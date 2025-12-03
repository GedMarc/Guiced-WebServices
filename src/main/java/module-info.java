module com.guicedee.webservices {
	exports com.guicedee.guicedservlets.webservices;

	requires com.guicedee.guicedservlets.undertow;
	requires org.apache.cxf;
	requires transitive jakarta.jws;
	requires transitive jakarta.xml.ws;
	requires transitive jakarta.xml.soap;


	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.webservices.implementations.WebServiceServletModule;
	provides com.guicedee.client.services.lifecycle.IGuiceConfigurator with com.guicedee.guicedservlets.webservices.implementations.WebServiceScannerConfig;
	provides com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator with com.guicedee.guicedservlets.webservices.implementations.JaxWSUndertowDeploymentConfigurator;

	opens com.guicedee.guicedservlets.webservices.implementations to com.google.guice;
	opens com.guicedee.guicedservlets.webservices to com.google.guice;
}
