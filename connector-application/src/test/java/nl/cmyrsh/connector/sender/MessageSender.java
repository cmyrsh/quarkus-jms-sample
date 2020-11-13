package nl.cmyrsh.connector.sender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import nl.cmyrsh.messages.User;


@Testcontainers
@ApplicationScoped
public class MessageSender {

    private static final Logger LOG = Logger.getLogger(MessageSender.class.getName());

    private final ConnectionFactory artemisConnectionFactory;

    private final Random random = new Random();

    private JMSContext jmsContext;

    private final DatumWriter<User> datumWriter = new SpecificDatumWriter<>(User.class);

    @Inject
    MessageSender(ConnectionFactory artemisConnectionFactory) {
        this.artemisConnectionFactory = artemisConnectionFactory;
        LOG.info("Created MessageSender");
    }

    void startup(@Observes StartupEvent event) throws InterruptedException {
        LOG.info("Creating jmsContext");
        jmsContext = artemisConnectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
        LOG.info("Created jmsContext");
    }

    public void sendNewMessage(String queue) throws JMSException, IOException {

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

        

        LOG.info("Creating newUser");

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

    
}