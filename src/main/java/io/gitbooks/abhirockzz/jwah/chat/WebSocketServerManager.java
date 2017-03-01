package io.gitbooks.abhirockzz.jwah.chat;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.glassfish.tyrus.server.Server;


public final class WebSocketServerManager {

    private Server server;

    public void runServer(boolean autoShutDown) throws Exception {
        server = new Server(Optional.ofNullable(System.getenv("HOSTNAME")).orElse("localhost"),
                Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080")),
                "", null, ChatServer.class);

        server.start();

        
        if (autoShutDown) {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.stop();
                    System.out.println("Stopped WebSocket server");

                }
            }));

            System.out.print("Shutdown hook added");

        }

    }

    public void stop() {
        server.stop();
    }

    public static void main(String[] args) throws Exception {

        new WebSocketServerManager().runServer(true);
        new CountDownLatch(1).await();
    }

}
