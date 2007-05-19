package twcore.bots.sbbot;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import twcore.core.BotAction;


public class SBStatKeeper {
    private BotAction m_botAction;
    private SBMatchOperator matchOp;
    private MatchStats[]  matchHistory;
    private int curIndex;
    private int historyLength;

    public SBStatKeeper(BotAction botAction, SBMatchOperator o) {
	m_botAction = botAction;
	matchOp = o;

	matchOp.addListener(SBMatchOperator.GOAL, new GoalHandler());
	matchOp.addListener(SBMatchOperator.THEDROP, new DropHandler());
	matchOp.addListener(SBMatchOperator.GAMEOVER, new GameOverHandler());
	matchOp.addListener(SBMatchOperator.POSTGAME, new PostGameHandler());
	curIndex = 0;
	historyLength = 5;
	matchHistory = new MatchStats[historyLength];
	for(int i = 0; i < matchHistory.length; i++) {
	    matchHistory[i] = new MatchStats();
	}
    }

    private class PostGameHandler extends SBEventListener {
	public void notify(SBEventType type, SBEvent event) {
	    m_botAction.sendSmartPrivateMessage("arceo", "Entering postgame");
	    /*  Maybe when I figure out how to do a nicer graph
	    String[] graph = getGoalGraph(curIndex);
	    if(graph == null) {
		return;
	    }
	    for(String line : graph) {
		m_botAction.sendArenaMessage(line);
	    }
	    curIndex++;
	    */

	    m_botAction.sendArenaMessage(getMVPString(), 7);
	}
    }

    private String getMVPString() {
	HashMap<String,Integer> goals = new HashMap<String,Integer>();

	for(GoalRecord g : matchHistory[curIndex].goals) {
	    if(!goals.containsKey(g.playerName.toLowerCase()))
		goals.put(g.playerName.toLowerCase(), 0);
	    goals.put(g.playerName.toLowerCase(),goals.get(g.playerName.toLowerCase())+1);
	}
	String curHighestName = "";
	int curHighestCount = 0;
	for(String name : goals.keySet()) {
	    if(goals.get(name.toLowerCase()) > curHighestCount) {
		curHighestCount = goals.get(name.toLowerCase());
		curHighestName = name;
	    }
	}
	return "MVP:" + curHighestName + " with " + curHighestCount + " goals!";
    }

    private String[] getGoalGraph(int idx) {
	MatchStats match = matchHistory[idx];
	// Tried to get stats for a match that was never fully recorded
	if(match.endTime == null) {
	    //throw new Exception("Tried to print graph for nonexistent match.");

	    return null;
	    //match.endTime = new Date();
	}

	/* This can be done more efficiently, but I'll shoot for correctness for now.
	 * notes:
	 *    I chose 70 for the length just based on the fact that lines for the
	 *    duelbot's !help are 78 chars long.  Height is just hardcoded as 10
	 *    for now due to ease of implementation.  It would be nice to have it
	 *    made variable in the future to account for arbitrary number of goals.
	 *    The final graph printout is 72 chars wide due to borders.  Must be at
	 *    least 32.
	 */
	int graphLength = 70;
	char[] borderFiller = new char[graphLength];
	for(int i = 0; i < graphLength; i++) {
	    borderFiller[i] = '-';
	}
	String border = "|" + new String(borderFiller) + "|";

	char[][] graphBody = new char[10][];
	for(int i = 0; i < graphBody.length; i++) {
	    graphBody[i] = new char[graphLength];
	    for(int j = 0; j < graphBody[i].length; j ++) {
		graphBody[i][j] = ' ';
	    }
	}
	long gameLength = match.endTime.getTime() - match.startTime.getTime();
	long increment = gameLength / graphLength;

	int[] curScore = new int[2];
	int[] lastPos = new int[2];

	for(GoalRecord r : match.goals) {
	    long curPos = (r.time.getTime() - match.startTime.getTime()) / increment;
	    // possible due to rounding.  Should be extremely unlikely though.
	    if(curPos >= graphLength) curPos = graphLength - 1;
	    int oldScore = curScore[r.team]++;
	    if(oldScore > 0) {
		int lineNum = 10 - oldScore;
		for(int i = lastPos[r.team]; i <= curPos; i++) {
		    if(graphBody[lineNum][i] != ' ')
			graphBody[lineNum][i] = '*';
		    else if(r.team == 0)
			graphBody[lineNum][i] = '+';
		    else
			graphBody[lineNum][i] = 'x';

		    if(i == curPos) {
			if(graphBody[lineNum-1][i] != ' ')
			    if((r.team == 0 && graphBody[lineNum-1][i] != '+') ||
			       (r.team == 1 && graphBody[lineNum-1][i] != 'x'))
				graphBody[lineNum-1][i] = '*';
			else if (r.team == 0)
			    graphBody[lineNum-1][i] = '+';
			else
			    graphBody[lineNum-1][i] = 'x';
		    }
		}
	    }
	    lastPos[r.team] = (int) curPos;
	}

	String[] graph = new String[17];
	graph[0] = border;
	for(int i = 0; i < graphBody.length; i++) {
	    graph[i+1] = "|" + (new String(graphBody[i])) + "|";
	}
	graph[11] = border;
	graph[12] = String.format("| %-" + (graphLength-2) + "s |", "Legend:");
	graph[13] = String.format("| %-20s -- +++++ %" + (graphLength-31) + "s|", match.team0, " ");
	graph[14] = String.format("| %-20s -- xxxxx %" + (graphLength-31) + "s|", match.team1, " ");
	graph[15] = String.format("| %-20s -- ***** %" + (graphLength-31) + "s|", "Scores tied", " ");
	graph[16] = border;
	return graph;
    }

    public void newGame(String[] teamNames) {
	matchHistory[curIndex].initialize();
	matchHistory[curIndex].team0 = teamNames[0];
        matchHistory[curIndex].team1 = teamNames[1];
    }

    private class DropHandler extends SBEventListener {
	public void notify(SBEventType type, SBEvent event) {
	    pm("arceo","StatKeeper got drop!");
	    matchHistory[curIndex].recording = true;
	    matchHistory[curIndex].startTime = new Date();
	}
    }

    private class GoalHandler extends SBEventListener {
	public void notify(SBEventType type, SBEvent event) {
	    m_botAction.sendSmartPrivateMessage("arceo", "Goal scored by " + event.player.getPlayerName());
	    if(!matchHistory[curIndex].recording) return;
	    matchHistory[curIndex].goals.add(new GoalRecord(event));
	}
    }

    private class GameOverHandler extends SBEventListener {
	public void notify(SBEventType type, SBEvent event) {
	    pm("arceo", "StatKeeper got gameOver!");
	    if(matchHistory[curIndex].recording == false) return;
	    matchHistory[curIndex].recording = false;
	    matchHistory[curIndex].endTime = new Date();
	}
    }

    private class MatchStats {
	public String team0;
	public String team1;
	public Vector<GoalRecord> goals;
	public Date startTime;
	public Date endTime;
	public boolean recording;

	public MatchStats() { initialize(); }

	public void initialize() {
	    team0 = null;
	    team1 = null;
	    goals = new Vector<GoalRecord>();
	    startTime = null;
	    endTime = null;
	    recording = false;
	}
    }

    private class GoalRecord {
	public final String playerName;
	public final Date time;
	// It's possible that someone will score for both freqs due to subs etc.
	public final int team;

	public GoalRecord(SBEvent event) {
	    playerName = event.player.getPlayerName();
	    time = event.time;
	    team = event.player.getFrequency();
	}
    }

    private void pm(String name, String message) {
	m_botAction.sendSmartPrivateMessage(name,message);
    }
}