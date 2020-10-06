package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.model.statement.ResponseBase;
import com.viasat.ksqlstudio.model.statement.StatementError;
import com.viasat.ksqlstudio.service.InformationService;

import java.io.IOException;

public class StatementThread extends Thread {

    private String statement;
    private InformationService infoService;
    private RequestSource source;
    private volatile boolean running = false;

    public boolean isRunning() {
        return running;
    }


    public StatementThread(String statement, InformationService service, RequestSource source) {
        this.statement = statement;
        this.source = source;
        this.infoService = service;
    }

    @Override
    public void run() {
        running = true;
        try {
            ResponseBase result = infoService.executeCommand(statement, source.getProperties());
            if (result instanceof StatementError) {
                StatementError error = (StatementError) result;
                source.onError(error.getMessage());
            }
        } catch (InterruptedException | IOException e) {
            source.onError("Request Failed!");
        }
        source.onComplete();
        running = false;
    }
}
