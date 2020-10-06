package com.viasat.ksqlstudio;

import com.viasat.ksqlstudio.model.query.QueryChunk;
import com.viasat.ksqlstudio.model.query.StreamHeader;
import com.viasat.ksqlstudio.model.query.StreamProperties;
import com.viasat.ksqlstudio.model.statement.CommandResponse;
import com.viasat.ksqlstudio.model.statement.ResponseBase;
import com.viasat.ksqlstudio.model.statement.StatementError;
import com.viasat.ksqlstudio.model.statement.StatementResponse;
import com.viasat.ksqlstudio.service.InformationService;
import com.viasat.ksqlstudio.service.QueryService;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class AppTest {
    @Test
    public void testLists() throws InterruptedException, IOException  {
        InformationService service = new InformationService("http://localhost:8088");
        Object result = service.executeStatement("LIST STREAMS;");
        assertFalse(result instanceof StatementError);
        StatementResponse response = (StatementResponse) result;
        assertNotNull(response);
        assertNotNull(response.getStreams());
        assertTrue(response.getStreams().length > 0);

        Object object = service.executeStatement("LIST TABLES;");
        assertFalse(object instanceof StatementError);
        StatementResponse resp = (StatementResponse) object;
        assertNotNull(resp);
        assertNull(resp.getStreams());
        assertNotNull(resp.getTables());
        assertTrue(resp.getTables().length > 0);

        Object o = service.executeStatement("LIST CONNECTORS;");

    }

    @Test
    public void testDropStream() throws InterruptedException, IOException {
        InformationService service = new InformationService("localhost:8088");
        assertTrue(service.dropStream("transactions_west"));
    }

    @Test
    public void testCreateStream() throws InterruptedException, IOException  {
        InformationService service = new InformationService("localhost:8088");
        String command = "CREATE STREAM transactions_west AS SELECT * FROM transactions WHERE STORER='WEST' EMIT CHANGES;";
        ResponseBase result = service.executeCommand(command, new StreamProperties());

        assertFalse(result instanceof StatementError);
        CommandResponse response = (CommandResponse) result;
        System.out.println(response.getCommandStatus().getMessage());
    }

    @Test
    public void testQuery() throws InterruptedException, IOException {
        QueryService service = new QueryService("localhost:8088");
        String query = "SELECT * FROM transactions EMIT CHANGES LIMIT 20;";
        StreamProperties properties = new StreamProperties();
        properties.setOffsetRest("earliest");
        Stream<Object> stream = service.streamQuery(query, properties);
        stream.forEach((chunk) -> {
            if (chunk != null) {
                if (chunk instanceof StreamHeader) {
                    StreamHeader header = (StreamHeader)chunk;
                    System.out.println(header.getSchema());
                }
                else {
                    QueryChunk row = (QueryChunk)chunk;
                    if (row.getErrorMessage() != null) {
                        System.out.println(row.getErrorMessage());
                    }
                    else if (row.getFinalMessage() != null) {
                        System.out.println(row.getFinalMessage());
                    }
                    else {
                        System.out.println(Arrays.toString(row.getRow().getColumns()));
                    }
                }
            }
        });
    }
}
