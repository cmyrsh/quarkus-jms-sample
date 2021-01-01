package nl.cmyrsh.connector.sender;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Session;

import cmyrsh.message.Context;
import cmyrsh.message.Message;
import cmyrsh.message.Payload;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;


@ApplicationScoped
public class MessageSender {

    private static final Logger LOG = Logger.getLogger(MessageSender.class.getName());

    private final ConnectionFactory connectionFactory;

    private final Random random = new Random();

    private JMSContext jmsContext;

    @Inject
    MessageSender(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        LOG.info("Created MessageSender");
    }

    void startup(@Observes StartupEvent event) throws InterruptedException {
        LOG.info("Creating jmsContext");
        jmsContext = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
        LOG.info("Created jmsContext");
    }

    public void sendNewMessage(String queue) throws JMSException, IOException {


        final BytesMessage createBytesMessage = jmsContext.createBytesMessage();

        Message newMessage = newMessage();

        final byte[] data = newMessage.toByteBuffer().flip().array();

        LOG.info(String.format("Sending message of length %d", data.length));

        createBytesMessage.writeBytes(data);




        createBytesMessage.setStringProperty("id", newMessage.getContext().getPacsTxId().toString());

        jmsContext.createProducer().send(jmsContext.createQueue(queue), createBytesMessage);

        

    }

    private Message newMessage() throws IOException {
        LOG.info("Creating newMessage");

        final String pacsTxId = UUID.randomUUID().toString().replaceAll("\\-", "").toUpperCase();
        final String pacsTxMsgId = UUID.randomUUID().toString().replaceAll("\\-", "").toUpperCase();
        final BigDecimal amt = new BigDecimal("" + (Math.abs(random.nextDouble()) * 100));

        return Message.newBuilder()
        .setId(pacsTxId+"0")
        .setContext(
            Context.newBuilder()
                    .setPacsTxId(pacsTxId)
                    .setCoreId("default")
                    .setFlowId(pacsTxId+"F0")
                    .setAttributes(Map.of("BPT", "CT", "Channel", "X"))
                    .build())
        .setMessagePayload(
            Payload.newBuilder()
                    .setNamespace("pacs0080102")
                    .setValue(readPacs008(pacsTxMsgId, amt, pacsTxId))
                    .build())
        .build();
    }

    private String readPacs008(String id, BigDecimal amt, String ref) throws IOException {
        return Files.readString(Path.of("target/test-classes/sample.xml"))
        .replace("$id", id)
        .replace("$amt", amt.toString())
        .replace("$ref", ref);
    }
    void onStop(@Observes ShutdownEvent ev) {
        jmsContext.close();
    }

    
}
