package com.viasat.ksqlstudio.model.query;

public class QueryBody {


    private String ksql;
    private StreamProperties streamsProperties;


    public String getKsql() {
        return ksql;
    }

    public void setKsql(String ksql) {
        this.ksql = ksql;
    }

    public StreamProperties getStreamsProperties() {
        return streamsProperties;
    }

    public void setStreamsProperties(StreamProperties streamsProperties) {
        this.streamsProperties = streamsProperties;
    }
}
