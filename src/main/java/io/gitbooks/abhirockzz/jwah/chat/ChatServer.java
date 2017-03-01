package io.gitbooks.abhirockzz.jwah.chat;

import io.gitbooks.abhirockzz.jwah.chat.internal.NewJoineeMessageEncoder;
import io.gitbooks.abhirockzz.jwah.chat.internal.LogOutMessageEncoder;
import io.gitbooks.abhirockzz.jwah.chat.internal.ReplyEncoder;
import io.gitbooks.abhirockzz.jwah.chat.internal.ChatMessageDecoder;
import io.gitbooks.abhirockzz.jwah.chat.internal.DuplicateUserMessageEncoder;
import io.gitbooks.abhirockzz.jwah.chat.internal.WelcomeMessageEncoder;
import io.gitbooks.abhirockzz.jwah.chat.model.WelcomeMessage;
import io.gitbooks.abhirockzz.jwah.chat.model.ChatMessage;
import io.gitbooks.abhirockzz.jwah.chat.model.LogOutNotification;
import io.gitbooks.abhirockzz.jwah.chat.model.Reply;
import io.gitbooks.abhirockzz.jwah.chat.model.NewJoineeNotification;
import java.io.IOException;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint(
        value = "/chat/{user}/",
        encoders = {ReplyEncoder.class, 
                    WelcomeMessageEncoder.class, 
                    NewJoineeMessageEncoder.class, 
                    LogOutMessageEncoder.class,
                    DuplicateUserMessageEncoder.class},
        decoders = {ChatMessageDecoder.class}
)

public class ChatServer {

    private static final Set<String> USERS = new ConcurrentSkipListSet<>();
    private String user;
    private Session s;
    private boolean dupUserDetected;

    @OnOpen
    public void userConnectedCallback(@PathParam("user") String user, Session s) {
        if (USERS.contains(user)) {
            try {
                dupUserDetected = true;
                s.getBasicRemote().sendText("Username " + user + " has been taken. Retry with a different name");
                s.close();
                return;
            } catch (IOException ex) {
                Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        this.s = s;
        s.getUserProperties().put("user", user);
        this.user = user;
        USERS.add(user);

        welcomeNewJoinee();
        announceNewJoinee();
    }

    private void welcomeNewJoinee() {
        try {
            s.getBasicRemote().sendObject(new WelcomeMessage(this.user));
        } catch (Exception ex) {
            Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void announceNewJoinee() {
        s.getOpenSessions().stream()
                .filter((sn) -> !sn.getUserProperties().get("user").equals(this.user))
                //.filter((s) -> s.isOpen())
                .forEach((sn) -> sn.getAsyncRemote().sendObject(new NewJoineeNotification(user, USERS)));
    }

    public static final String LOGOUT_MSG = "[logout]";

    @OnMessage
    public void msgReceived(ChatMessage msg, Session s) {
        if (msg.getMsg().equals(LOGOUT_MSG)) {
            try {
                s.close();
                return;
            } catch (IOException ex) {
                Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Predicate<Session> filterCriteria = null;
        if (!msg.isPrivate()) {
            //for ALL (except self)
            filterCriteria = (session) -> !session.getUserProperties().get("user").equals(user);
        } else {
            String privateRecepient = msg.getRecepient();
            //private IM
            filterCriteria = (session) -> privateRecepient.equals(session.getUserProperties().get("user"));
        }

        s.getOpenSessions().stream()
                .filter(filterCriteria)
                //.forEach((session) -> session.getAsyncRemote().sendText(msgContent));
                .forEach((session) -> session.getAsyncRemote().sendObject(new Reply(msg.getMsg(), user, msg.isPrivate())));

    }

    @OnClose
    public void onCloseCallback() {
        if(!dupUserDetected){
            processLogout();
        }
        
    }

    private void processLogout() {
        try {
            USERS.remove(this.user);
            s.getOpenSessions().stream()
                    .filter((sn) -> sn.isOpen())
                    .forEach((session) -> session.getAsyncRemote().sendObject(new LogOutNotification(user)));

        } catch (Exception ex) {
            Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
