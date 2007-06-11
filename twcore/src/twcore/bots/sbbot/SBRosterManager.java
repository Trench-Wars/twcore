package twcore.bots.sbbot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.game.Player;

public class SBRosterManager {
    public static MessageType
	ALLREADY = new MessageType(),
	STARTPICK = new MessageType();
    private final BotAction m_botAction;
    private SBMatchOperator matchOp;
    private PickHandler picker;
    private int MAXPLAYERS = 7;
    private int MINPLAYERS = 2;
    HashMap<String,SBPlayer> players;
    Vector<SBTeam> teams; // I use a vector to force clean implementation
    Vector<SBPlayer> playerQueue;
    HashSet<String> notPlayers;

    public SBRosterManager(SBMatchOperator op, BotAction botAction) {
	m_botAction = botAction;
	matchOp = op;
	notPlayers = new HashSet<String>();
    }

    public void initialize(int numTeams, String[] teamNames) {
	players = new HashMap<String,SBPlayer>();
	teams = new Vector<SBTeam>();
	playerQueue = new Vector<SBPlayer>();

	for(int i = 0; i < numTeams; i++) {
	    teams.add(new SBTeam(teamNames[i], i));
	}
    }

    public void startPick(Player p) {
	for(SBTeam t : teams) {
	    if(t.getCaptain() == null) {
		pm(p.getPlayerName(), t.getName() + " still needs a captain.");
		return;
	    }
	}
	matchOp.notifyEvent(STARTPICK, new Message("Ready to start picking!"));
	picker = new PickHandler();
    }

    public void showCaps(Player p) {
	for(SBTeam t : teams) {
	    if(t.getCaptain() != null)
		pm(p.getPlayerName(), t.getName() + ": " + t.getCaptain());
	}
    }

    public void listPlayers(Player p) {
	String pName = p.getPlayerName();
	if(!havePlayerRec(pName) ||
	   getPlayerRec(pName).getStatus() != SBPlayer.ACTIVE) {
	    for(SBTeam t : teams) {
		pm(pName, t.getName());
		for(SBPlayer curSBP : players.values()) {
		    if(curSBP.getStatus() != SBPlayer.ACTIVE ||
		       curSBP.getTeam() != t) continue;
		    Player curP;
		    if((curP = m_botAction.getPlayer(curSBP.getName())) == null) {
			pm(pName, "     " + curSBP.getName() + " (Lagged out/left)");
			continue;
		    }
		    if(curP.getShipType() == 0) {
			pm(pName, "     " + curSBP.getName() + " (Lagged out/specced)");
			continue;
		    }
		    pm(pName, "     " + curSBP.getName());
		}
	    }
	} else {
	    SBPlayer pRec = getPlayerRec(pName);
	    for(SBPlayer curSBP : players.values()) {
		if(curSBP.getTeam() != pRec.getTeam()) continue;
		Player curP;
		if((curP = m_botAction.getPlayer(curSBP.getName())) == null) {
		    pm(pName, "     " + curSBP.getName() + " (Lagged out/left)");
		    continue;
		}
		if(curP.getShipType() == 0) {
			pm(pName, "     " + curSBP.getName() + " (Lagged out/specced)");
			continue;
		}
		pm(pName, "     " + curSBP.getName());
	    }
	}
    }

    public void setCap(Player host, int teamNum, String arg) {
	if(teamNum >= teams.size() || teamNum < 0) return; //should never happen
	Player p = m_botAction.getFuzzyPlayer(arg);
	if(p == null) {
	    pm(host.getPlayerName(), arg + " isn't here.");
	    return;
	}
	String capName = p.getPlayerName();
	if(isCaptain(capName)) {
	    getCapTeam(capName).setCaptain((String)null);
	}
	teams.get(teamNum).setCaptain(capName);
	arena(capName + " assigned as captain of " + teams.get(teamNum).getName() + ".");
    }

    public void sub(Player cap, String currentName, String subName) {
	String capName = cap.getPlayerName();
	if(!capCheck(capName)) return;
	SBPlayer currentRec = null;
	if(currentName == null) {
	    for(SBPlayer p : players.values()) {
		if(p.getTeam() != getCapTeam(capName)) continue;
		Player curP;
		if((curP = m_botAction.getPlayer(p.getName())) == null ||
		   curP.getShipType() == 0) {
		    currentRec = getFuzzyPlayerRec(p.getName());
		    currentName = currentRec.getName();
		    break;
		}
	    }
	} else
	    currentRec = getFuzzyPlayerRec(currentName);
	if(currentRec == null || currentRec.getTeam() != getCapTeam(capName)) {
	    pm(capName, "You can't sub out a player who isn't on your team.");
	    return;
	}
	String pName = m_botAction.getFuzzyPlayer(subName).getPlayerName();
	if(isCaptain(pName) && !capName.equalsIgnoreCase(pName)) {
	    pm(capName, "You can't sub in the other captain!");
	    return;
	}
	if(pName == null) {
	    pm(capName, subName + " isn't here to be subbed in.");
	    return;
	}
	if(m_botAction.getBotName().equalsIgnoreCase(pName)) {
	    pm(capName, "Sorry, but I'm too busy running the game to play :(.");
	}
	SBPlayer pRec;
	// note: I decided to let captains snape players off other teams' queues.
	if(!notPlayingCheck(capName, pName)) return;
	if(havePlayerRec(pName)) {
	    pRec = getPlayerRec(pName);
	    if(pRec.getStatus() == SBPlayer.ACTIVE) {
		pm(capName, pName + " is already in the game.");
		return;
	    } else if(pRec.getStatus() == SBPlayer.PENDING) {
		if(playerQueue.contains(pRec))
		    playerQueue.remove(pRec);
	    }
	} else {
	    createPlayerRec(pName);
	    pRec = getPlayerRec(pName);
	}
	currentRec.setStatus(SBPlayer.INACTIVE);
	if(m_botAction.getPlayer(currentRec.getName()) != null ) {
	    m_botAction.spec(currentRec.getName());
	}
	pRec.setStatus(SBPlayer.ACTIVE);
	pRec.setTeam(getCapTeam(capName));
	shipify(pName, pRec.getTeam().getFreq());
	pm(pName,"You have been subbed into the game.");
	arena(currentRec.getName() + " has been substituted by " + pName);
    }

    public void remove(Player cap, String args) {
	String capName = cap.getPlayerName();
	SBPlayer pRec = getFuzzyPlayerRec(args);
	if(!capCheck(capName)) return;
	if(pRec == null ||
	   pRec.getTeam() != getCapTeam(capName) ||
	   pRec.getStatus() == SBPlayer.INACTIVE) {
	    pm(capName, args + " isn't on your team.");
	    return;
	}
	String pName = pRec.getName();
	if(m_botAction.getPlayer(pName) != null &&
	   m_botAction.getPlayer(pName).getShipType() != 0) {
	    m_botAction.spec(pName);
	}
	pRec.setStatus(SBPlayer.INACTIVE);
	pm(pName, "You have been removed from the game.");
	pm(capName, pName + " has been removed.");
	SBTeam team = getCapTeam(capName);
	if(team.isReady()) {
	    arena(team.getName() + " is NOT ready to begin.");
	    team.setReady(false);
	}
    }

    public void ready(Player p) {
	String capName = p.getPlayerName();
	SBTeam team = getCapTeam(capName);
	int highestCount = 0;

	if(!capCheck(capName)) return;
	if(team.isReady()) {
	    team.setReady(false);
	    arena(getCapTeam(capName).getName() + " is NOT ready to begin.");
	    return;
	}
	for(SBTeam t : teams) {
	    int count = activePlayerCount(t);
	    if(count > highestCount) highestCount = count;
	}
	if(activePlayerCount(team) < highestCount) {
	    pm(capName, "You must have at least as much players as the other team(s) before you can !ready.");
	    return;
	}
	arena(getCapTeam(capName).getName() + " is ready to begin.");
	getCapTeam(capName).setReady(true);
	for(SBTeam t : teams) {
	    if(!t.isReady()) return;
	}
	matchOp.notifyEvent(ALLREADY, new Message("Good to go baby!"));
    }

    public void notPlaying(Player p) {
	String pName = p.getPlayerName();
	if(notPlayers.contains(pName.toLowerCase())) {
	    notPlayers.remove(pName.toLowerCase());
	    pm(pName, "Captains may now add you to the game.");
	} else {
	    if(isActive(pName)) {
		String capName = getPlayerRec(pName).getTeam().getCaptain();
		pm(capName, pName + " has decided to !notplay.", 13);
	    }
	    if(p.getShipType() != 0) {
		m_botAction.spec(pName);
	    }
	    if(havePlayerRec(pName)) getPlayerRec(pName).setStatus(SBPlayer.INACTIVE);
	    notPlayers.add(pName.toLowerCase());
	}
    }

    public void lagout(Player p) {
	String pName = p.getPlayerName();

	if(p.getShipType() != 0) {
	    pm(pName, "You're already in the game!");
	} else if(!havePlayerRec(pName)) {
	    pm(pName, "You aren't in the current game.");
	} else if(playerQueue.contains(getPlayerRec(pName))) {
	    pm(pName, "You are queued to go in as soon as possible.");
	} else if(!isActive(pName)) {
	    pm(pName, "You are no longer in the game.  You have either been subbed out or missed your window for re-entry.");
	    pm(pName, "A captain must either re-add you, or you need to get back in the entry queue by pm'ing me with !enter.");
	} else {
	    shipify(pName,getPlayerRec(pName).getTeam().getFreq());
	}
    }

    public void pick(Player cap, String arg) {
	String capName = cap.getPlayerName();
	if(!capCheck(capName)) return;
	debug("Entering maxplayercheck");
	if(!maxPlayerCheck(getCapTeam(capName))) return;
	debug("entering picker.");
	picker.tryPick(cap, arg);
    }

    public void enqueue(Player cap, String arg) {
	String capName = cap.getPlayerName();
	if(!capCheck(capName)) return;
	SBTeam team = getCapTeam(capName);
	if(!maxPlayerCheck(team)) return;
	if(!eligiblePlayerCheck(capName, arg)) return;
	String pName = m_botAction.getFuzzyPlayerName(arg);

	if(!notPlayingCheck(capName, pName)) return;
	if(!havePlayerRec(pName))
	   createPlayerRec(pName);
	playerQueue.add(getPlayerRec(pName));
	getPlayerRec(pName).setTeam(team);
	getPlayerRec(pName).setStatus(SBPlayer.PENDING);
	Vector<SBPlayer> pendingPlayers = new Vector<SBPlayer>();
	// If the the requesting cap's team is down players, let the player in immediately.
	if(activePlayerCount(team) < highestActivePlayerCount()) {
	    pendingPlayers.add(getPlayerRec(pName));
	} else {
	    // What I'm doing here is making sure that there's at least one person queued to go in
	    // for each team.  If there isn't, add the newest addition to the queue.  If there are,
	    // go ahead and put them all in.

	    one:
	    for(SBTeam t : teams) {
		boolean foundPlayer = false;
		for(SBPlayer p: playerQueue) {
		    if(p.getTeam() == t && (m_botAction.getPlayer(p.getName()) != null)) {
			pendingPlayers.add(p);
			foundPlayer = true;
			break;
		    }
		}
		if(foundPlayer == false) {
		    pm(capName, pName + " has been queued to enter.");
		    pm(pName, "You have been queued to enter for " + team.getName() + ".");
		    pm(pName, "You will enter the game when someone is queued for the other team(s).");
		    arena(pName + " is queued to enter the game for " + team.getName() + ".");
		    return;
		}
	    }
	}
	for(SBPlayer p : pendingPlayers) {
	    pm(p.getName(), "Enough players are available for you to enter the game.  Entering in 3 seconds!");
	}
	final Vector<SBPlayer> finalQueue = pendingPlayers;
	TimerTask t = new TimerTask() {
		public void run() {
		    String names = "";
		    for(int i = 0; i < finalQueue.size(); i++) {
			SBPlayer p = finalQueue.get(i);
			shipify(p.getName(), p.getTeam().getFreq());
			p.setStatus(SBPlayer.ACTIVE);
			if(i == finalQueue.size() - 1) names += " and " + p.getName();
			else if(i != 0) names += ", " + p.getName();
			else names = p.getName();
			playerQueue.remove(p);
		    }
		    arena(names + " have entered the game!", 19);
		}
	    };
	m_botAction.scheduleTask(t, 3000);

    }

    public void makeTeamsEven() {
	HashMap<SBTeam,Integer> rosterSizes = new HashMap<SBTeam,Integer>();
	int highestCount = 0;
	int lowestCount = 0;
	for(SBTeam t : teams) {
	    int count;
	    rosterSizes.put(t,count = activePlayerCount(t));
	    if(count < lowestCount) lowestCount = count;
	    if(count > highestCount) highestCount = count;
	}
	if(highestCount == lowestCount) return;
	for(SBTeam t : teams) {
	    int delta = rosterSizes.get(t) - lowestCount;
	    Iterator<SBPlayer> iter = players.values().iterator();
	    while(delta > 0) {
		SBPlayer unluckySOB = iter.next();
		if(unluckySOB.getTeam() == t) {
		    delta--;
		    m_botAction.spec(unluckySOB.getName());
		    unluckySOB.setStatus(SBPlayer.INACTIVE);
		    // least I can do is put the poor bastard at the front of the queue
		    playerQueue.add(0, unluckySOB);
		    pm(unluckySOB.getName(), "Congratulations!  You're the lucky winner of a spot on the bench :(.");
		    pm(unluckySOB.getName(), "When there are enough players to allow teams to remain even, you will re-enter the game.");
		}
		if(!iter.hasNext()) {
		    //wtfz?
		    break;
		}
	    }
	}
    }

    //I use a class since the picking process must maintain state, and I don't want to clutter
    // the main class.
    private class PickHandler {
	SBTeam currentTeam = null;

	public PickHandler() {
	    setNextTeam();
	}

	public void tryPick(Player cap, String args) {
	    debug("Entering getcapteam");
	    SBTeam t = getCapTeam(cap.getPlayerName());
	    debug("Entering teamcanpick.");
	    if(!teamCanPick(t)) {
		pm(cap.getPlayerName(),"It's not your turn to pick.");
		return;
	    }
	    debug("entering tryaddplayer");
	    if(!tryAddPlayer(cap, args)) return;
	    debug("entering setnextteam");
	    if(t == currentTeam) setNextTeam();
	    // if t != currentTeam it means that the team that picked was making up for a
	    // !remove and picking out-of-order, so we won't advance the pick order.
	}

	private boolean teamCanPick(SBTeam team) {
	    if(currentTeam == team) {
		return true;
	    }
	    //this means that they had to !remove a player, so are due another pick
	    if(activePlayerCount(team) < activePlayerCount(currentTeam)) {
		return true;
	    }
	    return false;
	}

	private void setNextTeam() {
	    if(currentTeam == null)
		currentTeam = teams.get(0);
	    else {
		int index = teams.indexOf(currentTeam) + 1;
		index = index % teams.size();
		currentTeam = teams.get(index);
	    }
	    arena(currentTeam.getName() + ", your pick.");
	}

	private boolean tryAddPlayer(Player cap, String arg) {
	    String capName = cap.getPlayerName();
	    SBTeam team;
	    Player p = m_botAction.getFuzzyPlayer(arg);
	    String pName = p.getPlayerName();
	    SBTeam t = getCapTeam(capName);
	    if(!notPlayingCheck(capName, pName)) return false;
	    if(!eligiblePlayerCheck(capName,arg)) return false;
	    int freq = t.getFreq();

	    if(!havePlayerRec(pName)) createPlayerRec(pName);
	    getPlayerRec(pName).setTeam(t);
	    shipify(pName, t.getFreq());
	    getPlayerRec(pName).setStatus(SBPlayer.ACTIVE);
	    arena(pName + " is in for " + t.getName() + ".");
	    return true;
	}
    }

        // -- utility/convenience functions ---
    private boolean capCheck(String capName) {
	if(!isCaptain(capName)) {
	    pm(capName, "Only captains may use that command.");
	    return false;
	}
	return true;
    }

    private void shipify(String pName, int freq) {
	m_botAction.setShip(pName,(freq % 2) + 1);
	m_botAction.setFreq(pName, freq);
	getPlayerRec(pName).setStatus(SBPlayer.ACTIVE);
    }



    private void pm(String name, String message, int sound) {
	m_botAction.sendSmartPrivateMessage(name, message, sound);
    }
    private void pm(String name, String message) {
	pm(name,message,0);
    }

    private void arena(String message, int sound) {
	m_botAction.sendArenaMessage(message, sound);
    }

    private void arena(String message) {
	arena(message,0);
    }

    private SBPlayer getPlayerRec(String name) { return players.get(name.toLowerCase()); }
    private boolean havePlayerRec(String name) { return players.containsKey(name.toLowerCase()); }

    private boolean isActive(String name) {
	return havePlayerRec(name) && getPlayerRec(name).getStatus() == SBPlayer.ACTIVE;
    }

    private boolean isCaptain(String name) {
	for(SBTeam t : teams) { if(t.isCaptain(name)) return true; }
	return false;
    }

    private SBTeam getCapTeam(String capName) {
	if(capName == null) debug("WTF capname == null??");
	for(SBTeam t : teams) {
	    if(t.isCaptain(capName)) return t;
	}
	return null;
    }

    private boolean isPending(String name) {
	name = name.toLowerCase();
	if(!havePlayerRec(name) || getPlayerRec(name).getStatus() != SBPlayer.PENDING)
	    return false;
	return true;
    }

    private void createPlayerRec(String name) {
	players.put(name.toLowerCase(),new SBPlayer(name));
    }

    private int activePlayerCount(SBTeam team) {
	int count = 0;
	for(SBPlayer p : players.values()) {
	    if(p.getTeam() == team && p.getStatus() == SBPlayer.ACTIVE) count++;
	}
	return count;
    }

    //I know this is much more computationally expensive than it needs to be, but I will code
    //it for correctness first, and optimize later if it proves necessary.
    private int highestActivePlayerCount() {
	int count = 0;
	for(SBTeam t : teams) {
	    int c;
	    if((c = activePlayerCount(t))  > count) count = c;
	}
	return count;
    }

    private int pendingPlayerCount(SBTeam team) {
	int count = 0;
	for(SBPlayer p : players.values()) {
	    if(p.getTeam() == team && p.getStatus() == SBPlayer.PENDING) count++;
	}
	return count;
    }

    private boolean maxPlayerCheck(SBTeam team) {
	if(activePlayerCount(team) + pendingPlayerCount(team) >= MAXPLAYERS) {
	    pm(team.getCaptain(), "You already have the maximum amount of players entered or queued to enter.");
	    return false;
	}
	return true;
    }

    private boolean eligiblePlayerCheck(String capName, String arg) {
	Player p = m_botAction.getFuzzyPlayer(arg);
	if(p == null) { pm(capName, arg + "Is not present."); return false; }
	String pName = p.getPlayerName();
	if(isActive(pName)) { pm(capName, pName + " is already in the game."); return false; }
	if(isPending(pName)) { pm(capName, pName + " is already queued to enter."); return false; }
	if(isCaptain(pName) && !pName.equalsIgnoreCase(capName)) {
	    pm(capName,"You can't add another team's captain to your line.");
	    return false;
	}
	return true;
    }

    private boolean notPlayingCheck(String capName, String pName) {
	if(notPlayers.contains(pName.toLowerCase())) {
	    pm(capName, pName + " has enabled !notplaying.");
	    return false;
	}
	return true;
    }

    private SBPlayer getFuzzyPlayerRec(String arg) {
	SBPlayer pRec = null;
	for(String name : players.keySet()) {
	    if(name.equalsIgnoreCase(arg)) {
		pRec = getPlayerRec(arg);
		break;
	    }
	    if(name.toLowerCase().startsWith(arg.toLowerCase())) {
		pRec = getPlayerRec(name);
		//no break because we might find a perfect match later in the set
	    }
	}
	return pRec;
    }

    private void debug(String str) {
    }
}
