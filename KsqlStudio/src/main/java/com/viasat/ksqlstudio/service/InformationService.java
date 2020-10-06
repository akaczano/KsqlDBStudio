package com.viasat.ksqlstudio.service;


import com.google.gson.Gson;
import com.viasat.ksqlstudio.model.query.QueryBody;
import com.viasat.ksqlstudio.model.query.StreamProperties;
import com.viasat.ksqlstudio.model.statement.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class InformationService {

    public static final String path = "/ksql";

    private final String hostname;

    public InformationService(String ksqlHostname) {
        hostname = ksqlHostname + path;
    }

    private Object execute(String statement, StreamProperties props,
                           Class<? extends ResponseBase> format)
            throws IOException, InterruptedException {
        QueryBody body = new QueryBody();
        body.setKsql(statement);
        body.setStreamsProperties(props);
        String raw = new Gson().toJson(body);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(hostname))
                .method("POST", HttpRequest.BodyPublishers.ofString(raw))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            Object[] information = (Object[]) new Gson().fromJson(response.body(), format.arrayType());
            if (information.length > 0) {
                return information[0];
            }
        } else {
            return new Gson().fromJson(response.body(), StatementError.class);
        }
        return null;
    }

    public ResponseBase executeStatement(String statement)
            throws IOException, InterruptedException {
        StreamProperties properties = new StreamProperties();
        Object result = execute(statement, properties, StatementResponse.class);
        if (result instanceof StatementResponse) {
            return (StatementResponse) result;
        } else {
            return (StatementError) result;
        }
    }

    public ResponseBase executeCommand(String command, StreamProperties props) throws IOException, InterruptedException {
        Object result = execute(command, props, CommandResponse.class);
        if (result instanceof CommandResponse) {
            return (CommandResponse) result;
        } else {
            return (StatementError) result;
        }
    }

    public boolean dropStream(String stream) throws IOException, InterruptedException {
        StreamProperties props = new StreamProperties();
        ResponseBase result = executeCommand("DROP STREAM " + stream + ";", props);
        if (result instanceof StatementError) {
            StatementError error = (StatementError) result;
            if (error.getMessage().contains("The following queries")) {
                int startIndex = error.getMessage().indexOf("CSAS");
                int stopIndex = startIndex + 4;
                while (error.getMessage().charAt(stopIndex) != ']') {
                    stopIndex++;
                }
                ResponseBase dropResult = executeCommand("TERMINATE "
                        + error.getMessage().substring(startIndex, stopIndex) + ";", props);
                if (!(dropResult instanceof StatementError)) {
                    dropStream(stream);
                }
            }
            return false;
        }
        return true;
    }


}
