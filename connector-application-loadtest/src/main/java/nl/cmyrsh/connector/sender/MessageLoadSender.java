package nl.cmyrsh.connector.sender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Session;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import cmyrsh.message.Context;
import cmyrsh.message.Message;
import cmyrsh.message.Payload;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class MessageLoadSender implements Runnable{

    private static final Logger LOG = Logger.getLogger(MessageLoadSender.class.getName());

    @Inject
    ConnectionFactory connectionFactory;

    private final Random random = new Random();

    private JMSContext jmsContext;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    @ConfigProperty(name = "jms.queue", defaultValue = "default.queue")
    String queue;

    @ConfigProperty(name = "load.delay.millies", defaultValue = "2000")
    Long delay;


    void startup(@Observes StartupEvent event) { 
        LOG.info("Creating jmsContext");
        jmsContext = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
        scheduler.scheduleWithFixedDelay(this, 0L, delay, TimeUnit.MILLISECONDS);
    }

    private void sendNewMessage() throws JMSException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        BytesMessage createBytesMessage = jmsContext.createBytesMessage();

        Message newMsg = newMessage();

        final byte[] data = newMsg.toByteBuffer().flip().array();

        LOG.info(String.format("Sending message of length %d", data.length));

        createBytesMessage.writeBytes(data);

        createBytesMessage.setStringProperty("msg_id", newMsg.getContext().getPacsTxId().toString());

        jmsContext.createProducer().send(jmsContext.createQueue(queue), createBytesMessage);
    }


    private Message newMessage() throws IOException {
        LOG.info("Creating newUser");

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
        return Files.readString(Path.of("sample.xml"))
        .replace("$id", id)
        .replace("$amt", amt.toString())
        .replace("$ref", ref);
    }

    void onStop(@Observes ShutdownEvent ev) {
        jmsContext.close();
    }

    @Override
    public void run() {
        try {
            sendNewMessage();
        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }
        
    }

    
}
