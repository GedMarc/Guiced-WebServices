package com.guicedee.guicedservlets.webservices;

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;

@WebService(name = "SimpleWebService")
public class SimpleWebService
{

	public static void main(String args[])
	{
		Endpoint.publish("http://localhost:6006/SimpleWebService", new SimpleWebService());
	}

	public String sayHello(String name)
	{
		return "Hello, " + name + "!";
	}
}
