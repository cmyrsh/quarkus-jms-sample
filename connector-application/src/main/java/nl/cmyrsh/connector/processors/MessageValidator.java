package nl.cmyrsh.connector.processors;

import java.util.Objects;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.jms.IllegalStateException;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;

import io.quarkus.vertx.ConsumeEvent;
import nl.cmyrsh.messages.User;

@ApplicationScoped
public class MessageValidator {

    private static final Logger LOG = Logger.getLogger(MessageValidator.class.getName());


    final DatumReader<nl.cmyrsh.messages.User> messageReader = new SpecificDatumReader<User>(User.class);

    @ConsumeEvent("process0")
    public String validateAndForward(byte[] jmsMessageBody) {
        try {
            if(Objects.isNull(jmsMessageBody)) throw new IllegalStateException("Got Null JMS Message");
            
            LOG.info(String.format("Processing Message of length %d", jmsMessageBody.length));
            

            User user = messageReader.read(null, DecoderFactory.get().binaryDecoder(jmsMessageBody, null));

            // if(Integer.parseInt(jmsMessageBody) % 2 == 0) {
            //     throw new RuntimeException(String.format("Bad Message %s", jmsMessageBody));
            // }

            return user.getName().toString();
            
        } catch (Exception e) {
            LOG.severe(String.format("Error in validateAndForward %s", e.getMessage()));
            throw new RuntimeException("Unable to parse JMS Message", e);
        }
    }
}
