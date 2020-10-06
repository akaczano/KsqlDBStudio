package com.viasat.ksqlstudio.model.statement;

public class CommandResponse extends ResponseBase {

    private String commandId;
    private CommandStatus commandStatus;

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public CommandStatus getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(CommandStatus commandStatus) {
        this.commandStatus = commandStatus;
    }
}




