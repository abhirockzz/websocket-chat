package io.gitbooks.abhirockzz.jwah.chat.model;

import java.util.Set;


public class NewJoineeNotification {
    private String newJoinee;
    private Set<String> existingSetOfUsers;

    public NewJoineeNotification(String newJoinee, Set<String> existingSetOfUsers) {
        this.newJoinee = newJoinee;
        this.existingSetOfUsers = existingSetOfUsers;
    }
    
    public String getNewJoineeMessage(){
        return "New user joined --- " + this.newJoinee + "\nCurrently connected users - " + existingSetOfUsers;
    }
}
