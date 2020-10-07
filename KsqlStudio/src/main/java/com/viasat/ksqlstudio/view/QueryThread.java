package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.model.query.QueryChunk;
import com.viasat.ksqlstudio.model.query.StreamHeader;
import com.viasat.ksqlstudio.model.statement.StatementError;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class is responsible for executing a query and continuously updating the
 * data grid view with data.
 */
public class QueryThread extends Thread {

    // The table view to update
    private final TableView<Object[]> view;
    // The stream of data coming from the server
    private final Stream<Object> stream;
    // The rows in the table
    private final ObservableList<Object[]> items;
    // A RequestSource object (this will be the Controller object with only a couple methods exposed)
    private final RequestSource errorView;

    // Date format for displaying ROWTIME field
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");

    // Maximum number of records to keep in the table
    private final int MAX_RECORDS = 2000;

    // Used to stop the thread, may not be necessary.
    private volatile boolean running = false;

    private int baseFontSize = 20;

    public boolean isRunning() {
        return running;
    }

    /**
     * Stops the thread
     */
    public void stopRunning() {
        running = false;
    }

    public QueryThread(TableView<Object[]> view, Stream<Object> stream, RequestSource errorView) {
        this.view = view;
        this.stream = stream;
        this.errorView = errorView;
        items = FXCollections.observableArrayList();
        view.setItems(items);

    }

    @Override
    public void run() {
        running = true;
        try {
            stream.forEach((obj) -> {
                if (!running) {
                    stream.close();
                }
                if (obj != null) {
                    if (obj instanceof StreamHeader) {
                        StreamHeader header = (StreamHeader) obj;
                        List<String> fields = parseHeader(header.getSchema());
                        for (int i = 0; i < fields.size(); i++) {
                            final int j = i;

                            TableColumn<Object[], String> col = new TableColumn<>(fields.get(i));
                            col.setStyle(String.format("-fx-font-size: %dpx;", (int)(this.baseFontSize * 1.25)));
                            if (fields.get(i).equals("ROWTIME") || fields.get(i).equals("WINDOWSTART")
                                    || fields.get(i).equals("WINDOWEND")) {
                                col.setCellValueFactory((TableColumn.CellDataFeatures<Object[], String> cellDataFeatures) ->
                                        new SimpleStringProperty(
                                                format.format(new Date((long) Double.parseDouble(cellDataFeatures.getValue()[j].toString())))
                                        )
                                );
                            } else {
                                col.setCellValueFactory((TableColumn.CellDataFeatures<Object[], String> cellDataFeatures) ->
                                        new SimpleStringProperty(cellDataFeatures.getValue()[j].toString())
                                );
                            }
                            col.setMinWidth(view.getWidth() / fields.size());
                            Platform.runLater(() -> view.getColumns().add(col));
                        }
                    } else if (obj instanceof QueryChunk) {
                        QueryChunk chunk = (QueryChunk) obj;
                        if (chunk.getRow() != null) {
                            items.add(0, chunk.getRow().getColumns());
                            if (items.size() > MAX_RECORDS) {
                                items.remove(items.size() - 1);
                            }
                        }
                    } else if (obj instanceof StatementError) {
                        StatementError err = (StatementError) obj;
                        Platform.runLater(() -> errorView.onError(err.getMessage()));
                    }
                }
            });
        } catch (
                Exception e) {
        }
        errorView.onComplete();
        running = false;
    }

    private List<String> parseHeader(String schema) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        String word = "";
        for (int i = 0; i < schema.length(); i++) {
            char c = schema.charAt(i);
            if (c == '`') {
                if (inQuotes) {
                    fields.add(word);
                    word = "";
                    inQuotes = false;
                } else {
                    inQuotes = true;
                }
            } else if (inQuotes) {
                word += c;
            }
        }
        return fields;
    }

    public void setBaseFontSize(int size) {
        this.baseFontSize = size;
        for (TableColumn t : view.getColumns()) {
            t.setStyle(String.format("-fx-font-size: %dpx;", (int)(size * 1.25)));
        }
    }

}
