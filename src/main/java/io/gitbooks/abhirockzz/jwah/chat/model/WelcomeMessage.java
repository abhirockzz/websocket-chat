package io.gitbooks.abhirockzz.jwah.chat.model;


public class WelcomeMessage {

    private final String forUser;
    
    public WelcomeMessage(String forUser) {
        this.forUser = forUser;
    }
    
    public String getWelcomeMessage(){
        return "Welcome " + forUser;
    }
    
}
