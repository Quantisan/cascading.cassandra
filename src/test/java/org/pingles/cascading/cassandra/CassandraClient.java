package org.pingles.cascading.cassandra;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KsDef;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SchemaDisagreementException;
import org.apache.thrift.TException;
import org.apache.cassandra.thrift.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CassandraClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraClient.class);

    private Cassandra.Client client;
    private final TTransport transport;

    public CassandraClient(String rpcHost, Integer rpcPort) {
        TSocket socket = new TSocket(rpcHost, rpcPort);
        transport = new TFramedTransport(socket);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        client = new Cassandra.Client(protocol);
        LOGGER.info("CassandraClient connecting to {}:{}", rpcHost, rpcPort);
    }

    public String createKeyspace(String keyspaceName) throws TException, SchemaDisagreementException, InvalidRequestException {
        List<CfDef> columnFamilyDefs = new ArrayList<CfDef>();

        KsDef ksDef = new KsDef(keyspaceName, "org.apache.cassandra.locator.SimpleStrategy", columnFamilyDefs);
        ksDef.strategy_options = new HashMap<String, String>() {{
            put("replication_factor", "1");
        }};

        client.send_system_add_keyspace(ksDef);
        return this.client.recv_system_add_keyspace();
    }

    public List<KsDef> describeKeyspaces() throws TException, InvalidRequestException {
        client.send_describe_keyspaces();
        return client.recv_describe_keyspaces();
    }

    public void open() throws InterruptedException, TTransportException {
        transport.open();
    }

    public void close() {
        transport.close();
    }

    public boolean keyspaceExists(String keyspaceName) throws TException, InvalidRequestException {
        List<KsDef> keyspaces = describeKeyspaces();
        for (KsDef ksDef : keyspaces) {
            if (ksDef.name.equals(keyspaceName)) {
                return true;
            }
        }
        return false;
    }

    public boolean columnFamilyExists(String keyspace, String columnFamily) throws TException, NotFoundException, InvalidRequestException {
        List<CfDef> columnFamilies = listColumnFamilies(keyspace);
        for (CfDef cfDef : columnFamilies) {
            if (cfDef.name.equals(columnFamily)) {
                return true;
            }
        }
        return false;
    }

    private List<CfDef> listColumnFamilies(String keyspace) throws TException, NotFoundException, InvalidRequestException {
        client.send_describe_keyspace(keyspace);
        KsDef ksDef = client.recv_describe_keyspace();
        return ksDef.cf_defs;
    }

    public String createColumnFamily(String keyspace, String name) throws TException, SchemaDisagreementException, InvalidRequestException {
        CfDef cfDef = new CfDef();
        cfDef.name = name;
        cfDef.keyspace = keyspace;

        client.send_set_keyspace(keyspace);
        client.recv_set_keyspace();

        client.send_system_add_column_family(cfDef);
        String s = client.recv_system_add_column_family();
        return s;
    }
}
