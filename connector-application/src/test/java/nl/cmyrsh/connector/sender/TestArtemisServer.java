package nl.cmyrsh.connector.sender;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ApplicationScoped
public class TestArtemisServer {

    private static final Logger LOG = Logger.getLogger(TestArtemisServer.class.getName());

    @Container
    public GenericContainer artemis = new GenericContainer<>(DockerImageName.parse("vromero/activemq-artemis:2.11.0-alpine"))
        .withEnv("ARTEMIS_USERNAME", "quarkus")
        .withEnv("ARTEMIS_PASSWORD", "quarkus")
        .withExposedPorts(61616)
        .withPrivilegedMode(Boolean.TRUE)
        .waitingFor(Wait.forListeningPort());


    
        @Produces
        ConnectionFactory artemisConnectionFactory() {
            LOG.info(String.format("Starting Container .."));
            artemis.start();;
            LOG.info(String.format("Started Container tcp//%s:%d", artemis.getHost(), artemis.getMappedPort(61616)));
            return new ActiveMQJMSConnectionFactory(
                String.format("tcp://%s:%d", artemis.getHost(), artemis.getMappedPort(61616)),
                "quarkus",
                "quarkus");
        }
}
