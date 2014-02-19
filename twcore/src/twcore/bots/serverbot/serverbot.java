package twcore.bots.serverbot;

import twcore.core.*;
import twcore.core.events.*;
import twcore.core.stats.DBPlayerData;
import twcore.core.util.Tools;
import twcore.core.events.SQLResultEvent;
import twcore.core.game.Player;

import java.net.*;
import java.io.*;
import java.sql.ResultSet;
import java.util.*;

public class serverbot extends SubspaceBot {
    private BotAction BA;
    private BotSettings BS;
    private OperatorList oplist;
    String pname;

    public serverbot(BotAction botAction) {
        super(botAction);

        BA = BotAction.getBotAction();
        BS = BA.getBotSettings();
        EventRequester req = BA.getEventRequester();
        oplist = BA.getOperatorList();

        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.LOGGED_ON);
        pname = null;
    }

    //Standard Message Event for TWCore. Commands and stuff!
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        int msgtype = event.getMessageType();
        String name = event.getMessager();
        if (name == null)
            name = BA.getPlayerName(event.getPlayerID());

        if (pname == null)
            pname = name;

        if ((msgtype == Message.PRIVATE_MESSAGE) || (msgtype == Message.REMOTE_PRIVATE_MESSAGE)) {
        	if(oplist.isSmod(name)){
        		if(msg.equalsIgnoreCase("!die")){
        			ba.die();
        		} else if(msg.startsWith("!go ")){
        			ba.changeArena(msg.substring(4));
        		}
        	}
        }
    }


    public void handleEvent(LoggedOn event) {
        BA.joinArena(BS.getString("arena"));
    }

    public void handleEvent(ArenaJoined event) {
        BA.setReliableKills(1);
    }

    public void handleEvent(SQLResultEvent event) {
    }

    public void handleEvent(InterProcessEvent event) {
      
    }

    public void handleEvent(SocketMessageEvent event){

    	if (event.getRequest().equals("GETPLAYERS")) {
    		ba.sendSmartPrivateMessage("Dezmond", "Connected successfully.");
    		Iterator<Player> it = m_botAction.getPlayerIterator();
    		
    		// Building a JSON-format result
    		StringBuilder builder = new StringBuilder();
    		while(it.hasNext()) {
    			Player p = it.next();
    			builder.append(p.getPlayerName()+":"+p.getXTileLocation()+":"+p.getYTileLocation()+":"+p.getShipType() + "$$:$$");
        		ba.sendSmartPrivateMessage("Dezmond", "Successfully sent to script...");
    		}

    		event.setResponse(builder.toString());
    	}
    }
}