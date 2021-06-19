package network;

import network.util.AppProperties;

import java.io.IOException;
import java.sql.SQLException;

import static network.util.Logger.log;

public class Application {
    public static void main(String[] args) throws IOException, SQLException {
        AppProperties properties = new AppProperties();
        String serverType = properties.getServerType();
        log(" Server starting...");
        if (serverType.equals("persist socket server")) new PersistSocketServer().run();
        else if (serverType.equals("selector server")) new SelectorServer().start();
        log(" Server finished");
    }
}
