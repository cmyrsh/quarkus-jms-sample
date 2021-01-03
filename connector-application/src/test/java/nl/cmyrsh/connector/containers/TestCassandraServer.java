package nl.cmyrsh.connector.containers;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

@Testcontainers
public class TestCassandraServer implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(TestCassandraServer.class.getName());

    public static final Integer CASSANDRA_PORT = 9042;
    public static final String CASSANDRA_DC = "datacenter1";
    public static final String CASSANDRA_KEYSPACE = "KSUT";

    public static GenericContainer cassandra = new GenericContainer<>(DockerImageName.parse("cassandra:latest"))
            .withExposedPorts(CASSANDRA_PORT).withEnv("CASSANDRA_DC", CASSANDRA_DC)
            .withFileSystemBind("target/test-classes/scripts/cassandra-init.sh", "/cassandra-init.sh", BindMode.READ_ONLY)
            //.withFileSystemBind("target/test-classes/data", "/var/lib/cassandra", BindMode.READ_WRITE)
            .withCommand("sh /cassandra-init.sh")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(20)));

    @Override
    public Map<String, String> start() {
        System.out.println("Starting Cassandra " + Path.of("scripts/cassandra-init.sh").toAbsolutePath().toString());
        cassandra.start();
        cassandra.followOutput(new Slf4jLogConsumer(LOG, Boolean.TRUE));

        System.out.println("Started Cassandra");
        return Map.of(
            "quarkus.cassandra.contact-points", cassandra.getHost() + ":" + cassandra.getMappedPort(CASSANDRA_PORT),
            "quarkus.cassandra.local-datacenter", CASSANDRA_DC,
            "quarkus.cassandra.keyspace", CASSANDRA_KEYSPACE);
    }

    @Override
    public void stop() {
        //LOG.info(cassandra.getLogs());
        cassandra.stop();
        LOG.info("Stopped Cassandra");

    }

}
