package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.App;
import com.viasat.ksqlstudio.AppSettings;
import com.viasat.ksqlstudio.model.query.StreamProperties;
import com.viasat.ksqlstudio.model.statement.*;
import com.viasat.ksqlstudio.service.InformationService;
import com.viasat.ksqlstudio.service.QueryService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The controller class for the application. Handles all UI events. Also handles
 * connection to ksqlDB and starts threads for query processing and list refreshes.
 */
public class Controller implements Initializable, RequestSource {

    /* Constants */

    // Refresh delay in seconds
    private static final int REFRESH_DELAY = 2;
    private static final int MAX_RECORDS = 5000;


    // Rest services
    private InformationService infoService;
    private QueryService queryService;


    /* UI */

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        scale();
        stage.setOnCloseRequest(this::destroy);
    }

    @FXML
    private TabPane editorPane;

    @FXML
    private Button runButton;

    @FXML
    private ListView<Topic> topicsView;
    private ObservableList<Topic> topicsList;

    @FXML
    private ListView<Stream> streamsView;
    private ObservableList<Stream> streamsList;

    @FXML
    private ListView<Table> tablesView;
    private ObservableList<Table> tablesList;

    @FXML
    private ListView<Connector> connectorsView;
    private ObservableList<Connector> connectorsList;

    @FXML
    private TableView<Object[]> resultsTable;
    private ObservableList<Object[]> resultsList;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox cbxOffset;

    @FXML
    private SplitPane splitPane1;

    @FXML
    private ScrollPane errorDisplay;

    private FileEditor fileEditor;


    // Date format for displaying ROWTIME field
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");

    // Executors
    private ScheduledExecutorService updateExecutor;
    private ExecutorService queryExecutor;
    private java.util.stream.Stream<Object> queryStream;
    private ExecutorService statementExecutor;

    /**
     * Called when the program is started up. Does some UI initialization and starts
     * threads.
     *
     * @param location
     * @param resources
     */
    @FXML
    public void initialize(URL location, ResourceBundle resources) {

        // Load settings
        if (App.getSettings().getKsqlHost() != null) {
            String hostname = App.getSettings().getKsqlHost();
            infoService = new InformationService(hostname);
            queryService = new QueryService(hostname);

            if (updateLists()) {
                runButton.setDisable(false);
            }
        }
        fileEditor = new FileEditor(this.editorPane);


        if (editorPane.getTabs().size() < 1) {
            runButton.setVisible(false);
            cbxOffset.setVisible(false);
        } else if (App.getSettings().getSelectedTab() >= 0 &&
                App.getSettings().getSelectedTab() < editorPane.getTabs().size()) {
            editorPane.getSelectionModel().select(App.getSettings().getSelectedTab());
        }
        App.getSettings().setSelectedTab(editorPane.getSelectionModel().getSelectedIndex());
        resultsTable.getStyleClass().add("data-table");
        resultsList = FXCollections.observableArrayList();
        resultsTable.setItems(resultsList);

        updateExecutor = Executors.newSingleThreadScheduledExecutor();
        updateExecutor.scheduleWithFixedDelay(this::refresh, REFRESH_DELAY,
                REFRESH_DELAY, TimeUnit.SECONDS);
    }


    /**
     * Called when the 'Connect to ksqlDB menu item is clicked'
     * Prompts the user for a hostname and then attempts to connect
     * to the server.
     */
    public void onConnect() {
        try {
            int baseFont = App.getSettings().getBaseFontSize();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/connect_dialog.fxml"));
            Parent parent = loader.load();
            Scene scene = new Scene(parent, baseFont * 25, baseFont * 15);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Connect server");
            stage.initModality(Modality.APPLICATION_MODAL);
            ConnectController controller = loader.getController();
            controller.initialize(stage);
            stage.showAndWait();
            infoService = new InformationService(App.getSettings().getKsqlHost());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the 'Run' or 'Terminate' button is clicked.
     * Handles query execution and termination.
     */
    public void runButtonClick() {
        if (runButton.getText().equals("Run")) {
            String queryText = fileEditor.getSelectedText();

            if (queryText.length() < 1) {
                queryText = fileEditor.getText();
            }

            errorDisplay.setVisible(false);
            resultsTable.setVisible(true);
            runButton.setText("Terminate");
            if (queryText.startsWith("SELECT")) {
                resultsTable.getItems().clear();
                resultsTable.getColumns().clear();
                this.queryStream = queryService.streamQuery(queryText, getProperties());
                queryExecutor = Executors.newSingleThreadExecutor();
                queryExecutor.submit(new QueryJob(queryStream, this));

            } else {
                statementExecutor = Executors.newSingleThreadExecutor();
                statementExecutor.submit(new StatementJob(infoService, queryText, this));
            }
        } else {
            if (statementExecutor != null && !statementExecutor.isTerminated()) {
                statementExecutor.shutdownNow();
            } else if (queryExecutor != null && !queryExecutor.isTerminated()) {
                queryStream.close();
                queryExecutor.shutdown();
            }
            runButton.setText("Run");
        }
    }

    /**
     * Updates the values stored in the topics, streams, tables, and connectors lists.
     * This is also the main way connection to ksqlDB is verified.
     *
     * @return True if the server was queried successfully, false otherwise.
     */
    private boolean updateLists() {
        if (infoService == null) {
            return false;
        }
        try {
            ResponseBase streamsResult = infoService.executeStatement("LIST STREAMS;");
            ResponseBase topicsResult = infoService.executeStatement("LIST TOPICS;");
            ResponseBase tablesResult = infoService.executeStatement("LIST TABLES;");
            ResponseBase connectorsResult = infoService.executeStatement("LIST CONNECTORS;");
            if (!(streamsResult instanceof StatementError) && !(topicsResult instanceof StatementError)
                    && !(tablesResult instanceof StatementError) && !(connectorsResult instanceof StatementError)) {
                StatementResponse streams = (StatementResponse) streamsResult;
                StatementResponse topics = (StatementResponse) topicsResult;
                StatementResponse tables = (StatementResponse) tablesResult;
                StatementResponse connectors = (StatementResponse) connectorsResult;
                Platform.runLater(() -> {
                    streamsList = FXCollections.observableArrayList(streams.getStreams());
                    streamsView.setItems(streamsList);
                    topicsList = FXCollections.observableArrayList(topics.getTopics());
                    topicsView.setItems(topicsList);
                    tablesList = FXCollections.observableArrayList(tables.getTables());
                    tablesView.setItems(tablesList);
                    connectorsList = FXCollections.observableArrayList(connectors.getConnectors());
                    connectorsView.setItems(connectorsList);
                });
                return true;
            }
            return false;
        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            //e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setColumns(List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            final int j = i;

            TableColumn<Object[], String> col = new TableColumn<>(fields.get(i));
            col.setStyle(String.format("-fx-font-size: %dpx;", App.getSettings().getBaseFontSize()));
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
            col.setMinWidth(resultsTable.getWidth() / fields.size());
            Platform.runLater(() -> resultsTable.getColumns().add(col));
        }
    }

    @Override
    public void addRow(Object[] row) {
        Platform.runLater(() -> {
            this.resultsList.add(0, row);
            if (this.resultsList.size() > MAX_RECORDS) {
                this.resultsList.remove(this.resultsList.size() - 1);
            }
        });
    }

    /**
     * Called from the query thread when there is an error with the
     * query. Displays the error on the screen.
     *
     * @param message The error text received from ksqlDB
     */
    @Override
    public void onError(String message) {
        // This is called from a separate thread, so we can't update the UI
        // directly.
        Platform.runLater(() -> {
            this.errorLabel.setText(message);
            this.errorDisplay.setVisible(true);
            this.resultsTable.setVisible(false);
            this.runButton.setText("Run");
        });
    }

    /**
     * Called from the query thread when an operation completes.
     * Sets the text of the run button from 'Terminate' to 'Run'
     * and updates the list views in case the query was a 'CREATE'
     * statement.
     */
    public void onComplete() {
        Platform.runLater(() -> {
            this.runButton.setText("Run");
            this.updateLists();
        });
    }

    /**
     * Called from the query threads to get the streams properties for the query.
     * Will add 'auto.offset.reset'='earliest' to the query if the 'From beginning'
     * box is checked.
     *
     * @return A stream properties object.
     */
    public StreamProperties getProperties() {
        StreamProperties props = new StreamProperties();
        if (cbxOffset.isSelected()) {
            props.setOffsetRest("earliest");
        }
        return props;
    }

    /**
     * Runs on the update thread. Wakes up every 3 seconds to verify the connection
     * and update the list views.
     */
    public void refresh() {
        boolean connected = updateLists();
        runButton.setDisable(!connected);
    }

    /**
     * Called when the File->New menu item is clicked. Creates a new tab
     * in the code editor.
     */
    public void onNew() {
        fileEditor.onNew();
    }

    /**
     * Called when the File->Open menu item is clicked. Opens a file
     * dialog and adds the selected files to the code editor. It also adds them
     * to the app settings so they will be automatically opened the next time the
     * app is started.
     */
    public void onOpen() {
        this.fileEditor.onOpen();
    }


    /**
     * Called when File-Save is clicked. Writes the context of the code editor to the appropriate
     * file.
     */
    public void onSave() {
        this.fileEditor.onSave();
    }

    /**
     * Called when File->Exit is clicked. Exits.
     */
    public void onExit() {
        Platform.exit();
    }

    /**
     * Called when the application is closed. Cleans up running threads
     * (including syntax highlighting threads).
     */
    public void destroy(WindowEvent event) {
        updateExecutor.shutdownNow();
        if (queryExecutor != null && !queryExecutor.isTerminated()) {
            queryExecutor.shutdownNow();
        }
        if (statementExecutor != null && !statementExecutor.isTerminated()) {
            statementExecutor.shutdownNow();
        }
        fileEditor.onClose(event);
        App.getSettings().setSplitPanePos(this.splitPane1.getDividerPositions()[0]);
        App.getSettings().setSelectedTab(this.editorPane.getSelectionModel().getSelectedIndex());
        AppSettings.save(App.getSettings());

    }


    public void onZoomIn() {
        if (App.getSettings().getBaseFontSize() < 30) {
            App.getSettings().setBaseFontSize(App.getSettings().getBaseFontSize() + 5);
            scale();
        }
    }

    public void onZoomOut() {
        if (App.getSettings().getBaseFontSize() > 5) {
            App.getSettings().setBaseFontSize(App.getSettings().getBaseFontSize() - 5);
            scale();
        }

    }

    public void scale() {
        if (this.stage != null) {
            splitPane1.setDividerPositions(App.getSettings().getSplitPanePos());
            this.stage.getScene().getRoot().setStyle(String.format("-fx-font-size: %dpx",
                    App.getSettings().getBaseFontSize()));
            this.fileEditor.scale(App.getSettings().getBaseFontSize());
            this.errorLabel.setStyle(String.format("-fx-font-size: %dpx;",
                    (int) (App.getSettings().getBaseFontSize() * 1.25)));
        }
    }


}
