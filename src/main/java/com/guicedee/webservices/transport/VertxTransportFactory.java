package com.guicedee.webservices.transport;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CXF transport factory that creates {@link VertxDestination} instances.
 * Registered with the CXF Bus to handle endpoint addresses using Vert.x as the HTTP transport.
 * <p>
 * This factory eliminates the need for any servlet container — SOAP requests flow
 * from Vert.x Router → VertxDestination → CXF interceptor chain → response back to Vert.x.
 */
public class VertxTransportFactory extends AbstractTransportFactory implements DestinationFactory {

    public static final String TRANSPORT_ID = "http://cxf.apache.org/transports/vertx";

    private static final Set<String> URI_PREFIXES = Set.of(
            "http://", "https://", "vertx://", "/"
    );

    private final Map<String, VertxDestination> destinations = new ConcurrentHashMap<>();

    public VertxTransportFactory() {
        super(List.of(TRANSPORT_ID));
    }

    @Override
    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        String address = endpointInfo.getAddress();
        return destinations.computeIfAbsent(address, addr -> {
            EndpointReferenceType ref = new EndpointReferenceType();
            AttributedURIType uri = new AttributedURIType();
            uri.setValue(addr);
            ref.setAddress(uri);
            return new VertxDestination(bus, ref, endpointInfo);
        });
    }

    @Override
    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

    /**
     * Returns the destination registered for the given address, or null.
     */
    public VertxDestination getDestinationForAddress(String address) {
        // Try exact match first
        VertxDestination dest = destinations.get(address);
        if (dest != null) {
            return dest;
        }
        // Try suffix match (path portion)
        for (Map.Entry<String, VertxDestination> entry : destinations.entrySet()) {
            if (address.endsWith(entry.getKey()) || entry.getKey().endsWith(address)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Returns all registered destinations.
     */
    public Map<String, VertxDestination> getDestinations() {
        return destinations;
    }
}
