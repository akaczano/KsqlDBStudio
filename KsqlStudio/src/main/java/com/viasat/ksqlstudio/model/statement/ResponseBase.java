package com.viasat.ksqlstudio.model.statement;

public abstract class ResponseBase {
    private String statementText;
    private Warning[] warnings;

    public String getStatementText() {
        return statementText;
    }

    public void setStatementText(String statementText) {
        this.statementText = statementText;
    }

    public Warning[] getWarnings() {
        return warnings;
    }

    public void setWarnings(Warning[] warnings) {
        this.warnings = warnings;
    }
}
