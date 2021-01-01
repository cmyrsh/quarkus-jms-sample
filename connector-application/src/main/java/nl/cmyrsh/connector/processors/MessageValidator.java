package nl.cmyrsh.connector.processors;

import java.nio.ByteBuffer;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.IllegalStateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cmyrsh.message.Message;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class MessageValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MessageValidator.class.getName());

    @Inject
    EventBus eventBus;


    @ConsumeEvent("process0")
    public String validateAndForward(byte[] jmsMessageBody) {
        try {
            if(Objects.isNull(jmsMessageBody)) throw new IllegalStateException("Got Null JMS Message");
            
            LOG.info(String.format("Processing Message of length %d", jmsMessageBody.length));

            Message message = Message.fromByteBuffer(ByteBuffer.wrap(jmsMessageBody));

            eventBus.request("persist0", message)
            .onItem()
            .transform(t -> t.body())
            .subscribe();

            return "OK";
            
        } catch (Exception e) {
            LOG.error(String.format("Error in validateAndForward %s", e.getMessage()));
            throw new RuntimeException("Unable to parse JMS Message", e);
        }
    }
}
