package com.viasat.ksqlstudio.model.query;

public class QueryChunk {
    private String errorMessage;
    private String finalMessage;
    private QueryRow row;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFinalMessage() {
        return finalMessage;
    }

    public void setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
    }

    public QueryRow getRow() {
        return row;
    }

    public void setRow(QueryRow row) {
        this.row = row;
    }
}
