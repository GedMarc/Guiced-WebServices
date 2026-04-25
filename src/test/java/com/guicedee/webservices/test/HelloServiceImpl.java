package com.guicedee.webservices.test;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

/**
 * Implementation of the HelloService SOAP endpoint.
 */
@WebService(endpointInterface = "com.guicedee.webservices.test.HelloService",
        serviceName = "HelloService",
        portName = "HelloPort",
        targetNamespace = "http://test.webservices.guicedee.com/hello")
public class HelloServiceImpl implements HelloService {

    @Override
    @WebMethod(operationName = "sayHello")
    @WebResult(name = "greeting")
    public String sayHello(@WebParam(name = "name") String name) {
        return "Hello " + name;
    }
}

