package ee.oop.onlinechat;

import com.google.gson.Gson;
import ee.ut.oop.Message;

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
        String msgToSend = changeToJSON(msg);
        if (!msgToSend.endsWith("\r\n")) { //et client teaks, millal rida otsa saab, peavad sõnumid lõppema uue rea märkidega.
            msgToSend = msgToSend.stripTrailing() + "\r\n";
        }
        byte[] bytes = msgToSend.getBytes(StandardCharsets.UTF_8);
        Server.logger.info(String.format("Sending: %s to connected clients", msgToSend.stripTrailing()));
        socketClientMap.forEach((c, d) -> { //saadab igale ühendusele, mis on kaardistatud socketClientMapis.
            try {
                if (d.isInChannel(channelName)) { //saadab ühendusele ainult siis kui ta on kanalis.
                    if (saatja.isWs()){
                        ByteBuffer data = ByteBuffer.wrap(encode(bytes));
                        c.write(data);
                    } else {
                        ByteBuffer data = ByteBuffer.wrap(bytes);
                        c.write(data);
                    }
                    Server.logger.info(String.format("Sent: \"%s\" to %s at %s", text, socketClientMap.get(c).getName(), c.getRemoteAddress()));
                }
            } catch (IOException e) {
                Server.logger.severe("Error sending to channel: " + e.getMessage());
            }
        });
    }

    private String changeToJSON(Message msg){
        Gson gson = new Gson();
        return gson.toJson(msg)+"\r\n";
    }

    public void sendText(String text, SocketChannel c){
        Message msg = new Message("NOTICE", "Server", text);
        sendMessage(msg, c);
    }

    public void sendMessage(Message msg, SocketChannel c) {
        ClientInfo client = socketClientMap.get(c);
        String msgToSend = changeToJSON(msg);

        byte[] tekstBytes = msgToSend.getBytes(StandardCharsets.UTF_8);
        if (client.isWs()){
            tekstBytes = encode(tekstBytes);
        }

        ByteBuffer data = ByteBuffer.wrap(tekstBytes);

        try {
            c.write(data);
            Server.logger.info(String.format("Sent: \"%s\" to %s at %s", msg.getMessage(), client.getName(), c.getRemoteAddress()));
        } catch (IOException e) {
            Server.logger.severe("Error sending to channel: " + e.getMessage());
        } finally {
            data.rewind();
        }
    }

    /**
     * Sends (up to) @param amount of last logged messages to specified channel.
     *
     * @param c           channel to send to.
     * @param channelName name of the channel.
     * @param amount      of messages to return to the specified channel.
     */
    public void sendChatLog(SocketChannel c, String channelName, int amount) {
        Message[] messageArray = chatLogHandler.getLastMessages(channelName, amount);
        if (messageArray != null) {
            for (Message msg : messageArray) {
                sendMessage(msg, c);
            }
        }
    }

    public void addChannel(String channelName) {
        chatLogHandler.addChannel(channelName);
    }

    /**
     * Kodeerib baitide massiivi websocket standarile sobivaks massiiviks.
     * @param rawData algne byte[] massiiv.
     * @return massiiv, millest saab websocket aru.
     */

    public static byte[] encode(byte[] rawData){  //websocketi byte magic võetud: https://stackoverflow.com/a/53208425
        int frameCount;
        byte[] frame = new byte[10];

        frame[0] = (byte) 129;

        if(rawData.length <= 125){
            frame[1] = (byte) rawData.length;
            frameCount = 2;
        }else if(rawData.length <= 65535){
            frame[1] = (byte) 126;
            int len = rawData.length;
            frame[2] = (byte)((len >> 8 ) & (byte)255);
            frame[3] = (byte)(len & (byte)255);
            frameCount = 4;
        }else{
            frame[1] = (byte) 127;
            int len = rawData.length;
            frame[2] = (byte)((len >> 56 ) & (byte)255);
            frame[3] = (byte)((len >> 48 ) & (byte)255);
            frame[4] = (byte)((len >> 40 ) & (byte)255);
            frame[5] = (byte)((len >> 32 ) & (byte)255);
            frame[6] = (byte)((len >> 24 ) & (byte)255);
            frame[7] = (byte)((len >> 16 ) & (byte)255);
            frame[8] = (byte)((len >> 8 ) & (byte)255);
            frame[9] = (byte)(len & (byte)255);
            frameCount = 10;
        }

        int bLength = frameCount + rawData.length;

        byte[] reply = new byte[bLength];

        int bLim = 0;
        for(int i=0; i<frameCount;i++){
            reply[bLim] = frame[i];
            bLim++;
        }
        for(int i=0; i<rawData.length;i++){
            reply[bLim] = rawData[i];
            bLim++;
        }

        return reply;
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
