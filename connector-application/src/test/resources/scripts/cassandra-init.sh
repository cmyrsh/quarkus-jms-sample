cat >/import.cql <<EOF
DROP keyspace KSUT;
CREATE keyspace KSUT with replication = {'class':'SimpleStrategy', 'replication_factor' : 1};
USE KSUT;
CREATE TABLE transaction (core_id text, pacs_tx_id text, flow_id text, sequence_id int, state text, handler text, attributes map<text, text> static, primary key ((core_id, pacs_tx_id, sequence_id), flow_id));
CREATE TABLE transaction_payload ( pacs_tx_id text, payload_ns text, payload text, primary key (pacs_tx_id));
CREATE TABLE transaction_progress ( pacs_tx_id text, sequence_id int, handler text, connector_attributes map <text, text>, primary key (pacs_tx_id, sequence_id));
EOF

# You may add some other conditionals that fits your stuation here
until cqlsh -f /import.cql; do
  echo "cqlsh: Cassandra is unavailable to initialize - will retry later"
  sleep 2
done &

exec /docker-entrypoint.sh "$@"