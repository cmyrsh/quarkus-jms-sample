package nl.cmyrsh.connector.sender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
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

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import nl.cmyrsh.messages.User;

@ApplicationScoped
public class MessageLoadSender implements Runnable{

    private static final Logger LOG = Logger.getLogger(MessageLoadSender.class.getName());

    @Inject
    ConnectionFactory connectionFactory;

    private final Random random = new Random();

    private JMSContext jmsContext;

    private final DatumWriter<User> datumWriter = new SpecificDatumWriter<>(User.class);

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

        User newUser = newUser();
        
        BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(out, null);

        datumWriter.write(newUser, binaryEncoder);

        binaryEncoder.flush();

        out.close();

        final byte[] data = out.toByteArray();

        LOG.info(String.format("Sending message of length %d", data.length));

        createBytesMessage.writeBytes(data);




        createBytesMessage.setStringProperty("user_name", newUser.getName().toString());

        jmsContext.createProducer().send(jmsContext.createQueue(queue), createBytesMessage);
    }


    private User newUser() {
        LOG.info("Creating newUser");
        return User.newBuilder()
        .setName("Name " + random.nextGaussian())
        .setFavoriteNumber(random.nextInt())
        .setFavoriteColor("Green")
        .build();
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
