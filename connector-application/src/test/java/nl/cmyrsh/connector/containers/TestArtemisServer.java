package nl.cmyrsh.connector.containers;

import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

@Testcontainers
public class TestArtemisServer implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(TestArtemisServer.class.getName());

    public static GenericContainer artemis = new GenericContainer<>(
            DockerImageName.parse("vromero/activemq-artemis:2.11.0-alpine")).withEnv("ARTEMIS_USERNAME", "quarkus")
                    .withEnv("ARTEMIS_PASSWORD", "quarkus").withExposedPorts(61616).withPrivilegedMode(Boolean.TRUE)
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(20)));


    @Override
    public Map<String, String> start() {
        System.out.println("Starting Artemis");
        artemis.start();
        System.out.println("Started Artemis @" + "tcp://" + artemis.getHost() + ":" + artemis.getMappedPort(61616));
        return Map.of("quarkus.artemis.url", "tcp://" + artemis.getHost() + ":" + artemis.getMappedPort(61616));
    }

    @Override
    public void stop() {
        artemis.stop();
        LOG.info("Stopped Artemis");
    }

}
