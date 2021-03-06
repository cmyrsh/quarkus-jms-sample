package nl.cmyrsh.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import javax.inject.Inject;
import javax.jms.JMSException;


import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import nl.cmyrsh.connector.sender.MessageSender;
import nl.cmyrsh.connector.containers.TestArtemisServer;
import nl.cmyrsh.connector.containers.TestCassandraServer;
import nl.cmyrsh.connector.cql.CheckDB;


@QuarkusTest
@QuarkusTestResource(TestArtemisServer.class)
@QuarkusTestResource(TestCassandraServer.class)
@TestInstance(Lifecycle.PER_CLASS)
public class Tests {

    private static final Logger LOG = LoggerFactory.getLogger(Tests.class);


    @ConfigProperty(name = "jms.queue", defaultValue = "default.queue")
    String queue;
    
    final MessageSender messageSender;

    final CheckDB checkDB;

    @Inject
    Tests(MessageSender messageSender, CheckDB checkDB) {
        this.checkDB = checkDB;
        System.out.println("MessageSender " + messageSender);
        this.messageSender = messageSender;
    }

    @BeforeAll
    public void prepareCassandra() {
       LOG.info("BeforeAll");
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

        String newMsgId = messageSender.sendNewMessage(queue);

        assertEquals(Boolean.TRUE, Boolean.TRUE);

        sleep(1000L);

        LOG.info(checkDB.getReport(newMsgId));


    }


    private void sleep(Long millies) {
        try {
            Thread.sleep(millies);
        } catch (Exception e) {
            throw new RuntimeException("Thread Interrupted");
        }
    }


}
