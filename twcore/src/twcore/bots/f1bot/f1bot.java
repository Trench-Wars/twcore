package twcore.bots.f1bot;

import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;

import java.util.*;

/**
 * Racing bot for arenas set up to match the racing track specification.  
 */
public class f1bot extends SubspaceBot {

    static f1bot static_f1bot;


    // --------------------------------------------------------------------------------------------------------------
    // Debug stuff
    // --------------------------------------------------------------------------------------------------------------

    boolean report_position  = false;



    // --------------------------------------------------------------------------------------------------------------
    // Bot mode constants
    // --------------------------------------------------------------------------------------------------------------

    //                                            STIMULUS                 | INITIAL ACTION                 | CONSTANT ACTION
     final int GAME_WAITING   = 1001;   // Host types !start        | Warping players to start_point | Warp back any players crossing the barrier for 10 seconds
     final int GAME_STARTED   = 1002;   // 10 seconds passed        | *arena GO! GO! GO!             | Monitor players for anyone completed 1 lap
     final int GAME_WON       = 1003;   // A player completed 1 lap | Announce the winner            | Announce the rest of the finishers for 30 seconds
     final int GAME_ENDED     = 1004;   // 30 seconds has passed    | Go back to idle mode           | Nothing to do, gone to idle mode already..
     final int GAME_IDLE      = 1005;   // !reset or otherwise      | The bot does nothing for sure  | Until..
     final int TIME_TRIAL     = 1006;


    // --------------------------------------------------------------------------------------------------------------
    // Game variables
    // --------------------------------------------------------------------------------------------------------------

    private Track[] tracks           = {new Track()};                           // Array of tracks
    private int current_track        = 0;                                       // The index of the track to race / being raced
    private int game_status          = GAME_ENDED;                              // General status of the bot/game
    private int total_laps           = 3;                                       // Total laps to play for this current race
    private int laps_completed       = 0;
    private Date time_race_started;                                             // Time game started
    private RaceRecords race_records_obj;                                       // Contains race-related information for each racer
                                                                                // Includes overall time taken.. each lap time.. etc


    // --------------------------------------------------------------------------------------------------------------
    // Text arrays
    // --------------------------------------------------------------------------------------------------------------

    String[] help_text =
    {
        "Command          Action",
        "--------------------------------",
        "!help            Displays this help",
        "!die             Send me to bot heaven",
        "!start           Start a test race",
        "!reset           Turn me off",
        "!mode            Displays current mode #",
        "!move <x> <y>    Move me to x,y",
        "!listplayers     List the players in-game",
        "!status          List the status of each player"
    };



    // --------------------------------------------------------------------------------------------------------------
    // Get / Set methods - Local
    // --------------------------------------------------------------------------------------------------------------

    public int getCurrentTrack(){
        return current_track;
    }

    public int getGameStatus(){
        return game_status;
    }

    public void setGameStatus(int game_status){
        this.game_status = game_status;
    }

    public int getTotalLaps(){
        return total_laps;
    }

    public int getLapsCompleted(){
        return laps_completed;
    }

    public void setLapsCompleted(int laps_completed){
        this.laps_completed = laps_completed;
    }

    public RaceRecords getRaceRecordsObj(){
	return race_records_obj;
    }

    public Date getTimeRaceStarted(){
        return time_race_started;
    }

    public void setTimeRaceStarted(Date time_race_started){
        this.time_race_started = time_race_started;
    }



    // --------------------------------------------------------------------------------------------------------------
    // Get / Set methods - External
    // --------------------------------------------------------------------------------------------------------------

    public int getTotalCheckpoints(){
        return tracks[current_track].checkpoints.length;
    }



    // --------------------------------------------------------------------------------------------------------------
    // Default constructor
    // --------------------------------------------------------------------------------------------------------------

    // Object constructor, in your own bots, only change the name.
    // Must be all lower case and match the filename and .cfg file.

    public f1bot( BotAction botAction )
    {
	super( botAction );                                                     // Every bot has to do this.  It calls the constructor of the superclass.
                                                                                // botAction can be accessed as m_botAction, any time.

	EventRequester events = m_botAction.getEventRequester();                // Request some events from the core:
	// First get the Event Requester object from the core.

	events.request( EventRequester.MESSAGE );                               // This is the syntax for requesting messages.  Almost all bots
	events.request( EventRequester.PLAYER_POSITION );                       // must have at least this much to start.  Look below, there is a method
	// for handling "Message" objects when they arrive.
	f1bot.static_f1bot = this;
        initialize();
    }


    // This method defines precisely how a bot behaves while it is attempting
    // to log into the zone.  This method is mostly the same on most bots, but
    // many times other such things need to be initialized when the bot
    // connects, not just in the bot's constructor.

    public void handleEvent( LoggedOn event ){
        /* Get the .cfg information from the core */
        BotSettings config = m_botAction.getBotSettings();

        /* Get the initial arena from the .cfg file */
        String initialArena = config.getString( "InitialArena" );

        /* Join the arena */
        m_botAction.joinArena( initialArena );
    }



    // ---------------------------------------------------------------------------------------------------
    //
    //                                   Bot life-cycle code
    //
    // ---------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------------
    // Prepare / Invoked by typing !start to the bot
    // ---------------------------------------------------------------------------------------------------

    public void initialize()
    {
        race_records_obj = new RaceRecords(this);
    }

    public void prepare(int current_track, int total_laps)
    {

        this.total_laps = total_laps;
        Track t = tracks[current_track];                                        // Reference to the current track object
        game_status = GAME_WAITING;                                           // Switch to GAME_PREPARING mode
        this.current_track = current_track;                                     // Remember what track the host said to play
        m_botAction.move(t.bot_warp.get_x(),t.bot_warp.get_y());                // Move the bot to monitor the appropriate area
        m_botAction.warpAllToLocation(  t.start_point.get_x(),                  // Warp all the players to the starting spot
                                        t.start_point.get_y() );
        m_botAction.sendArenaMessage("Race begins in 10 seconds",2);            // Notify players that game is starting

        Iterator it = m_botAction.getPlayerIDIterator();                        // Get a way to get all players in arena
        race_records_obj = new RaceRecords(it, this);                           // Create a new 'RaceRecords' out of it.

	// 'RaceRecords' holds info on eacn player's race progress

        TimerTask tt_startGame = new TimerTask() {                              // Create a 10 second timer
                public void run(){                                              // When time is up,
                    start();                                                    // The next method in the bot life-cycle will start
                };
	    };
        m_botAction.scheduleTask(tt_startGame, 10000);                           // Start the timer!
    }


    // ---------------------------------------------------------------------------------------------------
    // Start - Invoked by prepare timer finishing
    // ---------------------------------------------------------------------------------------------------
    public void start()
    {
        game_status =  GAME_STARTED;                                            // Switch to GAME_STARTED mode
        m_botAction.sendArenaMessage("GO!",104);                                // Tell the players it's time to GO!%104
        time_race_started = new Date();
    }

    // ---------------------------------------------------------------------------------------------------
    // WaitForFinishers - Invoked by first player winning
    // ---------------------------------------------------------------------------------------------------
    public void waitForFinishers()
    {
        TimerTask tt_waitForFinishers = new TimerTask() {
                public void run(){
                    announceResults();
                };
	    };
        m_botAction.scheduleTask(tt_waitForFinishers,15000);
    }

    public void announceResults()
    {
        String[] results = race_records_obj.getResultsArray();
        m_botAction.sendArenaMessage("Pos  Name                       Ship Total Time ",0);
        for(int i=0; i<results.length; i++)
        {
            m_botAction.sendArenaMessage(     getPaddedString(""+i,4)+""+      results[i],0);
        }
    }

//Pos  Name                       Ship Total Time
//0    Nockm <ER>                 1    12.359s


    public String getPaddedString(String inputString, int width)
    {
        String outputString = inputString + "";
        int limit = width - inputString.length();
        for(int i=0; i<limit; i++)
            outputString += " ";
        return outputString;
    }








    // ---------------------------------------------------------------------------------------------------
    // Reset - Invoked by !reset
    // ---------------------------------------------------------------------------------------------------
    public void reset(String name)
    {
        game_status         = GAME_ENDED;
        m_botAction.cancelTasks();
        m_botAction.sendArenaMessage("Bot reset by "+name,1);
    }

//    public void timeTrial()
//    {
//
//    }


    // ---------------------------------------------------------------------------------------------------
    //
    //                                   Event handling
    //
    // ---------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------------
    // PlayerPosition
    // ---------------------------------------------------------------------------------------------------

    public void handleEvent( PlayerPosition event )
    {
        // Get information about the event
        int x         = event.getXLocation();                                   // Current x-pos of the player
        int y         = event.getYLocation();                                   // Current y-pos of the player
        int playerID  = event.getPlayerID();
        String player = m_botAction.getPlayerName( playerID );                  // Name of the player
        Track t       = tracks[current_track];                                  // Reference to the current track object

        Point     c   = new Point(x,y);
        Point     c1  = t.start_barrier.get_p1();
        Point     c2  = t.start_barrier.get_p2();
        Box       b   = new Box(c1,c2);


        //m_botAction.sendArenaMessage(player+": "+x+","+y,1);
        // Anytime: Report position of messaging ship
//        if(report_position)
//	    {
//		m_botAction.sendArenaMessage(player+": "+x+","+y+"   (was "+report_position+")",1);
//		report_position = false;
//	    }


        // During GAME_PREPARING
        if(b.contains(c) && (game_status == GAME_WAITING))
	    {
		m_botAction.warpTo( event.getPlayerID(),
				    t.start_point.get_x(),
				    t.start_point.get_y()  );
	    }


        // During GAME_STARTED
        if((game_status == GAME_STARTED) || game_status == GAME_WON)
	    {
		for(int i=0; i<getTotalCheckpoints(); i++)
		    {
			if(t.checkpoints[i].contains(c))
			    {
                                getRaceRecordsObj().updateRacerProgress(playerID,i);
				//Racer racer = race_records_obj.getRacer(playerID);
				//m_botAction.sendArenaMessage(player+" passed through checkpoint "+i,1);
				//racer.updateProgress(i);
			    }
		    }
	    }
    }




    // ---------------------------------------------------------------------------------------------------
    // -- Event handling
    // -- Messages
    // ---------------------------------------------------------------------------------------------------

    public void handleEvent( Message event )
    {
	/* Every player has a player ID */
	int playerID = event.getPlayerID();

	/* Use BotAction to get the player's name as a String */
	String name = m_botAction.getPlayerName( playerID );



	int msgType = event.getMessageType();


	/* Send the player a message */
	if( msgType == Message.PRIVATE_MESSAGE )
	    {


                if(m_botAction.getOperatorList().isER(name))
                {//////////////

//		if(event.getMessage().equals("qwe"))
//		    {
//			m_botAction.sendArenaMessage("Flag = "+report_position,1);
//			report_position = true;
//		    }

		if(event.getMessage().equals("!help"))
		    {
			for(int i=0; i<help_text.length; i++)
			    m_botAction.sendSmartPrivateMessage( name , help_text[i], 26 );
		    }

		if(event.getMessage().equals("!die"))
		    m_botAction.die();

		if(event.getMessage().equals("!start"))
                {
		    prepare(0,1);
                }
		else if(event.getMessage().startsWith("!start"))
                {
			String input       = event.getMessage();
			StringTokenizer st = new StringTokenizer(input," ");
			String token;
			int x;
			token = st.nextToken();
			token = st.nextToken();
			try{x = Integer.parseInt(token);} catch(NumberFormatException e){x = 1;}

		    prepare(0,x);
                }

		if(event.getMessage().equals("!reset"))
		    reset(name);
//
//		if(event.getMessage().equals("!mode"))
//		    m_botAction.sendSmartPrivateMessage( name, "Game status: " + game_status,26);
//
//		if(event.getMessage().startsWith("!move "))
//		    {
//			String input       = event.getMessage();
//			StringTokenizer st = new StringTokenizer(input," ");
//			String token;
//			int x,y;
//			token = st.nextToken();
//			token = st.nextToken();
//			try{x = Integer.parseInt(token);} catch(NumberFormatException e){x = -1;}
//			token = st.nextToken();
//			try{y = Integer.parseInt(token);} catch(NumberFormatException e){y = -1;}
//			m_botAction.sendArenaMessage("Tried !move " + x + "," + y ,1);
//			m_botAction.move(x*16,y*16);
//		    }

		if(event.getMessage().startsWith("!status"))
		    {
			String[] statusArray = race_records_obj.toStringArray();
			for(int i=0; i<statusArray.length; i++)
			    m_botAction.sendSmartPrivateMessage( name , statusArray[i], 26 );
		    }
                }/////////////
	    }
    }
}


class Racer implements Comparable
{
    private int playerID;                                                       // The unique ID of the player
    private String playerName;                                                  // The unique ID of the player
    private int next_checkpoint;                                                // The next checkpoint index the racer will cross if he carries on in a legal fashion
    private int laps_completed;                                                 // The amount of laps the racer has already completed
    RaceRecords parent_race_records_obj;                                        // Parent object
    private boolean finished = false;                                           // Whether the player has finished or not..
    private int ship;

    RacerTimeRecords racer_time_records_object;                                 // Handy storage for players lap times


    public int compareTo(Object o)
    {
        long y = laps_completed - ((Racer)o).laps_completed;
        if(y!=0)
            return new Long(y).intValue();


        long x = ((Racer)o).racer_time_records_object.getTotalTimeLong() - racer_time_records_object.getTotalTimeLong();
        return new Long(x).intValue();
    }


    public String getResultString()
    {
        String timeString;
        if(finished)
            timeString = racer_time_records_object.getTotalTimeString()+"s";
        else
            timeString = "DNF";

        return " "+getPaddedString(getName(),27)+ship+"   "+timeString;
    }


    public String getPaddedString(String inputString, int width)
    {
        String outputString = inputString + "";
        int limit = width - inputString.length();
        for(int i=0; i<limit; i++)
            outputString += " ";
        return outputString;
    }




    public String toString()
    {
        String outputString2   = racer_time_records_object.toString();
        String outputString = "ID="+playerID+"  "+playerName+"  Next cp:"+next_checkpoint+"  Laps done:"+laps_completed;

        return outputString + outputString2;
    }

    public Racer(int playerID, RaceRecords parent_race_records_obj)
    {
        this.playerID   = playerID;
        this.parent_race_records_obj = parent_race_records_obj;
        this.playerName = getBotAction().getPlayerName(playerID);
        racer_time_records_object = new RacerTimeRecords();
        this.ship = getBotAction().getPlayer(playerID).getShipType();


        // FOR NOW... CONSTRUCTOR IS FOR STARTING BEFORE FINISH LINE!!!
        next_checkpoint = 4;
        laps_completed  = -1;
    }

    public RaceRecords getParentRaceRecordsObject()
    {
        return parent_race_records_obj;
    }

    public BotAction getBotAction()
    {
        return getParentRaceRecordsObject().getParentF1Bot().m_botAction;
    }

    public f1bot getGParentF1Bot()
    {
        return getParentRaceRecordsObject().getParentF1Bot();
    }

    public int getID()
    {
        return playerID;
    }

    public String getName()
    {
        return playerName;
    }

    public int getNextCheckpoint()
    {
        return next_checkpoint;
    }

    public int getLapsCompleted()
    {
        return laps_completed;
    }

    public synchronized void updateProgress(int checkpoint_passed)
    {
        int total_checkpoints = getParentRaceRecordsObject().getParentF1Bot().getTotalCheckpoints();

        if(checkpoint_passed == next_checkpoint)                                // If the player has completed the next checkpoint in turn
	    {
//		f1bot.static_f1bot.m_botAction.sendArenaMessage(" "+playerName+" progressed.. was "+next_checkpoint+", now "+(next_checkpoint+1),1);
		next_checkpoint++;                                              //    update the variable
	    }
        else if(checkpoint_passed == next_checkpoint - 2)                       // If the player has backtracked
	    {
//		f1bot.static_f1bot.m_botAction.sendArenaMessage(" "+playerName+" backtracked.. was "+next_checkpoint+", now "+(next_checkpoint-1),1);
		next_checkpoint--;
	    }
        if(next_checkpoint == total_checkpoints)                                // If racer has completed a lap..
	    {                                                                   // (the next checkpoint doesn't exist..)
//		f1bot.static_f1bot.m_botAction.sendArenaMessage(" "+playerName+" completed a lap.. laps++",1);
		next_checkpoint = 0;                                            //    and reset checkpoint_passed
		laps_completed++;                                               //    it's time to update the player's lap count
                racer_time_records_object.setTimeLapFinished(new Date(),-1);

                if(laps_completed > getGParentF1Bot().getLapsCompleted())
                {
                    getGParentF1Bot().setLapsCompleted(laps_completed);

                    if(getGParentF1Bot().getLapsCompleted() < getGParentF1Bot().getTotalLaps())
                        getBotAction().sendArenaMessage("Lap #"+laps_completed+" started by " + getName(),2);
                }

	    }
        if(!finished && getLapsCompleted() == getParentRaceRecordsObject().getParentF1Bot().getTotalLaps()) // if racer has completed all laps..
	    {
		finished = true;
                {
		    Date started  = getParentRaceRecordsObject().getParentF1Bot().getTimeRaceStarted();
      		    Date finished = new Date();

		    //f1bot.static_f1bot.m_botAction.sendArenaMessage(playerName + " finished!  Time takeN: "+mins+":"+secs+"."+ms,5);

                    String total_time = "";//racer_time_records_object.getTotalTimeString();

                    if(getGParentF1Bot().getGameStatus() == getGParentF1Bot().GAME_STARTED)
                    {
                        getGParentF1Bot().setGameStatus(getGParentF1Bot().GAME_WON);
                        getBotAction().sendArenaMessage(getName() + " wins!    "+" "+total_time+" ",5);
                        getParentRaceRecordsObject().getParentF1Bot().waitForFinishers();
                    }
                    else if(getGParentF1Bot().getGameStatus() == getGParentF1Bot().GAME_WON)// (the game is won)
                    {
                        getBotAction().sendArenaMessage(getName() + " finishes "+" "+total_time+" ",1);
                    }
                }
	    }

    }
}


class RaceRecords
{
    Vector racer_vector;                                                        // Vector of Racer objects
    f1bot parent_f1bot;                                                         // Reference to the bot from whence this came

    //  This object is constructed anew for every race (i.e. one for every GO!!%104).
    //  This object holds information of all the racer's progress,
    //  (i.e. how many checkpoints each player has legally completed).
    //
    //  How this object is used:
    //  When the PlayerLocation event handler finds a player has entered/passed checkpoint box,
    //  - it will call boolean RaceRecords.hasWon(int playerID, int checkpoint_passed).
    //  - boolean updatePlayerProgress() will return true if the player has completed the necessary # of laps.
    //
    //  The returing of 'true' has the following consequences:
    //  - 1. Stimulates the game_mode to change from GAME_STARTED to GAME_WON
    //  - 2. The bot will report a "%tickname has finished" style arena message.



    public String[] getResultsArray()
    {
        String[] resultsArray = new String[racer_vector.size()];

        for(int i=0; i<racer_vector.size(); i++)
        {
            resultsArray[i] = ((Racer)racer_vector.elementAt(i)).getResultString();
        }
        return resultsArray;
    }




    public RaceRecords(f1bot parent_f1bot)
    {
        this.parent_f1bot       = parent_f1bot;
        this.racer_vector       = new Vector();
    }

    public void addPlayer(int playerID)
    {
	Player p = getBotAction().getPlayer(playerID);   //    Derive a player object
	Racer r = new Racer(p.getPlayerID(), this);      //        Create a new racer object for the
	racer_vector.addElement(r);                      //        player and add it to the racer_vector
    }

    public RaceRecords(Iterator it, f1bot parent_f1bot)
    {
        this.parent_f1bot       = parent_f1bot;
        this.racer_vector       = new Vector();

        // Let's store the player IDs derived from the iterator
        Vector v = new Vector();                                                      // Get somewhere to store the player IDs
        while(it.hasNext())                                                           // While there are more player IDs to collect..
	    {
		v.add(it.next());                                                         //    Add it to the vector
	    }

            // Let's create a Racer object for each player in-game and store in the vector
        for(int i=0; i<v.size(); i++)                                                 // For each player ID in the vector,
	    {
		Player p = getBotAction().getPlayer(((Integer)v.elementAt(i)).intValue());   //    Derive a player object
		if(p.isPlaying())                                                         //    If it is a player not in spec..
		    {
			Racer r = new Racer(p.getPlayerID(), this);      //        Create a new racer object for the
			racer_vector.addElement(r);                      //        player and add it to the racer_vector
		    }

        // QUICK FIX!!
                if(p.getShipType()==8)
                    getBotAction().setShip(p.getPlayerID(),1);


	    }


    }



    public BotAction getBotAction()
    {
        return getParentF1Bot().m_botAction;
    }

    public f1bot getParentF1Bot()
    {
        return parent_f1bot;
    }

    public String[] toStringArray()
    {
        String[] outputStringArray = new String[racer_vector.size()+1];

        outputStringArray[0] = "Racer status:";

        for(int i=0; i<racer_vector.size(); i++)
        {
            outputStringArray[i+1] = ((Racer)racer_vector.elementAt(i)).toString();
        }
        return outputStringArray;
    }

    public synchronized void updateRacerProgress(int playerID, int checkpoint_passed)
    {
        Racer r = getRacer(playerID);
        if(r!=null)
            r.updateProgress(checkpoint_passed);
    }

    Racer getRacer(int playerID)
    {
        for(int i=0; i<racer_vector.size(); i++)
	    {
		Racer r = (Racer)racer_vector.elementAt(i);
		if(r.getID() == playerID)
		    return r;
	    }
        return null;
    }
}












































// Simple self contained race time holder.
// On race GO!, submit the setTimeRaceStarted
// If there is a lap time to submit, use setTimeLapFinished and enter the Date.
// Use getLapTime(int lap) to get the duration of a specific completed lap

class RacerTimeRecords
{
    private Date time_race_started;               // GO! GO! time
    private Vector time_laps_finished_vector = new Vector();     // Date objects.. elementAt(x) = time lap x finished

    public void setTimeRaceStarted(Date time_race_started){
        this.time_race_started = time_race_started;
    }

    public void setTimeLapFinished(Date time_lap_finished, int lap_number){
        time_laps_finished_vector.addElement(time_lap_finished);
    }

    public long getDurationMilliseconds(Date started, Date finished){
        return finished.getTime() - started.getTime();
    }

    public long getLapTime(int lap_number)
    {
        Date started  = (Date)time_laps_finished_vector.elementAt(lap_number-1);
        Date finished = (Date)time_laps_finished_vector.elementAt(lap_number);
        long duration = getDurationMilliseconds(started,finished);
        return duration;
    }

    public String getDurationString(long milliseconds)
    {
	long total_secs   = milliseconds/1000;
	long mins         = total_secs/60;
	long secs         = total_secs%60;
	long ms           = milliseconds%1000;
        long ts   = (60*mins)+secs;
        String returnString = ((ts>99)?(""+ts):(" "+ts))  + "." +  ((ms>99)?(""+ms):(ms>9)?("0"+ms):("00"+ms));
        return returnString;
    }

    public String toString()
    {
        String vectorString = " Size of laps finished vector: "+time_laps_finished_vector.size();


        String returnString = "";

        for(int i=1; i<time_laps_finished_vector.size(); i++)
        {
            returnString+= "["+i+": "+getDurationString(getLapTime(i))+"] ";
        }

        if(returnString.equals(""))
            returnString+= "No laps completed";


        for(int i=0; i<time_laps_finished_vector.size(); i++)
        {
            returnString+="["+
            ((Date)time_laps_finished_vector.elementAt(i)).getTime()
            +"]";
        }

        return returnString + vectorString;
    }

    public long getTotalTimeLong()
    {
        if(time_laps_finished_vector.size() == 0)
            return -1;

        long returnLong = getDurationMilliseconds(
            (Date)time_laps_finished_vector.elementAt(0),
            (Date)time_laps_finished_vector.elementAt(time_laps_finished_vector.size()-1)
        );

        return returnLong;
    }

    public String getTotalTimeString()
    {
        return getDurationString(getTotalTimeLong());
    }
}








/* Bits of useful unused code:
   m_botAction.sendSmartPrivateMessage( name, "Hello, "+name+"!" );
   m_botAction.sendSmartPrivateMessage( name,  );
   java.util.Date m_timeStarted;
   java.util.Date m_timeEnded;
*/



class Track
{
    Point   bot_warp;                 // Where the bot has to be warped to, to watch for player locations
    String  name;                     // Name of the track
    String  description;              // Artistic/informative description of the track.
    Point   start_point;              // Where the players are initially warped to.
    Point   start_grid;               // Where the starting grid boxes are.
    Box     start_barrier;            // Area before start where players crossing into it will be warped back to start_point.
    Box[]   checkpoints;              // Areas a player must run through in order to qualify as completed a lap. (Last is the finish)
    // Checkpoint logic:  Each player has a boolean array representing whether each checkpoint has been crossed.
    //                    Each boolean element can only be true if the previous element is true already.
    public Track()
    {
        bot_warp      = new Point(575*16,440*16);//560,440
        name          = "Albert Park";
        description   = "A wide speedy track that will test your reflexes.";
        start_point   = new Point(612,468);
        start_barrier = new Box(new Point(9712,7300), new Point(9269,7650));
        //start_barrier = new Box(new Point(0   ,0   ), new Point(7267,7496));

        Box cp1 = new Box(new Point( 7300,7650), new Point( 8100,7300));
        Box cp0 = new Box(new Point( 8200,7650), new Point( 8900,7300));
        Box cp3 = new Box(new Point( 9000,7650), new Point( 9712,7300));
        Box cp2 = new Box(new Point( 9800,7650), new Point(10600,7300));


	//        Box cp0 = new Box(new Point(9529,7633),  new Point(9290,7390));
	//        Box cp1 = new Box(new Point(8839,7633),  new Point(8398,7390));
	//        Box cp2 = new Box(new Point(11428,7631),new Point(10972,7392));
	//        Box cp3 = new Box(new Point(9711,7390),  new Point(9551,7633));
        checkpoints = new Box[] {cp0,cp1,cp2,cp3};
    }

}






// Represents an x,y coordinate
class Point
{
    private int x;
    private int y;

    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int get_x(){
        return x;
    }

    public int get_y(){
        return y;
    }
}

// Represents a box (rectangle)
class Box
{
    private Point p1;
    private Point p2;

    public Box(Point p1, Point p2){
        this.p1 = p1;
        this.p2 = p2;
    }

    public Point get_p1(){
        return p1;
    }

    public Point get_p2(){
        return p2;
    }

    // Checks if particle p is in the bounds of a rectangle
    // defined by two corners c1 and c2
    public boolean contains(Point p){
        int  p_x =  p.get_x();
        int  p_y =  p.get_y();
        int c1_x = p1.get_x();
        int c1_y = p1.get_y();
        int c2_x = p2.get_x();
        int c2_y = p2.get_y();

        // Make c1_xy have the lowest coordinates.
        if(c1_x > c2_x)
	    {
		int temp  = c2_x;
		c2_x      = c1_x;
		c1_x      = temp;
	    }

        // Make c2_xy have the largest coordinates.
        if(c1_y > c2_y)
	    {
		int temp  = c2_y;
		c2_y      = c1_y;
		c1_y      = temp;
	    }

        //f1bot.static_f1bot.m_botAction.sendArenaMessage(c1_x+" "+p_x+" "+c2_x+"     "+c1_y+" "+p_y+" "+c2_y,1);

        // Now boundary checking is easier
        // Check that p is in between c1 and c2, for both axes
        if(c1_x < p_x  &&  p_x < c2_x  &&
           c1_y < p_y  &&  p_y < c2_y    )
            return true;
        else
            return false;
    }
}
