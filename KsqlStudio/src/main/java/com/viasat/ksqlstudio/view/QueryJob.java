package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.model.query.QueryChunk;
import com.viasat.ksqlstudio.model.query.StreamHeader;
import com.viasat.ksqlstudio.model.statement.StatementError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class QueryJob implements Callable<Boolean> {
    private final Stream<Object> stream;
    private final RequestSource source;

    private volatile boolean stopped = false;

    public void stop() {
        stopped = true;
    }

    public QueryJob(Stream<Object> stream, RequestSource source) {
        this.stream = stream;
        this.source = source;
    }

    @Override
    public Boolean call() {
        try {
            stream.takeWhile(p -> !stopped).forEach((obj) -> {
                if (stopped) {
                    stream.close();
                }
                if (obj != null) {
                    if (obj instanceof StreamHeader) {
                        StreamHeader header = (StreamHeader) obj;
                        List<String> fields = parseHeader(header.getSchema());
                        source.setColumns(fields);
                    } else if (obj instanceof QueryChunk) {
                        QueryChunk chunk = (QueryChunk) obj;
                        if (chunk.getRow() != null) {
                            source.addRow(chunk.getRow().getColumns());
                        }
                    } else if (obj instanceof StatementError) {
                        StatementError err = (StatementError) obj;
                        source.onError(err.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            return false;
        }

        return true;
    }


    private List<String> parseHeader(String schema) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        String word = "";
        for (int i = 0; i < schema.length(); i++) {
            char c = schema.charAt(i);
            if (c == '`') {
                if (inQuotes) {
                    fields.add(word);
                    word = "";
                    inQuotes = false;
                } else {
                    inQuotes = true;
                }
            } else if (inQuotes) {
                word += c;
            }
        }
        return fields;
    }
}
