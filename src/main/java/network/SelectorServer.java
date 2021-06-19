package network;

import network.util.AppProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static network.util.Logger.log;

/**
 * Server on the selector with channels.
 * Java nio.
 *
 * Communication with the database is not implemented.
 */
public class SelectorServer {
    private Selector selector;
    AppProperties properties = new AppProperties();
    // Array of all connected channels.
    private final ArrayList<SocketChannel> clientBase = new ArrayList<>();
    // <channel, array of client messages>
    private final Map<SocketChannel, LinkedList<String>> pendingData = new HashMap<>();

    public SelectorServer() throws IOException {
    }

    public void start() throws IOException {
        // Registered clients to receive messages.
        selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(properties.getHostname(), properties.getPort()));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // Waiting for events on channels.
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove(); // Deleting the processed event.
                if (!key.isValid()) continue;
                if (key.isAcceptable())
                    accept(key);
                else if (key.isReadable()) {
                    EchoProtocol newConnection = new EchoProtocol();
                    newConnection.read(key);
                } else if (key.isWritable()) {
                    EchoProtocol newConnection = new EchoProtocol();
                    newConnection.write(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        pendingData.put(channel, new LinkedList<>());
        clientBase.add(channel);

        log("connected " + channel.socket().getRemoteSocketAddress());
    }

    /**
     * Class for working with a new connection (channel).
     */
    public class EchoProtocol {
        private static final char GS = 0x1D;
        //private static final char RS = 0x1E;
        private SocketChannel channel;
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());

        private void read(SelectionKey key) throws IOException {
            channel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int numRead = channel.read(byteBuffer);
            if (numRead == -1) {   // If client is disconnected.
                removeClient(key);
                return;
            }

            byte[] data = new byte[numRead];
            System.arraycopy(byteBuffer.array(), 0, data, 0, numRead);
            String gotData = new String(data);
            LinkedList<String> dataList = pendingData.get(channel);
            dataList.add(gotData);
            pendingData.replace(channel, dataList);

            key = channel.keyFor(selector);
            key.interestOps(SelectionKey.OP_WRITE);
        }

        private void removeClient(SelectionKey key) throws IOException {
            pendingData.remove(channel);
            clientBase.remove(channel);
            log("finished " + channel.socket().getRemoteSocketAddress());
            channel.close();
            key.cancel();
        }

        private void write(SelectionKey key) throws IOException {
            channel = (SocketChannel) key.channel();
            LinkedList<String> dataList = pendingData.get(channel);
            while (!dataList.isEmpty()) {
                String data = dataList.get(0);
                parseMessage(data, key);
                dataList.remove(0);
            }

            key = channel.keyFor(selector);
            key.interestOps(SelectionKey.OP_READ);
        }

        private void parseMessage(String msg, SelectionKey key) throws IOException {
            msg = msg.substring(0, msg.length() - 1);
            var array = msg.split(String.valueOf(GS));

            if (array[0].equals("T_REGISTER")) {
                log("register client: " + array[1]);
                String text = "<" + array[1] + " connected to the server>";
                sendAll(text, array[1]);
            } else if (array[0].equals("T_MESSAGE")) {
                log("received from " + channel.socket().getRemoteSocketAddress() + ": " + array[2]);
                sendAll(array[2], array[1]);
            } else if (array[0].equals("T_DELETE_ACCOUNT")) {
                removeClient(key);  // ??
            }
        }

        private void sendAll(String msg, String clientName) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            byteBuffer.clear();
            String serverMsg = msg + " | from <" + clientName + "> " + formatter.format(date) + "\n";
            byteBuffer.put(serverMsg.getBytes(StandardCharsets.UTF_8));
            byteBuffer.flip();
            clientBase.forEach(socketChannel -> {
                try {
                    socketChannel.write(byteBuffer);
                    byteBuffer.flip();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
