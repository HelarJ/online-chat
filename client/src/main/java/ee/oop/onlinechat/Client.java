package ee.oop.onlinechat;

import com.google.crypto.tink.BinaryKeysetWriter;
import com.google.crypto.tink.CleartextKeysetHandle;
import ee.ut.oop.Crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {
        int aeg = 5000;
        Crypto decrypter = new Crypto();
        Crypto encrypter;
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 1337);
        SocketChannel socketChannel;
        while (true){
            try { //initial trading of public keys.
                socketChannel = SocketChannel.open(hostAddress);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CleartextKeysetHandle.write(decrypter.getPublicKeysetHandle(), BinaryKeysetWriter.withOutputStream(bos));
                socketChannel.write(ByteBuffer.wrap(bos.toByteArray()));

                bos.reset();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int count = socketChannel.read(buffer);
                byte[] data = new byte[count];
                System.arraycopy(buffer.array(), 0, data, 0, count);
                bos.writeBytes(data);
                buffer.clear();
                byte[] decrypted = decrypter.decrypt(bos.toByteArray());
                encrypter = new Crypto(decrypted);
                break;
            } catch (ConnectException e) {
                System.out.println("Error connecting to the server.. Retrying in " + aeg / 1000+"s.");
                Thread.sleep(aeg);
                aeg += 1000;

                if (aeg > 20000) {
                    aeg = 20000;
                }
            }
        }

        System.out.println("Client connected!");

        final Kuulaja kuulaja = new Kuulaja(socketChannel, decrypter);
        Thread.sleep(50);
        final Saatja saatja = new Saatja(socketChannel, encrypter);

        Thread kuulajaThread = new Thread(kuulaja);
        Thread saatjaThread = new Thread(saatja);
        kuulajaThread.start();
        saatjaThread.start();

        kuulajaThread.join(); //need joinid toimuvad alles siis, kui while loopid nendes threadides otsa saavad.
        saatjaThread.join();
        socketChannel.close();

        System.out.println("Client shut down.");
    }
}
