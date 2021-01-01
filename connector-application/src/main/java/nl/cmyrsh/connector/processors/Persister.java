package nl.cmyrsh.connector.processors;

import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cmyrsh.message.ConnectorResponse;
import cmyrsh.message.Message;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;

public class Persister {

        private static final Logger LOG = LoggerFactory.getLogger(Persister.class);

        @Inject
        QuarkusCqlSession session;

        @ConfigProperty(name = "connector.cassandra.keyspace", defaultValue = "zero")
        String keyspace;

        private PreparedStatement transactionInsert;
        private PreparedStatement transactionPayloadInsert;
        private PreparedStatement transactionProgressInsert;

        void startSequence(@Observes StartupEvent startupEvent) {

                session.execute("use " + keyspace);

                transactionInsert = session.prepare(
                                "INSERT INTO transaction(core_id, pacs_tx_id, flow_id, sequence_id, state, handler, attributes) values (?, ?, ?, ?, ?, ?, ?)");
                transactionPayloadInsert = session.prepare(
                                "INSERT INTO transaction_payload(pacs_tx_id, payload_ns, payload) values (?, ?, ?)");
                transactionProgressInsert = session.prepare(
                                "INSERT INTO transaction_progress(pacs_tx_id, sequence_id, handler, connector_attributes) values (?, ?, ?, ?)");
        }

        @ConsumeEvent("persist0")
        Boolean persistNew(Message message) {

                session.execute(transactionInsert.bind(message.getContext().getCoreId(),
                                message.getContext().getPacsTxId(), message.getContext().getFlowId(), 0, "NEW", null,
                                message.getContext().getAttributes())).all().forEach(r -> LOG.info(":: ROW " + r.toString()));

                                final Integer sequence = message.getResponses().size();
                final ConnectorResponse lastResponse = message.getResponses().get(sequence - 1);

                session.execute(transactionPayloadInsert.bind(message.getContext().getPacsTxId(), sequence,
                                lastResponse.getStage(), lastResponse.getEventData()));

                return Boolean.TRUE;

        }

        void updateTransaction(Message message) {
                session.execute(transactionProgressInsert.bind(message.getContext().getPacsTxId(),
                                message.getMessagePayload().getNamespace(), message.getMessagePayload().getValue()));

        }

}
