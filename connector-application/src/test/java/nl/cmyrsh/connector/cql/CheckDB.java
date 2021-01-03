package nl.cmyrsh.connector.cql;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.type.codec.registry.MutableCodecRegistry;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;

import io.quarkus.runtime.StartupEvent;
import nl.cmyrsh.connector.util.codec.Avro2CassandraTEXTCodec;

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
        ((MutableCodecRegistry) session.getContext().getCodecRegistry()).register(new Avro2CassandraTEXTCodec());
        getTrasaction = session.prepare(GET_FROM_TRANSACTION);
        getTransactionPayload = session.prepare(GET_FROM_TRANSACTION_PAYLOAD);
        getTransactionProgress = session.prepare(GET_FROM_TRANSACTION_PROGRESS);
    }

    public String getReport(String transactionId) {


        ResultSet transactions = session.execute(getTrasaction.bind(transactionId));
        ResultSet transactionPayloads = session.execute(getTransactionPayload.bind(transactionId));
        ResultSet transactionProgress = session.execute(getTransactionProgress.bind(transactionId));

        List<ColumnDefinition> txColums = StreamSupport
            .stream(transactions.getColumnDefinitions().spliterator(), false)
            .collect(Collectors.toList());
        List<ColumnDefinition> txPldColums = StreamSupport
            .stream(transactionPayloads.getColumnDefinitions().spliterator(), false)
            .collect(Collectors.toList());
        List<ColumnDefinition> txPrgColums = StreamSupport
            .stream(transactionProgress.getColumnDefinitions().spliterator(), false)
            .collect(Collectors.toList());

        String getTx = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(
                                transactions.iterator(), Spliterator.ORDERED), 
                                false)
                                .map(r -> extract(r, txColums))
                                .collect(Collectors.joining("\n"));

        String getTxPayload = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(
                                transactionPayloads.iterator(), Spliterator.ORDERED), 
                                false)
                                .map(r -> extract(r, txPldColums))
                                .collect(Collectors.joining("\n"));
        String getTxProgress = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(
                                transactionProgress.iterator(), Spliterator.ORDERED), 
                                false)
                                .map(r -> extract(r, txPrgColums))
                                .collect(Collectors.joining("\n"));

        return new StringBuilder().append("=================================================").append("\n")
                .append("----------------------TransactionData-------------------").append(getTx).append("\n")
                .append("----------------------Payload-------------------").append("\n").append(getTxPayload)
                .append("\n").append("----------------------Progress-------------------").append("\n")
                .append(getTxProgress).append("\n").append("=================================================")
                .toString();

    }


    private String extract(Row row, List<ColumnDefinition> columns) {
        return columns.stream()
            .map(c -> row.getObject(c.getName()))
            .map(rv -> rv.toString())
            .collect(Collectors.joining(","));
    }
}
