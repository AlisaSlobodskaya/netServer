package network.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppProperties {
    Properties properties = new Properties();
    FileInputStream in = new FileInputStream("my.properties");

    public AppProperties() throws IOException {
        properties.load(in);
    }

    public String getUrlForSQL() {
        return properties.getProperty("urlForSQl");
    }

    public String getSQLUsername() {
        return properties.getProperty("SQLUsername");
    }

    public String getSQLPass() {
        return properties.getProperty("SQLPass");
    }

    public String getServerType() {
        return properties.getProperty("serverType");
    }

    public String getHostname() {
        return properties.getProperty("hostname");
    }

    public Integer getPort() {
        return Integer.valueOf(properties.getProperty("port"));
    }
}
