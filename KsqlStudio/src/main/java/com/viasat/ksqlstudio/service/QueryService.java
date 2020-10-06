package com.viasat.ksqlstudio.service;

import com.google.gson.Gson;
import com.viasat.ksqlstudio.model.query.QueryBody;
import com.viasat.ksqlstudio.model.query.QueryChunk;
import com.viasat.ksqlstudio.model.query.StreamProperties;
import com.viasat.ksqlstudio.model.query.StreamWrapper;
import com.viasat.ksqlstudio.model.statement.StatementError;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

public class QueryService {

    private static final String PATH = "/query";
    private String url;

    public QueryService(String hostname) {
        url = hostname + PATH;
    }

    public Stream<Object> streamQuery(String query, StreamProperties props) {
        HttpClient client = HttpClient.newHttpClient();
        QueryBody body = new QueryBody();
        body.setKsql(query);
        body.setStreamsProperties(props);
        String raw = new Gson().toJson(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .method("POST", HttpRequest.BodyPublishers.ofString(raw))
                .build();
        try {
            HttpResponse<Stream<String>> response =
                    client.send(request, HttpResponse.BodyHandlers.ofLines());
            return response.body().map((line) -> {
                if (line.startsWith("[")) {
                    line = line.substring(1);
                }
                if (line.endsWith(",") || line.endsWith("]")) {
                    line = line.substring(0, line.length() - 1);
                }
                try {
                    if (line.contains("header\":{")) {
                        StreamWrapper wrapper = new Gson().fromJson(line, StreamWrapper.class);
                        return wrapper.getHeader();
                    }
                    else if (line.contains("\"@type\":\"statement_error\"")) {
                        return new Gson().fromJson(line, StatementError.class);
                    }
                    else {
                        return new Gson().fromJson(line, QueryChunk.class);
                    }
                } catch(Exception e) {
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
