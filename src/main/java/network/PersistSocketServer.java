package network;

import network.util.AppProperties;
import network.util.Watchdog;
import postgresJDBC.Client;
import postgresJDBC.ClientDAO;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;

import static network.util.Logger.log;

/**
 * Multithreaded server on sockets.
 * Java io.
 *
 * Supports storing client data in the database:
 * login, password hash, password salt, session key (not implemented).
 *
 * Sends the last 20 messages from the server to each new connection.
 */
public class PersistSocketServer implements Runnable {
    // Array of all connected sockets.
    private static final ArrayList<EchoProtocol> clientBase = new ArrayList<>();
    AppProperties properties = new AppProperties();
    Connection connection = DriverManager.getConnection(properties.getUrlForSQL(), properties.getSQLUsername(), properties.getSQLPass());
    ClientDAO clientDAO = new ClientDAO(connection);

    public PersistSocketServer() throws IOException, SQLException {
    }

    @Override
    public void run() {
        var pool = Executors.newCachedThreadPool();
        try (var serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(properties.getHostname(), properties.getPort()));
            while (true) {
                var clientSocket = serverSocket.accept();
                log("connected " + clientSocket);
                EchoProtocol newConnection = new EchoProtocol(clientSocket);
                pool.submit(newConnection);
                clientBase.add(newConnection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Class for working with a new connection (socket).
     */
    private class EchoProtocol implements Runnable {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private static final char GS = 0x1D;
        private static final char RS = 0x1E;
        private final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        private Date date = new Date(System.currentTimeMillis());

        private EchoProtocol(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true,
                    StandardCharsets.UTF_8);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                    StandardCharsets.UTF_8));
        }

        @Override
        public void run() {
            /*
            try {
                checking();     // Starting the watchdog timer.
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
            try (socket) {
                tryRun();       // Start receiving messages.
            } catch (Exception e) {
                e.printStackTrace();
            }
            log("finished " + socket);
        }

        private void checking() throws IOException {
            Watchdog watchdog = new Watchdog(socket);
            watchdog.start();
        }

        private synchronized void tryRun() throws Exception {
            while (true) {
                char msg = ' ';
                var baos = new ByteArrayOutputStream();
                while (msg != RS) {
                    int msgInt;
                    msgInt = in.read();
                    msg = (char) msgInt;
                    baos.write(msg);
                }
                var text = baos.toString(StandardCharsets.UTF_8);
                text = text.substring(0, text.length() - 1);
                parseMessage(text);
            }
        }

        private void parseMessage(String msg) {
            var array = msg.split(String.valueOf(GS));
            if (array[0].equals("T_MESSAGE")) {
                log("received from " + socket + ": " + array[2]);
                sendAll(array[2], array[1]);
            } else if (array[0].equals("T_REGISTER")) {
                registerOrLogin(array);
            } else if (array[0].equals("T_DELETE_ACCOUNT")) {
                deleteAccount(array);
                log("delete client: " + array[1]);
                closeSocketIfRequired();
            }
            //else if (array[0].equals("T_WATCHDOG")) watchdog.setCheck("true");
        }

        private void registerOrLogin(String[] array) {
            int randValue = (int) (Math.random() * 101);
            Client client = createClient(array[1], array[2].hashCode(), String.valueOf(randValue).hashCode(), randValue);
            if (client.getLogin() == null) {      // If client is in database.
                passwordCheck(array);
            } else {
                clientDAO.create(client);
                log("register client: " + array[1]);
                send("You have successfully registered!");
                sendTwentyLatestMsg();
                sendAll("<" + array[1] + " connected to the server>", array[1]);
            }
        }

        private void passwordCheck(String[] array) {
            if (clientDAO.read(array[1]).getPassHash() == array[2].hashCode()) {
                send("Welcome to server " + array[1] + "!");
                log("enter client: " + array[1]);
                sendTwentyLatestMsg();
                sendAll("<" + array[1] + " connected to the server>", array[1]);
            } else {
                out.println("Invalid password. Try again");
                //closeSocketIfRequired();
            }
        }

        private void deleteAccount(String[] array) {
            Client client = clientDAO.read(array[1]);
            clientDAO.delete(client);
        }

        // Send the last 20 messages.
        private void sendTwentyLatestMsg() {
            String[] array;
            array = clientDAO.getMsg();
            String msg = String.join(", ", array);
            send("20 latest msg: " + msg);
        }

        private Client createClient(String login, int passHash, int passSalt, int sessionKey) {
            Client client = new Client();
            if (clientDAO.read(login).getId() == -1) {
                client.setLogin(login);
                client.setPassHash(passHash);
                client.setPassSalt(passSalt);
                client.setSessionKey(sessionKey);
            }
            return client;
        }

        private void sendAll(String msg, String name) {
            String text = msg + " | from <" + name + "> " + formatter.format(date);
            try {
                for (EchoProtocol client : clientBase) {
                    client.send(text);
                }
                clientDAO.writeMsg(text);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        private void send(String text) {
            out.println(text);
        }

        private void closeSocketIfRequired() {
            if (socket != null && socket.isConnected()) {
                try {
                    socket.close();
                    clientBase.remove(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
