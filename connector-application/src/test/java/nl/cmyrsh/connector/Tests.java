package nl.cmyrsh.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jms.JMSException;

import com.google.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import io.quarkus.test.junit.QuarkusTest;
import nl.cmyrsh.connector.sender.MessageSender;


@QuarkusTest
public class Tests {

    private static final Logger LOG = Logger.getLogger(Tests.class.getName());

    MessageSender messageSender;

    @ConfigProperty(name = "jms.queue", defaultValue = "default.queue")
    String queue;




    @Inject
    Tests(MessageSender messageSender) {
        this.messageSender = messageSender;
    }


    @BeforeEach
    public void testArtemis() throws InterruptedException {
        LOG.info("Waiting before running test");
    }

    @Test
    public void testSimpleSend() throws JMSException, IOException, InterruptedException {

        assertEquals(queue, "mytestqueue");

        LOG.info("Getting MessageSender");

        assertNotNull(messageSender);

        messageSender.sendNewMessage(queue);

        assertEquals(Boolean.TRUE, Boolean.TRUE);

    }


}