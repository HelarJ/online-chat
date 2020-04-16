package ee.oop.onlinechat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Sender {

    private Map<SocketChannel, ClientInfo> socketClientMap;
    private ChatLogHandler chatLogHandler;

    public Sender(Map<SocketChannel, ClientInfo> socketClientMap) {
        this.socketClientMap = socketClientMap;
        this.chatLogHandler = new ChatLogHandler();
    }
    public void sendMsgWithSenderToChannel(String channelName, ClientInfo saatja, String text) { // sama asi mis sendToAll, aga selle asemel et saadab byteBufferi siis saadab stringi
        Message msg = new Message(channelName, saatja.getName(), text);
        chatLogHandler.logMessage(channelName, msg);
        String msgToSend = msg.toString();
        byte[] bytes = msgToSend.getBytes(StandardCharsets.UTF_8);
        ByteBuffer data = ByteBuffer.wrap(bytes);
        Server.logger.info(String.format("Sending: %s to connected clients", new String(data.array())));
        socketClientMap.forEach((c, d) -> { //saadab igale ühendusele, mis on kaardistatud dataMapperis.
            try {
                if (d.isInChannel(channelName)){ //saadab ühendusele ainult siis kui ta on kanalis.
                    c.write(data);
                    Server.logger.info(String.format("Sent: \"%s\" to %s at %s", new String(data.array()), socketClientMap.get(c).getName(), c.getRemoteAddress()));
                }
            } catch (IOException e) {
                Server.logger.severe("Error sending to channel: " + e.getMessage());
            } finally {
                data.rewind();
            }
        });
    }

    public void sendTextBack(String text, SocketChannel c) {
        byte[] tekstBytes = text.getBytes(StandardCharsets.UTF_8);
        ByteBuffer data = ByteBuffer.wrap(tekstBytes);
        try {
            c.write(data);
            Server.logger.info(String.format("Sent: \"%s\" to %s at %s", new String(data.array()), socketClientMap.get(c).getName(), c.getRemoteAddress()));
        } catch (IOException e) {
            Server.logger.severe("Error sending to channel: " + e.getMessage());
        } finally {
            data.rewind();
        }
    }

    /**
     * Sends (up to) @param amount of last logged messages to specified channel.
     * @param c channel to send to.
     * @param channelName name of the channel.
     * @param amount of messages to return to the specified channel.
     */
    public void sendChatLog(SocketChannel c, String channelName, int amount) {
        for (Message msg : chatLogHandler.getLastMessages(channelName, amount)) {
            sendTextBack(msg.toString(), c);
        }
    }

    public void addChannel(String channelName){
        chatLogHandler.addChannel(channelName);
    }


      /*private void sendToAll(ByteBuffer data) { // saab hiljem kasutada, kui saadame pilte ja asju
        dataMapper.forEach((c, d) -> { //saadab igale ühendusele, mis on kaardistatud dataMapperis.
            try {
                c.write(data);
                System.out.printf("Sent: %s to %s%n at %s%n", new String(data.array()), d.getNimi(), c.getRemoteAddress());
                data.rewind();
            } catch (IOException e) {
                System.out.println("Error sending to channel: " + e.getMessage());
            }
        });
    }*/
}
