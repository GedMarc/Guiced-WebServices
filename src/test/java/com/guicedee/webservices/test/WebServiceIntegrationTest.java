package com.guicedee.webservices.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.utils.LogUtils;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SOAP web service endpoints published via CXF on Vert.x.
 * Boots the full GuicedEE context, waits for the server, and issues SOAP requests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebServiceIntegrationTest {

    private HttpClient client;

    @BeforeAll
    void setUp() {
        LogUtils.addConsoleLogger(Level.DEBUG);

        System.out.println("Initializing GuicedEE context for WS tests...");
        // Register test endpoints manually since test module classes may not be found by classpath scan
        com.guicedee.webservices.WSContext.registerEndpoint(HelloServiceImpl.class);
        com.guicedee.webservices.WSContext.registerEndpoint(CalculatorService.class);
        IGuiceContext.instance().inject();

        System.out.println("Waiting for Vert.x HTTP server...");
        TestServerReady.waitForServer();
        System.out.println("Server ready.");

        // Try to manually publish endpoints to see errors
        try {
            var bus = org.apache.cxf.BusFactory.getDefaultBus(false);
            System.out.println("Default CXF Bus: " + bus);
            if (bus == null) {
                bus = org.apache.cxf.BusFactory.newInstance().createBus();
                org.apache.cxf.BusFactory.setDefaultBus(bus);
                System.out.println("Created new CXF Bus: " + bus);
            }

            var vtf = new com.guicedee.webservices.transport.VertxTransportFactory();
            var dfm = bus.getExtension(org.apache.cxf.transport.DestinationFactoryManager.class);
            dfm.registerDestinationFactory(com.guicedee.webservices.transport.VertxTransportFactory.TRANSPORT_ID, vtf);
            dfm.registerDestinationFactory("http://cxf.apache.org/transports/http", vtf);
            dfm.registerDestinationFactory("http://cxf.apache.org/transports/http/configuration", vtf);
            com.guicedee.webservices.WSContext.setTransportFactory(vtf);

            // Publish HelloServiceImpl
            var helloInstance = IGuiceContext.get(HelloServiceImpl.class);
            var factory1 = new org.apache.cxf.jaxws.JaxWsServerFactoryBean();
            factory1.setBus(bus);
            factory1.setServiceBean(helloInstance);
            factory1.setAddress("/WebServices/HelloServiceImpl");
            factory1.setTransportId(com.guicedee.webservices.transport.VertxTransportFactory.TRANSPORT_ID);
            factory1.create();
            System.out.println("Published HelloServiceImpl");

            // Publish CalculatorService
            var calcInstance = IGuiceContext.get(CalculatorService.class);
            var factory2 = new org.apache.cxf.jaxws.JaxWsServerFactoryBean();
            factory2.setBus(bus);
            factory2.setServiceBean(calcInstance);
            factory2.setAddress("/WebServices/CalculatorService");
            factory2.setTransportId(com.guicedee.webservices.transport.VertxTransportFactory.TRANSPORT_ID);
            factory2.create();
            System.out.println("Published CalculatorService");

            System.out.println("Destinations after manual publish: " + vtf.getDestinations().keySet());
        } catch (Exception e) {
            System.out.println("Manual publish failed: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.out);
            if (e.getCause() != null) {
                System.out.println("Root cause: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                e.getCause().printStackTrace(System.out);
            }
        }

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    void tearDown() {
        IGuiceContext.get(Vertx.class).close();
    }

    // --- WSDL accessibility tests ---

    @Test
    @Order(1)
    void testHelloServiceWsdlIsAccessible() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/WebServices/HelloServiceImpl?wsdl"))
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("HelloService WSDL status: " + response.statusCode());
        System.out.println("HelloService WSDL body (first 500): " + response.body().substring(0, Math.min(500, response.body().length())));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("definitions") || response.body().contains("wsdl"),
                "Response should contain WSDL content");
    }

    @Test
    @Order(2)
    void testCalculatorServiceWsdlIsAccessible() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/WebServices/CalculatorService?wsdl"))
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("CalculatorService WSDL status: " + response.statusCode());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("definitions") || response.body().contains("wsdl"),
                "Response should contain WSDL content");
    }

    // --- SOAP invocation tests ---

    @Test
    @Order(3)
    void testSayHelloSoapCall() throws Exception {
        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:hel="http://test.webservices.guicedee.com/hello">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <hel:sayHello>
                         <name>World</name>
                      </hel:sayHello>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                        .uri(new URI("http://localhost:8080/WebServices/HelloServiceImpl"))
                        .header("Content-Type", "text/xml; charset=utf-8")
                        .header("SOAPAction", "")
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("sayHello SOAP response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Hello World"), "Response should contain 'Hello World'");
    }

    @Test
    @Order(4)
    void testCalculatorAddSoapCall() throws Exception {
        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:calc="http://test.webservices.guicedee.com/calculator">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <calc:add>
                         <a>3</a>
                         <b>7</b>
                      </calc:add>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                        .uri(new URI("http://localhost:8080/WebServices/CalculatorService"))
                        .header("Content-Type", "text/xml; charset=utf-8")
                        .header("SOAPAction", "")
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("add SOAP response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("10"), "3 + 7 should equal 10");
    }

    @Test
    @Order(5)
    void testCalculatorSubtractSoapCall() throws Exception {
        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:calc="http://test.webservices.guicedee.com/calculator">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <calc:subtract>
                         <a>20</a>
                         <b>8</b>
                      </calc:subtract>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                        .uri(new URI("http://localhost:8080/WebServices/CalculatorService"))
                        .header("Content-Type", "text/xml; charset=utf-8")
                        .header("SOAPAction", "")
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("subtract SOAP response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("12"), "20 - 8 should equal 12");
    }

    @Test
    @Order(6)
    void testCalculatorMultiplySoapCall() throws Exception {
        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:calc="http://test.webservices.guicedee.com/calculator">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <calc:multiply>
                         <a>4</a>
                         <b>5</b>
                      </calc:multiply>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                        .uri(new URI("http://localhost:8080/WebServices/CalculatorService"))
                        .header("Content-Type", "text/xml; charset=utf-8")
                        .header("SOAPAction", "")
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("multiply SOAP response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("20"), "4 * 5 should equal 20");
    }

    @Test
    @Order(7)
    void testCalculatorEchoSoapCall() throws Exception {
        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:calc="http://test.webservices.guicedee.com/calculator">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <calc:echo>
                         <input>GuicedEE Works</input>
                      </calc:echo>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                        .uri(new URI("http://localhost:8080/WebServices/CalculatorService"))
                        .header("Content-Type", "text/xml; charset=utf-8")
                        .header("SOAPAction", "")
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("echo SOAP response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Echo: GuicedEE Works"), "Response should contain echoed message");
    }

    // --- Error / edge case tests ---

    @Test
    @Order(8)
    void testInvalidSoapEnvelopeReturns500() throws Exception {
        String badSoap = "<not-valid-soap/>";

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(badSoap))
                        .uri(new URI("http://localhost:8080/WebServices/HelloServiceImpl"))
                        .header("Content-Type", "text/xml; charset=utf-8")
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Invalid SOAP response: " + response.statusCode() + " - " + response.body());
        // CXF should return a SOAP fault (500) for invalid requests
        assertTrue(response.statusCode() == 400 || response.statusCode() == 500,
                "Invalid SOAP should return 400 or 500, got: " + response.statusCode());
    }

    @Test
    @Order(9)
    void testNonExistentServiceReturns404() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/WebServices/NoSuchService?wsdl"))
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Non-existent service response: " + response.statusCode());
        // Should not return 200 with valid WSDL
        assertNotEquals(200, response.statusCode(),
                "Non-existent service should not return 200");
    }

    @Test
    @Order(10)
    void testSayHelloWithEmptyName() throws Exception {
        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:hel="http://test.webservices.guicedee.com/hello">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <hel:sayHello>
                         <name></name>
                      </hel:sayHello>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                        .uri(new URI("http://localhost:8080/WebServices/HelloServiceImpl"))
                        .header("Content-Type", "text/xml; charset=utf-8")
                        .header("SOAPAction", "")
                        .timeout(Duration.ofSeconds(5))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Empty name SOAP response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Hello"), "Response should still contain 'Hello'");
    }
}

