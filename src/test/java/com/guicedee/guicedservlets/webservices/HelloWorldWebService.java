package com.guicedee.guicedservlets.webservices;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class HelloWorldWebService
{
	private String message = new String("Hello, ");

	public void Hello()
	{
	}

	@WebMethod
	public String sayHello(String name)
	{
		return message + name + ".";
	}
}
