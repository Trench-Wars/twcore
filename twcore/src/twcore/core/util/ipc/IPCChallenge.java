package twcore.core.util.ipc;

/**
 *
 * @author WingZero
 */
public class IPCChallenge {

    EventType type;
    String bot;
    String arena;
    String name;
    String squad1;
    String squad2;
    int players;
    
    public IPCChallenge(String toBot, EventType type, String arena, String name, String squad1, String squad2, int players) {
        this.bot = toBot;
        this.type = type;
        this.arena = arena;
        this.name = name;
        this.squad1 = squad1;
        this.squad2 = squad2;
        this.players = players;
    }
    
    public String getRecipient() {
        return bot;
    }
    
    public void setBot(String name) {
        bot = name;
    }
    
    public EventType getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public String getArena() {
        return arena;
    }
    
    public String getSquad1() {
        return squad1;
    }
    
    public String getSquad2() {
        return squad2;
    }
    
    public int getPlayers() {
        return players;
    }
    
}
