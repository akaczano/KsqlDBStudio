package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.App;
import com.viasat.ksqlstudio.model.query.StreamProperties;
import com.viasat.ksqlstudio.model.statement.*;
import com.viasat.ksqlstudio.service.InformationService;
import com.viasat.ksqlstudio.service.QueryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import javafx.stage.FileChooser;



import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * The controller class for the application. Handles all UI events. Also handles
 * connection to ksqlDB and starts threads for query processing and list refreshes.
 */
public class Controller implements Initializable, RequestSource {

    // Rest services
    private InformationService infoService;
    private QueryService queryService;


    /* UI */

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

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox cbxOffset;

    private List<CodeEditor> codeEditors;

    // Threads
    private Thread updateThread;
    private QueryThread queryThread;
    private StatementThread statementThread;


    /**
     * Called when the program is started up. Does some UI initialization and starts
     * threads.
     * @param location
     * @param resources
     */
    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        codeEditors = new ArrayList<>();

        // Load settings

        if (App.getSettings().getKsqlHost() != null) {
            String hostname = App.getSettings().getKsqlHost();
            infoService = new InformationService(hostname);
            queryService = new QueryService(hostname);

            if (updateLists()) {
                runButton.setDisable(false);
            }
        }

        for (int i = 0; i < App.getSettings().getOpenFiles().size(); i++) {
            String f = App.getSettings().getOpenFiles().get(i);
            File file = new File(f);
            if (file.exists()) {
                openFile(file);
            }
            else {
                App.getSettings().getOpenFiles().remove(f);
            }
        }
        if (editorPane.getTabs().size() < 1) {
            runButton.setVisible(false);
            cbxOffset.setVisible(false);
        }
        updateThread = new Thread(this::refresh);
        updateThread.start();
        resultsTable.getStyleClass().add("data-table");
    }

    /**
     * Adds a tab to the code editor.
     * @param filename The name of the file
     * @param filePath The full path to the file
     */
    private void addTab(String filename, String filePath) {
        Tab defaultTab = new Tab(filename);
        AnchorPane defaultPane = new AnchorPane();
        CodeEditor editor = new CodeEditor();
        editor.setId(filePath);
        editor.getStylesheets().add("styles.css");
        editor.getStyleClass().add("html-editor");
        defaultPane.getChildren().add(editor);
        codeEditors.add(editor);
        AnchorPane.setBottomAnchor(editor, 0D);
        AnchorPane.setTopAnchor(editor, 0D);
        AnchorPane.setLeftAnchor(editor, 0D);
        AnchorPane.setRightAnchor(editor, 0D);
        defaultTab.setClosable(true);
        defaultTab.setContent(defaultPane);
        editorPane.getTabs().add(defaultTab);
        ContextMenu menu = new ContextMenu();
        MenuItem closeItem = new MenuItem("Close");
        closeItem.setOnAction(e -> {
            int index = editorPane.getTabs().indexOf(defaultTab);
            codeEditors.remove(index);
            editorPane.getTabs().remove(defaultTab);
            App.getSettings().getOpenFiles().remove(editor.getId());
        });
        menu.getItems().add(closeItem);
        defaultTab.setContextMenu(menu);

        if (editorPane.getTabs().size() == 1) {
            runButton.setVisible(true);
            cbxOffset.setVisible(true);
        }
    }

    /**
     * Called when the 'Connect to ksqlDB menu item is clicked'
     * Prompts the user for a hostname and then attempts to connect
     * to the server.
     */
    public void onConnect() {
        // Create dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Connect to ksqlDB");
        dialog.setContentText("Hostname: ");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            infoService = new InformationService(result.get());
            queryService = new QueryService(result.get());
            if (updateLists()) {
                runButton.setDisable(false);
                // If it worked, we want to update the app settings to save
                // the new hostname
                App.getSettings().setKsqlHost(result.get());
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection failed");
                alert.setContentText("Failed to connect to ksqlDB server on " + result.get());
                alert.showAndWait();
            }
        }
    }

    /**
     * Called when the 'Run' or 'Terminate' button is clicked.
     * Handled query execution and termination.
     */
    public void runButtonClick() {
        if (runButton.getText().equals("Run")) {
            String queryText = codeEditors.get(editorPane.getSelectionModel().getSelectedIndex())
                    .getSelectedText();

            if (queryText.length() < 1) {
                queryText = codeEditors.get(editorPane.getSelectionModel().getSelectedIndex()).getText();
            }

            errorLabel.setVisible(false);
            resultsTable.setVisible(true);
            if (queryText.startsWith("SELECT")) {
                resultsTable.getItems().clear();
                resultsTable.getColumns().clear();
                runButton.setText("Terminate");
                StreamProperties props = getProperties();
                queryThread = new QueryThread(resultsTable,
                        queryService.streamQuery(queryText, props), this);
                queryThread.start();
            } else {
                runButton.setText("Terminate");
                statementThread = new StatementThread(queryText,
                        infoService, this);
                statementThread.start();
            }
        } else {
            if (statementThread != null && statementThread.isRunning()) {
                statementThread.interrupt();
            } else if (queryThread != null && queryThread.isRunning()) {
                queryThread.stopRunning();

            }
            runButton.setText("Run");
        }
    }

    /**
     * Updates the values stored in the topics, streams, tables, and connectors lists.
     * This is also the main way connection to ksqlDB is verified.
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
                Platform.runLater(() -> {
                    StatementResponse connectors = (StatementResponse) connectorsResult;
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

    /**
     * Called from the query thread when there is an error with the
     * query. Displays the error on the screen.
     * @param message The error text received from ksqlDB
     */
    @Override
    public void onError(String message) {
        // This is called from a separate thread, so we can't update the UI
        // directly.
        Platform.runLater(() -> {
            this.errorLabel.setText(message);
            this.errorLabel.setVisible(true);
            this.resultsTable.setVisible(false);
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
        while (true) {
            try {
                boolean connected = updateLists();
                if (!connected) {
                    runButton.setDisable(true);
                } else {
                    runButton.setDisable(false);
                }
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Called when the File->New menu item is clicked. Creates a new tab
     * in the code editor.
     */
    public void onNew() {
        String name = "query1";

        // Compute tab name (harder than it sounds)
        Optional<Tab> maxQuery = editorPane.getTabs().stream()
                .filter(t -> t.getText().startsWith("query"))
                .reduce((max, t) -> max.getText().compareTo(t.getText()) > 0 ? max : t);

        if (maxQuery.isPresent()) {
            try {
                int num = Integer.parseInt(maxQuery.get().getText().substring(5)) + 1;
                name = "query" + num;
            } catch (NumberFormatException e) {}
        }
        // Add tab to editor
        addTab(name, "");
    }

    /**
     * Called when the File->Open menu item is clicked. Opens a file
     * dialog and adds the selected files to the code editor. It also adds them
     * to the app settings so they will be automatically opened the next time the
     * app is started.
     */
    public void onOpen() {
        FileChooser fileChooser = new FileChooser();
        List<File> files = fileChooser.showOpenMultipleDialog(null);
        for (File file : files) {
            if (file != null && file.exists()) {
                openFile(file);
                App.getSettings().getOpenFiles().add(file.getAbsolutePath());
            }
        }
    }

    /**
     * Does the actual work of reading the text in the file
     * and adding a tab to the screen.
     * @param file The file to open
     */
    private void openFile(File file) {
        addTab(file.getName(), file.getAbsolutePath());
        CodeEditor text = codeEditors.get(codeEditors.size() - 1);
        StringBuilder builder = new StringBuilder("");
        try {
            for (String line : Files.readAllLines(file.toPath())) {
                builder.append(line).append("\n");
            }
            text.appendText(builder.toString());
            editorPane.getSelectionModel().selectLast();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when File-Save is clicked. Writes the context of the code editor to the appropriate
     * file.
     */
    public void onSave() {
        CodeEditor editor = codeEditors.get(editorPane.getSelectionModel().getSelectedIndex());
        String filePath = editor.getId();
        File file;
        if (filePath.length() == 0) {
            FileChooser fileDialog = new FileChooser();
            file = fileDialog.showSaveDialog(null);
            App.getSettings().getOpenFiles().add(file.getAbsolutePath());
        } else {
            file = new File(filePath);
        }
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            Files.writeString(file.toPath(), editor.getText(), StandardOpenOption.TRUNCATE_EXISTING);
            editorPane.getSelectionModel().getSelectedItem().setText(file.getName());
            editor.setId(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

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
    public void destroy() {
        this.updateThread.interrupt();
        if (queryThread != null && queryThread.isRunning()) {
            queryThread.stopRunning();
        }
        if (statementThread != null && statementThread.isRunning()) {
            statementThread.interrupt();
        }
        for (CodeEditor area : codeEditors) {
            area.destroy();
        }
    }

}
