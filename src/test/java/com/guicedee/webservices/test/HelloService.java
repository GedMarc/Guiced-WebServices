package com.guicedee.webservices.test;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

/**
 * Service contract for a simple hello SOAP service.
 */
@WebService(targetNamespace = "http://test.webservices.guicedee.com/hello", name = "HelloService")
public interface HelloService {

    @WebMethod(operationName = "sayHello")
    @WebResult(name = "greeting")
    String sayHello(@WebParam(name = "name") String name);
}

