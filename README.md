# KsqlDBStudio

This is provides a GUI-based tool for editing and executing ksqlDB queries.

### What you cannot do right now
- Run queries that process millions of records. The application is not prepared for that yet and if you try it, bad things will happen.
- Run any query that doesn't start with CREATE or SELECT. I currently intend to add support for such queries through the user interface, not through the code editor.

### Current to-do list:
- GUI support for DROP, TERMINATE,and DESCRIBE queries. For instance, you should be able to right-click on a stream and click 'Drop' or 'Properties'
- Improvements for filed editing: track changes to files and notify if file is unsaved when closing, reorder tabs, close all except, etc.
- Display statistics on messages processed during a query. This might be a toolbar that shows the number of messages processed, the throughput, etc.
- Display status of server, status of connectors
- Optimize for large amounts of data: make sure not to overpopulate the table view
- Use the schema to actually convert the data received into the appropriate datatype. This will increase support for operations like sorting inside the GUI
- Wizards for creating streams, connectors
- Auto-complete?
- Dark mode?

