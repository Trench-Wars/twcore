package twcore.bots.racebot;

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
				ArrayList<ArrayList<String>> positions = currentTrack.playerPositions;
				if(laps < positions.size())
				{
					ArrayList<String> thisLap = positions.get(laps);
					ArrayList<String> lastLap = positions.get(laps - 1);

                    // TODO: Implemented very BASIC check to prevent out of bounds...
                    //       needs verification/logic check.
					int indexOfName = lastLap.indexOf(name.toLowerCase());
					if( indexOfName >= 0 && indexOfName < lastLap.size() )
					    lastLap.remove(indexOfName);
					positions.remove(laps - 1);
					positions.add(laps - 1, lastLap);

					thisLap.add(name.toLowerCase());
					positions.remove(laps);
					positions.add(laps, thisLap);
				}
				else
				{
					ArrayList<String> thisLap = new ArrayList<String>();
					ArrayList<String> lastLap = positions.get(laps - 1);

                    // TODO: Implemented very BASIC check to prevent out of bounds...
                    //       needs verification/logic check.
                    int indexOfName = lastLap.indexOf(name.toLowerCase());
                    if( indexOfName >= 0 && indexOfName < lastLap.size() )
                        lastLap.remove(indexOfName);
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