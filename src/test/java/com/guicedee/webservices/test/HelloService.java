package com.guicedee.webservices.test;

import jakarta.jws.WebService;

/**
 * Service contract for a simple hello SOAP service.
 */
@WebService(targetNamespace = "http://test.webservices.guicedee.com/hello", name = "HelloService")
public interface HelloService {

    String sayHello(String name);
}

