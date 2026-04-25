package com.guicedee.webservices.test;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

/**
 * A calculator SOAP service to test multiple operations and parameter binding.
 */
@WebService(serviceName = "CalculatorService",
        portName = "CalculatorPort",
        targetNamespace = "http://test.webservices.guicedee.com/calculator")
public class CalculatorService {

    @WebMethod(operationName = "add")
    @WebResult(name = "result")
    public int add(@WebParam(name = "a") int a, @WebParam(name = "b") int b) {
        return a + b;
    }

    @WebMethod(operationName = "subtract")
    @WebResult(name = "result")
    public int subtract(@WebParam(name = "a") int a, @WebParam(name = "b") int b) {
        return a - b;
    }

    @WebMethod(operationName = "multiply")
    @WebResult(name = "result")
    public int multiply(@WebParam(name = "a") int a, @WebParam(name = "b") int b) {
        return a * b;
    }

    @WebMethod(operationName = "echo")
    @WebResult(name = "message")
    public String echo(@WebParam(name = "input") String input) {
        return "Echo: " + input;
    }
}

