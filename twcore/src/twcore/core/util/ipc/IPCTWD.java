package twcore.core.util.ipc;

/**
 *
 * @author WingZero
 */
public class IPCTWD {

    EventType type;
    String arena;
    String bot;
    String squad1;
    String squad2;
    int score1;
    int score2;
    int id;
    
    public IPCTWD(EventType type, String arena, String bot, String squad1, String squad2, int id) {
        this.type = type;
        this.arena = arena;
        this.bot = bot;
        this.squad1 = squad1;
        this.squad2 = squad2;
        this.id = id;
        score1 = 0;
        score2 = 0;
    }
    
    public IPCTWD(EventType type, String arena, String bot, String squad1, String squad2, int id, int score1, int score2) {
        this.type = type;
        this.arena = arena;
        this.bot = bot;
        this.squad1 = squad1;
        this.squad2 = squad2;
        this.id = id;
        this.score1 = score1;
        this.score2 = score2;
    }
    
    public IPCTWD(EventType type, String arena, String bot, int id) {
        this.type = type;
        this.arena = arena;
        this.bot = bot;
        this.squad1 = null;
        this.squad2 = null;
        this.id = id;
        score1 = 0;
        score2 = 0;
    }
    
    public EventType getType() {
        return type;
    }
    
    public String getArena() {
        return arena;
    }
    
    public String getBot() {
        return bot;
    }
    
    public int getID() {
        return id;
    }
    
    public String getSquad1() {
        return squad1;
    }
    
    public String getSquad2() {
        return squad2;
    }
    
    public int getScore1() {
        return score1;
    }
    
    public int getScore2() {
        return score2;
    }
}
