package network.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Watchdog timer that sends a message to the client
 * every 5 seconds to check the connection.
 */
public class Watchdog extends Thread {
    public String check = "true";
    private Socket socket;
    private final PrintWriter out;

    public Watchdog(Socket socket) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true,
                StandardCharsets.UTF_8);
    }

    @Override
    public void run() {
        while (true) {
            out.println("connection check");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!check.equals("true")) {
                if (socket != null && socket.isConnected()) {
                    try {
                        out.println("Connection interrupted!");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            setCheck("false");
        }
    }

    public void setCheck(String check) {
        this.check = check;
    }
}
