package com.viasat.ksqlstudio.model.query;

public class StreamHeader {

    private String queryId;
    private String schema;

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}

