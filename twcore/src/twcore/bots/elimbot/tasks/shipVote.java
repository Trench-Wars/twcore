package twcore.bots.elimbot.tasks;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.elimbot.ElimState;
import twcore.bots.elimbot.elimbot;
import twcore.core.util.Tools;

public class shipVote extends TimerTask {

	private elimbot bot;
	
	public shipVote(elimbot bot) {
		this.bot = bot;
	}
	
	public void run()
	{
		if(bot.state != ElimState.SHIPVOTE) {
			return;		// "Cancel" this task if the bot isn't in the expected status anymore
		}
		
		int voters = bot.votes.size();				// Number of total players who voted
		int voteWin = countVotes();					// Vote option that won
		int voteWinners = countVotes(voteWin);		// Number of players that voted for the winning option
		
		if(voters > 0)	{
			float voteWinnersPerc = (100 * voteWinners)/voters;
			bot.m_botAction.sendArenaMessage("VOTE RESULT: "+Math.round(voteWinnersPerc)+"% of "+voters+" players voted for ship #"+voteWin+" - "+Tools.shipName(voteWin).toUpperCase()+".");
		} else {
			voteWin = bot.getConfiguration().getCurrentConfig().getShipsDefault();
			String shipName = Tools.shipName(voteWin);
			bot.m_botAction.sendArenaMessage("VOTE RESULT: 0 votes. Defaulted to "+shipName+".");
		}
		
		bot.ship = voteWin;
		bot.state = ElimState.SHIPVOTE;
		bot.step();
	}
	
	/**
	 * Counts the votes and returns the number that won the most votes
	 * @return the number that won the most votes
	 */
	private int countVotes() {
		Iterator<Integer> voteValues = bot.votes.values().iterator();
		int[] voteResult = new int[9];
		int voteWin = 2;
		
		while(voteValues.hasNext()) {
			int vote = voteValues.next().intValue();
			voteResult[vote] = voteResult[vote] + 1; 
		}
		
		int tmp =0;
		for(int i = 0 ; i < voteResult.length ; i++) {
			if(voteResult[i] > tmp) {
				tmp = voteResult[i];
				voteWin = i;
			}
		}
		
		return voteWin;
	}
	
	/**
	 * Counts the number of votes for the given option
	 * @param option
	 * @return
	 */
	private int countVotes(int option) {
		Iterator<Integer> voteValues = bot.votes.values().iterator();
		
		int votes = 0;
		while(voteValues.hasNext()) {
			int vote = voteValues.next().intValue();
			if(vote == option) {
				votes++;
			}
		}
		
		return votes;
	}
}
