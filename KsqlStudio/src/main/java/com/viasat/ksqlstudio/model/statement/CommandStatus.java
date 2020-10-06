package com.viasat.ksqlstudio.model.statement;

public class CommandStatus {
    private Status status;
    private String message;
    private long commandSequenceNumber;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCommandSequenceNumber() {
        return commandSequenceNumber;
    }

    public void setCommandSequenceNumber(long commandSequenceNumber) {
        this.commandSequenceNumber = commandSequenceNumber;
    }
}
