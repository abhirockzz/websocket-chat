package io.gitbooks.abhirockzz.jwah.chat;

import io.gitbooks.abhirockzz.jwah.chat.WebSocketServerManager;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class ChatServerTest {

    public ChatServerTest() {
    }

    private WebSocketServerManager instance;
    private CountDownLatch controlLatch;
    private static final String BASE_SERVER_URL = "ws://localhost:8080/chat/";

    @Before
    public void setUp() {
        try {
            instance = new WebSocketServerManager();
            instance.runServer(false);
        } catch (Exception ex) {
            Logger.getLogger(ChatServerTest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @After
    public void tearDown() {
        instance.stop();
        controlLatch = null;
    }

    class ChatClient extends Endpoint {

        private String response;
        private Session sn;

        @Override
        public void onOpen(Session sn, EndpointConfig ec) {
            this.sn = sn;
            controlLatch.countDown();
            sn.addMessageHandler(String.class, s -> {
                response = s;
                controlLatch.countDown();
            });
        }

        public String getResponse() {
            return response;
        }

        public void sendPublicChat(String msg) {
            try {
                sn.getBasicRemote().sendText(msg);
            } catch (IOException ex) {
                Logger.getLogger(ChatServerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void sendPrivateChat(String msg, String recepient) {
            try {
                sn.getBasicRemote().sendText("[" + recepient + "]" + msg);
            } catch (IOException ex) {
                Logger.getLogger(ChatServerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void sendLogout() {
            try {
                sn.getBasicRemote().sendText("[logout]");
            } catch (IOException ex) {
                Logger.getLogger(ChatServerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public void closeConnection(){
            try {
                sn.close();
            } catch (IOException ex) {
                Logger.getLogger(ChatServerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private CloseReason closeReason;
        
        @Override
        public void onClose(Session session, CloseReason closeReason) {
            //System.out.println("close reason "+ closeReason.getReasonPhrase() + "\n"+ closeReason.getCloseCode());
            this.closeReason = closeReason;
            if(connClosedLatch!=null){
                connClosedLatch.countDown();
            }
            
        }
        
        public CloseReason getCloseReason(){
            return this.closeReason;
        }
        
    }
    
    private CountDownLatch connClosedLatch = null;

    @Test
    public void newJoineeGetsWelcomeMsg() throws DeploymentException, IOException, InterruptedException {
        controlLatch = new CountDownLatch(2);

        ChatClient newJoinee = new ChatClient();
        String newJoineeName = "abhishek";
        String endpointURL = BASE_SERVER_URL + newJoineeName + "/";
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(newJoinee,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(endpointURL));

        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        String expectedWelcomeMessage = "Welcome " + newJoineeName;
        assertTrue(newJoinee.getResponse().contains(expectedWelcomeMessage));
        newJoinee.closeConnection();
    }

    @Test
    public void existingChattersGetNewJoineeNotification() throws Exception {
        controlLatch = new CountDownLatch(2);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        ChatClient abhi = new ChatClient();
        String chatter1 = "abhishek";
        String endpointURL = BASE_SERVER_URL + chatter1 + "/";
        container.connectToServer(abhi,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(endpointURL));

        String expectedWelcomeMessageForChatter1 = "Welcome " + chatter1;
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(abhi.getResponse().contains(expectedWelcomeMessageForChatter1));

        controlLatch = new CountDownLatch(3);

        ChatClient gitu = new ChatClient();
        String chatter2 = "gitu";
        endpointURL = BASE_SERVER_URL + chatter2 + "/";
        container.connectToServer(gitu,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(endpointURL));

        String expectedWelcomeMessageForChatter2 = "Welcome " + chatter2;
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(gitu.getResponse().contains(expectedWelcomeMessageForChatter2));
        String expectedNewJoineeNotificationMsgToChatter1 = "New user joined --- " + chatter2;
        assertTrue(abhi.getResponse().contains(expectedNewJoineeNotificationMsgToChatter1));

        String expectedConnectedUsersNotificationMsgToChatter1 = "Currently connected users - [" + chatter1 + ", " + chatter2 + "]";
        assertTrue(abhi.getResponse().contains(expectedConnectedUsersNotificationMsgToChatter1));
        
        abhi.closeConnection();
        gitu.closeConnection();

    }

    @Test
    public void sendPublicMsgToAll() throws Exception {
        controlLatch = new CountDownLatch(2);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        ChatClient abhi = new ChatClient();
        container.connectToServer(abhi,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(BASE_SERVER_URL + "abhi/"));

        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(abhi.getResponse().contains("Welcome abhi"));

        controlLatch = new CountDownLatch(3);

        ChatClient gitu = new ChatClient();
        container.connectToServer(gitu,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(BASE_SERVER_URL + "gitu/"));

        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(gitu.getResponse().contains("Welcome gitu"));
        assertTrue(abhi.getResponse().contains("New user joined --- gitu"));
        assertTrue(abhi.getResponse().contains("Currently connected users - [abhi, gitu]"));

        controlLatch = new CountDownLatch(1);

        String publicMsg = "hey everyone!!";
        gitu.sendPublicChat(publicMsg); //send public message to ALL
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS)); // wait for message to be received
        assertTrue(abhi.getResponse().equals("[From: gitu] " + publicMsg));
        
        abhi.closeConnection();
        gitu.closeConnection();
    }

    @Test
    public void sendPrivateMsg() throws Exception {
        controlLatch = new CountDownLatch(2);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        ChatClient abhi = new ChatClient();
        container.connectToServer(abhi,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(BASE_SERVER_URL + "abhi/"));

        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(abhi.getResponse().contains("Welcome abhi"));

        controlLatch = new CountDownLatch(3);

        ChatClient gitu = new ChatClient();
        container.connectToServer(gitu,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(BASE_SERVER_URL + "gitu/"));

        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(gitu.getResponse().contains("Welcome gitu"));
        assertTrue(abhi.getResponse().contains("New user joined --- gitu"));
        assertTrue(abhi.getResponse().contains("Currently connected users - [abhi, gitu]"));

        controlLatch = new CountDownLatch(1);

        String privateMsg = "hey abhishek";
        gitu.sendPrivateChat(privateMsg, "abhi"); //send private message to abhi
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS)); // wait for message to be received
        assertTrue(abhi.getResponse().equals("(privately) [From: gitu] " + privateMsg));
        
        abhi.closeConnection();
        gitu.closeConnection();
    }

    @Test
    public void usersReceiveLogoutNotification() throws Exception {
        controlLatch = new CountDownLatch(2);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        ChatClient abhi = new ChatClient();
        container.connectToServer(abhi,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(BASE_SERVER_URL + "abhi/"));

        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(abhi.getResponse().contains("Welcome abhi"));

        controlLatch = new CountDownLatch(3);

        ChatClient gitu = new ChatClient();
        container.connectToServer(gitu,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(BASE_SERVER_URL + "gitu/"));

        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        //assertTrue(gitu.getResponse().contains("Welcome gitu"));
        assertEquals(gitu.getResponse(), "Welcome gitu");
        assertTrue(abhi.getResponse().contains("New user joined --- gitu"));
        assertTrue(abhi.getResponse().contains("Currently connected users - [abhi, gitu]"));

        controlLatch = new CountDownLatch(1);

        gitu.sendLogout();
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS)); // wait for message to be received
        assertTrue(abhi.getResponse().equals("User gitu has logged out"));
        
       abhi.closeConnection(); //no need to close 'gitu' connection since it will be closed by server endpoint due to 'logout'
       // gitu.closeConnection();
    }

    @Test
    public void userNameMustBeUnique() throws Exception {
        controlLatch = new CountDownLatch(2);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        ChatClient abhi = new ChatClient();
        String chatter = "abhishek";

        String endpointURL = BASE_SERVER_URL + chatter + "/";
        container.connectToServer(abhi,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(endpointURL));

        String expectedWelcomeMessageForChatter1 = "Welcome " + chatter;
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(abhi.getResponse().contains(expectedWelcomeMessageForChatter1));

        controlLatch = new CountDownLatch(2);

        ChatClient otherAbhi = new ChatClient();

        endpointURL = BASE_SERVER_URL + chatter + "/";
        container.connectToServer(otherAbhi,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(endpointURL));

        String expectedMessageForChatter2 = "Username " + chatter + " has been taken. Retry with a different name";
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertEquals(expectedMessageForChatter2, otherAbhi.getResponse());
        
        abhi.closeConnection(); //no need to close 'otherAbhi' connection since it will be closed by server endpoint due to 'duplicate user'

    }
    public static long maxIdleTime = 2000; 
    
    @Test
    public void sessionAutoClosedAfterMaxIdleTimeoutBreach() throws Exception {
        controlLatch = new CountDownLatch(2);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        ChatClient abhi = new ChatClient();
        String chatter = "abhishek";

        String endpointURL = BASE_SERVER_URL + chatter + "/";
        Session session = container.connectToServer(abhi,
                ClientEndpointConfig.Builder.create().build(),
                URI.create(endpointURL));
        
        session.setMaxIdleTimeout(maxIdleTime); //set the timeout
        
        String expectedWelcomeMessageForChatter1 = "Welcome " + chatter;
        assertTrue(controlLatch.await(5, TimeUnit.SECONDS));
        assertTrue(abhi.getResponse().contains(expectedWelcomeMessageForChatter1));
        
        
        connClosedLatch = new CountDownLatch(1);
        assertTrue(connClosedLatch.await(maxIdleTime + 5000, TimeUnit.MILLISECONDS)); // wait 5 seconds more than the timeout
        
        String expectedSessionTimeoutCloseReasonPhrase = "\"Session closed by the container because of the idle timeout.\"";
        assertEquals(expectedSessionTimeoutCloseReasonPhrase, abhi.getCloseReason().getReasonPhrase()); //check the exact phrase
        
        //String expectedSessionTimeoutCloseReasonCode = "CLOSED_ABNORMALLY";
        //assertEquals(expectedSessionTimeoutCloseReasonCode, abhi.getCloseReason().getCloseCode().toString()); //check the exact code
    }

}
