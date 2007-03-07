package twcore.bots.elimbot;

import java.util.Iterator;
import java.util.TimerTask;

public class deathlimitVote extends TimerTask {

	private elimbot bot;
	
	public deathlimitVote(elimbot bot) {
		this.bot = bot;
	}
	
	public void run()
	{
		if(bot.state != ElimState.DEATHLIMITVOTE) {
			return;		// "Cancel" this task if the bot isn't in the expected status anymore
		}
		
		int voters = bot.votes.size();				// Number of total players who voted
		int voteWin = countVotes();					// Vote option that won
		int voteWinners = countVotes(voteWin);		// Number of players that voted for the winning option
		if(voters == 0)	voters = 1;					// Prevent divide by zero errors
		int voteWinnersPerc = (100 * voteWinners)/voters;
		
		bot.m_botAction.sendArenaMessage("VOTE RESULT: "+voteWinnersPerc+"% of "+voters+" players voted for death limit of "+voteWin+" .");
		bot.deathLimit = voteWin;
		bot.state = ElimState.DEATHLIMITVOTE;
		bot.start();
	}
	
	/**
	 * Counts the votes and returns the number that won the most votes
	 * @return the number that won the most votes
	 */
	private int countVotes() {
		Iterator<Integer> voteValues = bot.votes.values().iterator();
		int[][] voteResult = new int[9][1];
		int voteWin = 3;
		
		while(voteValues.hasNext()) {
			int vote = voteValues.next().intValue();
			
			if(vote < 9) {
				voteResult[vote][0]++; 
			}
		}
		
		int tmp =0;
		for(int i = 0 ; i < voteResult.length ; i++) {
			if(voteResult[i][0] < tmp) {
				tmp = voteResult[i][0];
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
