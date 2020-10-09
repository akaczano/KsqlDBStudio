package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.model.query.StreamProperties;

import java.util.List;

public interface RequestSource {
    void onError(String message);
    void onComplete();
    void setColumns(List<String> fields);
    void addRow(Object[] row);
    StreamProperties getProperties();

}
