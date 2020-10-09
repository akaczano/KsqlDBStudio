package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.model.statement.ResponseBase;
import com.viasat.ksqlstudio.model.statement.StatementError;
import com.viasat.ksqlstudio.service.InformationService;

import java.io.IOException;
import java.util.concurrent.Callable;

public class StatementJob implements Callable<Boolean> {

    private final String statement;
    private final RequestSource source;
    private final InformationService service;


    public StatementJob(InformationService service, String statement, RequestSource source) {
        this.service = service;
        this.statement = statement;
        this.source = source;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            ResponseBase result = service.executeCommand(statement, source.getProperties());
            if (result instanceof StatementError) {
                StatementError error = (StatementError) result;
                source.onError(error.getMessage());
            }
        } catch (InterruptedException | IOException e) {
            source.onError("Request Failed!");
            return false;
        }
        source.onComplete();
        return true;
    }
}
