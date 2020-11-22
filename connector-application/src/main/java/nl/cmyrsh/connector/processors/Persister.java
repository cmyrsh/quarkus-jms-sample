package nl.cmyrsh.connector.processors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;

import io.quarkus.runtime.StartupEvent;

public class Persister {
    

    @Inject
    QuarkusCqlSession session;


    public static final String CREATE_TABLE_USER = "CREATE TABLE user ( id UUID PRIMARY KEY, name text, fav_number integer, fav_color text);";

    private PreparedStatement createUser;
    private PreparedStatement updateUser;
    private PreparedStatement deleteUser;
    private PreparedStatement getUser;

    void startSequence(@Observes StartupEvent startupEvent) {
        checkTables();
        prepareStatements();
    }


    private boolean checkTables() {
        return session.execute(CREATE_TABLE_USER).isFullyFetched();
    }

    private void prepareStatements() {
        createUser = session.prepare("ADD_USER");
        updateUser = session.prepare("UPDATE_USER");
        deleteUser = session.prepare("DELETE_USER");
        getUser = session.prepare("GET_USER");
    }


}
