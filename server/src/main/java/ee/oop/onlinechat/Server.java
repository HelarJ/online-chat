package ee.oop.onlinechat;

import java.io.IOException;

public class Server
{
    public static void main(String[] args) {
        try {
            new Ühendus("localhost", 1337).ühenda();
        } catch (IOException e) {
            System.out.println("ERROR");
            System.out.println(e.getMessage());
        }
    }
}
