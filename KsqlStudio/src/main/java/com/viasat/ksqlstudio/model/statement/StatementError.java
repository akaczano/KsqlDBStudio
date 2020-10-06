package com.viasat.ksqlstudio.model.statement;

public class StatementError extends ResponseBase {
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
