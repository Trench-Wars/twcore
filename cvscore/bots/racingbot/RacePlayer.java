package twcore.bots.racingbot;

import java.util.ArrayList;

public class RacePlayer {
	
	int laps  = 0;
	int check = 0;
	
	int lastPointOnTrack;
	int lastCheck = 0;
		
	public RacePlayer( int last ) {
		lastPointOnTrack = last;
	}
	
	public int hitPoint( int checkPoint, String name, Track currentTrack) {
		//System.out.println( "Current CP: " + checkPoint + "   Last CP: " + lastCheck + "   LPOT: " + lastPointOnTrack );
		if( lastCheck+1 == checkPoint ) {
			lastCheck = checkPoint;
		} else if( lastCheck == lastPointOnTrack && checkPoint == 1 ) {
			laps++;
			lastCheck = checkPoint;
			if(laps > 0)
			{
				ArrayList positions = currentTrack.playerPositions;
				if(laps < positions.size())
				{
					ArrayList thisLap = (ArrayList)positions.get(laps);
					ArrayList lastLap = (ArrayList)positions.get(laps - 1);
					
					lastLap.remove(lastLap.indexOf(name.toLowerCase()));
					positions.remove(laps - 1);
					positions.add(laps - 1, lastLap);
					
					thisLap.add(name.toLowerCase());
					positions.remove(laps);
					positions.add(laps, thisLap);
				}
				else
				{
					ArrayList thisLap = new ArrayList();
					ArrayList lastLap = (ArrayList)positions.get(laps - 1);
						
					lastLap.remove(lastLap.indexOf(name.toLowerCase()));
					positions.remove(laps - 1);
					positions.add(laps - 1, lastLap);
					
					thisLap.add(name.toLowerCase());
					positions.add(thisLap);
				}
			}
			return laps;
		}
		
		return -1;
	}	
}