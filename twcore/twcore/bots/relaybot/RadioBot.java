package twcore.bots.relaybot;

public class RadioBot {
    public void sendArenaMessage( String msg, int soundid ){

        System.out.println( "Send arena message: " + msg + " -DoCk> %" + soundid );        
    }
    
    public void sendSmartPrivateMessage( String name, String msg ){
        
        System.out.println( "Send private message: " + name + "> "
        + msg + " -DoCk>" );
    }
}
