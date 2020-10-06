package com.viasat.ksqlstudio.model.statement;

public class StatementResponse extends ResponseBase {


    private Stream[] streams;
    private Table[] tables;
    private Topic[] topics;
    private Connector[] connectors;

    public Topic[] getTopics() {
        return topics;
    }

    public void setTopics(Topic[] topics) {
        this.topics = topics;
    }

    public Stream[] getStreams() {
        return streams;
    }

    public void setStreams(Stream[] streams) {
        this.streams = streams;
    }

    public Table[] getTables() {
        return tables;
    }

    public void setTables(Table[] tables) {
        this.tables = tables;
    }

    public Connector[] getConnectors() {
        return connectors;
    }

    public void setConnectors(Connector[] connectors) {
        this.connectors = connectors;
    }
}
