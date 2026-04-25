package com.guicedee.webservices.test;

import com.guicedee.client.IGuiceContext;
import io.vertx.core.Vertx;
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
        System.out.println("Initializing GuicedEE context for WS tests...");
        IGuiceContext.instance().inject();

        System.out.println("Waiting for Vert.x HTTP server...");
        TestServerReady.waitForServer();
        System.out.println("Server ready.");

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

