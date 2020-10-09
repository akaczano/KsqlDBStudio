# KsqlDBStudio

This is provides a GUI-based tool for editing and executing ksqlDB queries.

### Getting Started
**Prerequisites:** Java version 12 and up, Maven, git

1. git clone https://github.com/akaczano/KsqlDBStudio
2. cd KsqlDBStudio/KsqlStudio
3. mvn javafx:run


### Current to-do list:
- GUI support for DROP, TERMINATE,and DESCRIBE queries. For instance, you should be able to right-click on a stream and click 'Drop' or 'Properties'
- Improvements for files editing: track changes to files and notify if file is unsaved when closing, reorder tabs, close all except, etc.
- Display statistics on messages processed during a query. This might be a toolbar that shows the number of messages processed, the throughput, etc.
- Display status of server, status of connectors
- Use the schema to actually convert the data received into the appropriate datatype. This will increase support for operations like sorting inside the GUI
- Wizards for creating streams, connectors
- Auto-complete?
- Dark mode?

