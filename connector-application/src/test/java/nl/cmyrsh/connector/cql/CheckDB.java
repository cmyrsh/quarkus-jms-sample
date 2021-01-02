package nl.cmyrsh.connector.cql;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class CheckDB {

    @Inject
    QuarkusCqlSession session;

    public static final String GET_FROM_TRANSACTION = "select * from transaction where core_id = 'default' and pacs_tx_id = ? and sequence_id = 0";
    public static final String GET_FROM_TRANSACTION_PAYLOAD = "select * from transaction_payload where pacs_tx_id = ?";
    public static final String GET_FROM_TRANSACTION_PROGRESS = "select * from transaction_progress where pacs_tx_id = ?";

    private PreparedStatement getTrasaction;
    private PreparedStatement getTransactionPayload;
    private PreparedStatement getTransactionProgress;

    void prepareStatements(@Observes StartupEvent onStart) {
        getTrasaction = session.prepare(GET_FROM_TRANSACTION);
        getTransactionPayload = session.prepare(GET_FROM_TRANSACTION_PAYLOAD);
        getTransactionProgress = session.prepare(GET_FROM_TRANSACTION_PROGRESS);
    }

    public String getReport(String transactionId) {
        String getTx = session.execute(getTrasaction.bind(transactionId)).all().stream().map(row -> row.toString())
                .collect(Collectors.joining("\\n"));

        String getTxPayload = session.execute(getTransactionPayload.bind(transactionId)).all().stream()
                .map(row -> row.toString()).collect(Collectors.joining("\\n"));
        String getTxProgress = session.execute(getTransactionProgress.bind(transactionId)).all().stream()
                .map(row -> row.toString()).collect(Collectors.joining("\\n"));

        return new StringBuilder().append("=================================================").append("\\n")
                .append("----------------------TransactionData-------------------").append(getTx).append("\\n")
                .append("----------------------Payload-------------------").append("\\n").append(getTxPayload)
                .append("\\n").append("----------------------Progress-------------------").append("\\n")
                .append(getTxProgress).append("\\n").append("=================================================")
                .toString();

    }
}
