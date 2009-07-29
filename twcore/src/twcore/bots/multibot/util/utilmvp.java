package twcore.bots.multibot.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ArrayList;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.util.ModuleEventRequester;

/**
 * Tracks player kills and deaths when activated and supplies the best K/D ratio.
 * @author milosh
 */
public class utilmvp extends MultiUtil {
	
	public TreeMap<String, MVPPlayer> mvpPlayers = new TreeMap<String, MVPPlayer>();
	public boolean isRecording = false;
	
	@Override
	public String[] getHelpMessages() {
		String[] message =
	    {
	      "+=============== MVP ===============+",
	      "| !startmvp - Starts MVP tracking   |",
	      "| !stopmvp  - Stops MVP tracking    |",
	      "| !arenamvp - Displays MVP to arena |",
	      "| !mvp      - Displays MVP in PM    |",
	      "+===================================+"
	    };

	    return message;
	}
	
	@Override
	public void init() {}

	@Override
	public void requestEvents(ModuleEventRequester modEventReq) {
		// required :o
	}
	
	public void handleEvent(Message event)
	{
		String message = event.getMessage();
        String name = event.getMessager() == null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(name))
			handleStaffCommands(name, message);
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			handlePlayerCommands(name, message);
	}
	
	public void handleStaffCommands(String name, String cmd){
		if(cmd.equalsIgnoreCase("!startmvp"))
			doStartMvp(name);
		else if(cmd.equalsIgnoreCase("!stopmvp"))
			doStopMvp(name);
		else if(cmd.equalsIgnoreCase("!arenamvp"))
			doArenaMvp(name);
	}
	
	public void handlePlayerCommands(String name, String cmd){
		if(cmd.equalsIgnoreCase("!mvp"))
			doMvp(name);
	}
	
	public void doStartMvp(String name){
		if(!isRecording){
			mvpPlayers.clear();
			isRecording = true;
			m_botAction.sendSmartPrivateMessage(name, "MVP Recording has been enabled.");
		}else m_botAction.sendSmartPrivateMessage(name, "MVP Recording is already running. Use !stopmvp to stop.");
		
	}
	
	public void doStopMvp(String name){
		if(isRecording){
			isRecording = false;
			m_botAction.sendSmartPrivateMessage(name, "MVP Recording has been disabled. Use !arenamvp to display the MVP.");
		}else m_botAction.sendSmartPrivateMessage(name, "MVP Recording is not running. Use !startmvp to start.");
	}
	
	public void doArenaMvp(String name){
		if(mvpPlayers.isEmpty() && !isRecording)
			m_botAction.sendSmartPrivateMessage(name, "There is currently no MVP.");
		else {
			int z = 0;
			Iterator<String> i = getMvpString().iterator();
			while(z < 6 && i.hasNext())
				m_botAction.sendArenaMessage(i.next());
		}
	}
	
	public void doMvp(String name){
		if(mvpPlayers.isEmpty() && !isRecording)
			m_botAction.sendSmartPrivateMessage(name, "There is currently no MVP.");
		else {
			int z = 0;
			Iterator<String> i = getMvpString().iterator();
			while(z < 6 && i.hasNext())
				m_botAction.sendSmartPrivateMessage(name, i.next());
		}
	}
	
	public ArrayList<String> getMvpString(){
		ArrayList<String> mvpArray = new ArrayList<String>();
		mvpArray.add("There is currently no MVP.");
		if(mvpPlayers.isEmpty()) return mvpArray;
		Iterator<MVPPlayer> i = mvpPlayers.values().iterator();
		while(i.hasNext()){
			MVPPlayer p = i.next();
			p.ratio = p.kills / p.deaths;
		}
		CompareByRatio byRatio = new CompareByRatio();
    	List<MVPPlayer> l = Arrays.asList(mvpPlayers.values().toArray(new MVPPlayer[mvpPlayers.values().size()]));
    	Collections.sort(l, Collections.reverseOrder(byRatio));
    	if(isRecording){
    		mvpArray.clear();
    		mvpArray.add("+===================== MVPs =====================+");
    		int z = 0;
    		while(z < 5 && z != l.size()){
    			mvpArray.add(" " + (z+1) + ") " + l.get(z).name + " (" + l.get(z).kills + "-" + l.get(z).deaths + ")");
    			z++;
    		}
    		return mvpArray;
    	}
    	else {
    		mvpArray.clear();
    		mvpArray.add("MVP: " + l.get(0).name + " (" + l.get(0).kills + "-" + l.get(0).deaths + ")");
    		return mvpArray;
    	}
	}
	
	public void handleEvent(PlayerDeath event){
		if(!isRecording)return;
		String killer = m_botAction.getPlayerName(event.getKillerID());
        String killed = m_botAction.getPlayerName(event.getKilleeID());
        if(!mvpPlayers.containsKey(killer))
        	mvpPlayers.put(killer, new MVPPlayer(killer));
        if(!mvpPlayers.containsKey(killed))
        	mvpPlayers.put(killed, new MVPPlayer(killed));
        mvpPlayers.get(killer).kills++;
        mvpPlayers.get(killed).deaths++;
	}
	
	public void cancel() {
		mvpPlayers.clear();
	}
	
	private class MVPPlayer {
		private String name;
		private int kills = 0,deaths = 0;
		private double ratio = 0;
		
		private MVPPlayer(String name){
			this.name = name;
		}
	}
	
	private class CompareByRatio implements Comparator<MVPPlayer> {
		public int compare(MVPPlayer a, MVPPlayer b){
			if(a.ratio > b.ratio)return 1;
			else if(a.ratio == b.ratio)return 0;
			else return -1;
		}
	}
}