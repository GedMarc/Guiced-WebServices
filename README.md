# 🧾 GuicedEE Web Services (Apache CXF‑style)

[![JDK](https://img.shields.io/badge/JDK-25%2B-0A7?logo=java)](https://openjdk.org/projects/jdk/25/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

GuicedEE Web Services uses the same annotations and configuration model as Apache CXF. You build SOAP services using standard JAX‑WS annotations (`@WebService`, `@WebMethod`, `@WebParam`, `@WebResult`) and configure endpoints, bindings, interceptors, and WS‑Security in the same way you would with CXF.

This README shows—end‑to‑end—how to create, configure, and expose a SOAP web service in a GuicedEE application.

## ✨ Features
- JAX‑WS (code‑first or WSDL‑first) with Apache CXF conventions
- Familiar CXF configuration: addresses, bindings (SOAP 1.1/1.2), interceptors, logging, MTOM, WS‑Security (WSS4J)
- Guice DI friendly: bind implementations and bootstrap endpoints via SPI/lifecycle
- Works alongside GuicedEE Vert.x Web and REST/OpenAPI modules
- JPMS + ServiceLoader discovery for configurators/providers

## 📦 Install (Maven)
```
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>guiced-webservices</artifactId>
</dependency>

<!-- If you generate from WSDL, add CXF codegen plugin in your host app -->
<plugin>
  <groupId>org.apache.cxf</groupId>
  <artifactId>cxf-codegen-plugin</artifactId>
  <version>${cxf.version}</version>
  <executions>
    <execution>
      <id>wsdl2java</id>
      <phase>generate-sources</phase>
      <configuration>
        <wsdlOptions>
          <wsdlOption>
            <wsdl>${project.basedir}/src/main/resources/wsdl/hello.wsdl</wsdl>
            <extraargs>
              <extraarg>-exsh</extraarg>
              <extraarg>true</extraarg>
            </extraargs>
          </wsdlOption>
        </wsdlOptions>
      </configuration>
      <goals>
        <goal>wsdl2java</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

## 🚀 Quick Start (code‑first JAX‑WS)
1) Define the service contract and implementation using JAX‑WS annotations (CXF‑style):
```
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService(targetNamespace = "http://example.com/hello", name = "HelloService")
public interface HelloService {
  @WebMethod(operationName = "sayHello")
  @WebResult(name = "greeting")
  String sayHello(@WebParam(name = "name") String name);
}

@WebService(endpointInterface = "com.example.ws.HelloService",
            serviceName = "HelloService",
            portName = "HelloPort",
            targetNamespace = "http://example.com/hello")
public class HelloServiceImpl implements HelloService {
  @Override
  public String sayHello(String name) { return "Hello " + name; }
}
```

2) Bind your implementation with Guice and publish the endpoint during startup. You can publish using JAX‑WS `Endpoint` or CXF’s `JaxWsServerFactoryBean`—both are supported configurations in CXF:
```
// A GuicedEE post‑startup hook
public final class WebServicesStartup implements com.guicedee.client.IGuicePostStartup {
  @jakarta.inject.Inject HelloServiceImpl hello;

  @Override
  public void onPostStartup() {
    // Option A: JAX‑WS Endpoint (CXF will wire the transport)
    jakarta.xml.ws.Endpoint.publish("/services/hello", hello);

    // Option B: CXF factory (for advanced settings)
    // org.apache.cxf.jaxws.JaxWsServerFactoryBean factory = new org.apache.cxf.jaxws.JaxWsServerFactoryBean();
    // factory.setServiceBean(hello);
    // factory.setAddress("/services/hello");
    // factory.setBindingId(org.apache.cxf.binding.soap.SoapBindingConstants.SOAP11_BINDING_ID);
    // factory.create();
  }
}
```

3) Start your GuicedEE application; the endpoint will be available at the configured address (see Configuration below for base URL/host configuration).

## 🧰 WSDL‑first (optional)
If you start from WSDL, run CXF’s `wsdl2java` (plugin shown in Install). Implement the generated SEI interface in your class and publish as above. Keep the WSDL under `src/main/resources/wsdl/` and consider exposing it under `?wsdl` via CXF settings.

## ⚙️ Configuration (Apache CXF conventions)
Typical environment keys (align with your global config rules):
```
WS_BASE_ADDRESS=http://0.0.0.0:8080            # base HTTP address
WS_CONTEXT_PATH=/services                      # root path for SOAP services
WS_HELLO_PATH=/hello                           # service‑specific path
WS_SOAP_VERSION=1.1                             # 1.1 or 1.2
WS_LOGGING_ENABLED=true                         # CXF logging interceptors
WS_MATOM_ENABLED=false                          # MTOM attachments
WS_WSDL_LOCATION=classpath:wsdl/hello.wsdl      # optional for WSDL‑first
WS_SECURITY_POLICY=classpath:ws/security.xml    # WS‑Security policy file
```

Interceptor and security examples (mirror CXF patterns):
```
// Example of adding interceptors when using JaxWsServerFactoryBean
org.apache.cxf.jaxws.JaxWsServerFactoryBean factory = new org.apache.cxf.jaxws.JaxWsServerFactoryBean();
factory.setServiceBean(hello);
factory.setAddress(System.getenv().getOrDefault("WS_CONTEXT_PATH", "/services") + 
                   System.getenv().getOrDefault("WS_HELLO_PATH", "/hello"));

if (Boolean.parseBoolean(System.getenv().getOrDefault("WS_LOGGING_ENABLED", "true"))) {
  factory.getInInterceptors().add(new org.apache.cxf.interceptor.LoggingInInterceptor());
  factory.getOutInterceptors().add(new org.apache.cxf.interceptor.LoggingOutInterceptor());
}

// WS‑Security (WSS4J) example
// Map<String,Object> props = new HashMap<>();
// props.put("action", "UsernameToken Timestamp");
// props.put("passwordType", "PasswordText");
// props.put("passwordCallbackClass", "com.example.ws.security.ServerPasswordCallback");
// factory.getInInterceptors().add(new org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor(props));
// factory.create();
```

MTOM (attachments):
```
// Enable MTOM on binding
// factory.getProperties().put("mtom-enabled", Boolean.TRUE);
// Or annotate @MTOM on your endpoint implementation
```

SOAP 1.2 binding:
```
// factory.setBindingId(org.apache.cxf.binding.soap.SoapBindingConstants.SOAP12_BINDING_ID);
```

Vertex/HTTP hosting notes:
- If you host HTTP with GuicedEE Vert.x Web, expose CXF endpoints under the same HTTP server. Use consistent context paths so `/services/*` routes through to CXF.
- TLS/HTTPS is configured at the Vert.x Web server level (see GuicedEE Web). CXF inherits the transport from the server container.

## ✅ Testing your service
Manual checks:
```
# Retrieve WSDL
curl -sSf http://localhost:8080/services/hello?wsdl | head -n 20

# SoapUI / Postman
- Import the WSDL and invoke sayHello(name)
```

Generated client (CXF):
```
// Using wsdl2java generated stubs
HelloService client = new HelloService_Service().getHelloPort();
String result = client.sayHello("World");
```

## 🧩 JPMS & SPI
- JPMS‑ready; if you publish SPI/lifecycle hooks, register them via `META-INF/services` and (optionally) `module-info.java` provides entries.
- Common SPI you may implement in this module: GuicedEE post‑startup to bootstrap endpoints; route/config providers.

## 📚 Docs & Rules
- Rules: `RULES.md`
- Guides: `GUIDES.md`
- Architecture: `docs/architecture/README.md`
- CXF Docs (reference): https://cxf.apache.org/docs/

## 🧰 Troubleshooting
- 404 on `?wsdl`: ensure the binding and address match and that the service is published before requests arrive.
- 415/Content‑Type errors: verify SOAPAction headers and that you’re using SOAP 1.1 vs 1.2 consistently across client/server.
- Interceptor ordering: logging/security interceptors should be added before `create()`.
- MTOM not working: enable MTOM on both server and client; check attachment size limits on the HTTP server.

## 🤝 Contributing
- Issues/PRs welcome. When adding new configuration knobs (security, interceptors, transports), update examples above and cross‑link RULES/GUIDES.

## 📝 License
- Apache 2.0 — see `LICENSE`.
