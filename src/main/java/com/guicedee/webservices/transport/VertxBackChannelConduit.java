package com.guicedee.webservices.transport;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Back-channel conduit that captures CXF's response output into a byte array
 * so it can be written back to the Vert.x response.
 */
public class VertxBackChannelConduit extends AbstractConduit {

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(VertxBackChannelConduit.class.getName());

    private final Message inMessage;

    public VertxBackChannelConduit(Message inMessage) {
        super(new EndpointReferenceType());
        this.inMessage = inMessage;
    }

    @Override
    protected java.util.logging.Logger getLogger() {
        return LOG;
    }

    @Override
    public void prepare(Message message) throws IOException {
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, responseStream);

        // Store the back channel stream on the exchange for the destination to retrieve
        if (inMessage.getExchange() != null) {
            inMessage.getExchange().setOutMessage(message);
        }
    }

    @Override
    public void close(Message message) throws IOException {
        OutputStream out = message.getContent(OutputStream.class);
        if (out != null) {
            out.flush();
            out.close();
        }
        super.close(message);
    }
}
