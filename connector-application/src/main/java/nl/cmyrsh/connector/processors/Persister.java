package nl.cmyrsh.connector.processors;

import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.type.codec.registry.MutableCodecRegistry;
import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cmyrsh.message.ConnectorResponse;
import cmyrsh.message.Message;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import nl.cmyrsh.connector.util.codec.Avro2CassandraTEXTCodec;

@ApplicationScoped
public class Persister {

        private static final Logger LOG = LoggerFactory.getLogger(Persister.class);

        @Inject
        QuarkusCqlSession session;

        private PreparedStatement transactionInsert;
        private PreparedStatement transactionPayloadInsert;
        private PreparedStatement transactionProgressInsert;

        void startSequence(@Observes StartupEvent startupEvent) {

                ((MutableCodecRegistry) session.getContext().getCodecRegistry()).register(new Avro2CassandraTEXTCodec());
/* 
                CREATE TABLE transaction (core_id text, pacs_tx_id text, flow_id text, sequence_id int, state text, handler text, attributes map<text, text> static, primary key ((core_id, pacs_tx_id, sequence_id), flow_id));
                CREATE TABLE transaction_payload ( pacs_tx_id text, payload_ns text, payload text, primary key (pacs_tx_id));
                CREATE TABLE transaction_progress ( pacs_tx_id text, sequence_id int, handler text, connector_attributes map <text, text>, primary key ((pacs_tx_id), sequence_id));
                
 */
                transactionInsert = session.prepare(
                                "INSERT INTO transaction(core_id, pacs_tx_id, flow_id, sequence_id, state, handler, attributes) values (?, ?, ?, ?, ?, ?, ?)");
                transactionPayloadInsert = session.prepare(
                                "INSERT INTO transaction_payload(pacs_tx_id, payload_ns, payload) values (?, ?, ?)");
                transactionProgressInsert = session.prepare(
                                "INSERT INTO transaction_progress(pacs_tx_id, sequence_id, handler, connector_attributes) values (?, ?, ?, ?)");

                                boolean wasApplied = session.execute(
                        
                        transactionInsert.bind(
                                "A",
                                "B", 
                                "C", 
                                0, 
                                "NEW", 
                                "None",
                                Map.of()
                                ) 
        
                                        
                        ).wasApplied();

                        LOG.info("transactionInsert Applied : {}", wasApplied);

                        boolean wasApplied2 = session
                                .execute(
                                        transactionPayloadInsert.bind(
                                                "A",
                                                "PACS08", 
                                                "Payload"))
                                .wasApplied();

                                LOG.info("transactionPayloadInsert Applied : {}", wasApplied2);

        }

        @ConsumeEvent("persist0")
        Boolean persistNew(Message message) {

                LOG.info("Persisting message {} on Nodes {}", message, session.getMetadata().getNodes());

                Boolean txupdated = session.execute(
                        
                        transactionInsert.bind(
                                message.getContext().getCoreId(),
                                message.getContext().getPacsTxId(), 
                                message.getContext().getFlowId(), 
                                0, 
                                "NEW", 
                                "None",
                                message.getContext().getAttributes()
                                ) 
        
                                        
                        ).wasApplied();

                LOG.info("transactionInsert applied {}", txupdated);



                Boolean payloadinserted = session
                                .execute(
                                        transactionPayloadInsert.bind(
                                                message.getContext().getPacsTxId(),
                                                message.getMessagePayload().getNamespace(), 
                                                message.getMessagePayload().getValue()))
                                .wasApplied();
                LOG.info("transactionPayloadInsert applied {}", payloadinserted);                



                return txupdated && payloadinserted;

        }

        public void updateTransaction(Message message) {
                final Integer sequence = message.getResponses().size();
                final ConnectorResponse lastResponse = message.getResponses().get(sequence - 1);

                session.execute(
                        transactionProgressInsert.bind(message.getContext().getPacsTxId(), sequence,
                        lastResponse.getStage(), lastResponse.getEventData())        
                );

        }

}
