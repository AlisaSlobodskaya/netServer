# The server part of the network console chat

Serves client connections:
* accepts registration;
* accepts messages and sends them to all participants (including
  the sender). Attaches the time of the message and the name of the sender to the sent messages.

The server is made in two versions:
1) using blocking sockets and multithreaded processing for messaging, supports communication with the database;
2) using single-threaded processing on the selector, doesn't support communication with the database.

User data is stored in the postgresql database. Data about the tables used in the db.sql.

Simple Messaging Protocol: <br>
`value GS value GS value RS` <br>

GS and RS are 1D and 1E ASCII characters. <br>
The first value is always the package type, such as "T_REGISTER "or
"T_MESSAGE".

The configurable program parameters are located in my. properties:
* server address and port;
* type of network exchange server;
* data for connecting to the database.