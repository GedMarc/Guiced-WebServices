package com.guicedee.webservices.transport;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import org.apache.cxf.Bus;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A CXF {@link org.apache.cxf.transport.Destination} that receives SOAP requests from Vert.x
 * {@link RoutingContext} and processes them through CXF's interceptor chain without any servlet dependency.
 */
@Log4j2
public class VertxDestination extends AbstractDestination {

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(VertxDestination.class.getName());

    private final Bus bus;

    public VertxDestination(Bus bus, EndpointReferenceType ref, EndpointInfo endpointInfo) {
        super(ref, endpointInfo);
        this.bus = bus;
    }

    @Override
    protected java.util.logging.Logger getLogger() {
        return LOG;
    }

    @Override
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        return new VertxBackChannelConduit(inMessage);
    }

    /**
     * Handles an incoming Vert.x HTTP request by converting it into a CXF message,
     * processing it through the CXF interceptor chain, and writing the response back.
     */
    public void handleRequest(RoutingContext ctx) {
        try {
            Buffer body = ctx.body().buffer();
            byte[] requestBytes = body != null ? body.getBytes() : new byte[0];

            // Build inbound CXF message
            MessageImpl inMessage = new MessageImpl();
            inMessage.setContent(java.io.InputStream.class, new ByteArrayInputStream(requestBytes));
            inMessage.put(Message.HTTP_REQUEST_METHOD, ctx.request().method().name());
            inMessage.put(Message.REQUEST_URI, ctx.request().uri());
            inMessage.put(Message.PATH_INFO, ctx.request().path());
            inMessage.put(Message.QUERY_STRING, ctx.request().query());
            inMessage.put(Message.BASE_PATH, getAddress().getAddress().getValue());
            inMessage.put(Message.CONTENT_TYPE, ctx.request().getHeader("Content-Type"));

            // Copy HTTP headers into protocol headers
            Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ctx.request().headers().forEach(entry ->
                    headers.computeIfAbsent(entry.getKey(), k -> new java.util.ArrayList<>()).add(entry.getValue()));
            inMessage.put(Message.PROTOCOL_HEADERS, headers);

            // Set up exchange
            Exchange exchange = new ExchangeImpl();
            exchange.setInMessage(inMessage);
            exchange.put(Bus.class, bus);
            exchange.setDestination(this);
            inMessage.setExchange(exchange);

            // Prepare output capture
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            exchange.put(VertxDestination.class.getName() + ".responseStream", responseStream);
            exchange.put(VertxDestination.class.getName() + ".routingContext", ctx);

            // Trigger CXF processing
            getMessageObserver().onMessage(inMessage);

            // Get response from the back channel
            Message outMessage = exchange.getOutMessage();
            if (outMessage != null) {
                OutputStream out = outMessage.getContent(OutputStream.class);
                if (out instanceof ByteArrayOutputStream baos) {
                    byte[] responseBytes = baos.toByteArray();
                    String contentType = (String) outMessage.get(Message.CONTENT_TYPE);
                    Integer responseCode = (Integer) outMessage.get(Message.RESPONSE_CODE);

                    ctx.response()
                            .setStatusCode(responseCode != null ? responseCode : 200)
                            .putHeader("Content-Type", contentType != null ? contentType : "text/xml; charset=utf-8")
                            .end(Buffer.buffer(responseBytes));
                    return;
                }
            }

            // If output was written via back channel
            if (responseStream.size() > 0) {
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("Content-Type", "text/xml; charset=utf-8")
                        .end(Buffer.buffer(responseStream.toByteArray()));
            } else if (!ctx.response().ended()) {
                ctx.response().setStatusCode(500).end("SOAP processing error");
            }

        } catch (Exception e) {
            log.error("Error processing SOAP request: {}", ctx.request().path(), e);
            if (!ctx.response().ended()) {
                ctx.response().setStatusCode(500).end("Internal Server Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handles ?wsdl requests by routing through the full CXF interceptor chain,
     * which includes the WSDLGetInterceptor that recognizes GET ?wsdl requests.
     */
    public void handleWsdlRequest(RoutingContext ctx) {
        handleRequest(ctx);
    }

    @Override
    public void shutdown() {
        // cleanup if needed
    }
}
