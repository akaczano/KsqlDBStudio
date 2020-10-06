package com.viasat.ksqlstudio.model.query;

public class QueryRow {
    private Object[] columns;

    public Object[] getColumns() {
        return columns;
    }

    public void setColumns(Object[] columns) {
        this.columns = columns;
    }
}
