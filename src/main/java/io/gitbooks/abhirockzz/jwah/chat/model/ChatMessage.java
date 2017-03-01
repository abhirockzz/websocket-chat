package io.gitbooks.abhirockzz.jwah.chat.model;


public class ChatMessage {
    private String msg;
    private String recepient;
    private boolean isPrivate;
    
    public ChatMessage(String msg) {
        this.msg = msg;
        this.isPrivate = msg.equals("[logout]") ? false : msg.startsWith("[") ; //e.g. [abhishek] hey abhi!
        this.recepient = this.isPrivate ? msg.substring(msg.indexOf("[") + 1, msg.lastIndexOf("]")) : "ALL";
        
        //System.out.println(toString());
    }
    
    public String getMsg(){
        return this.msg;
    }
    
    public String getRecepient(){
        return this.recepient;
    }
    
    public boolean isPrivate(){
        return this.isPrivate;
    }

    @Override
    public String toString() {
        return "ChatMessage{" + "msg=" + msg + ", recepient=" + recepient + ", isPrivate=" + isPrivate + '}';
    }
    
    
  
}
