package twcore.bots.purepubbot;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.purepubbot.pubitem.PubItem;
import twcore.bots.purepubbot.pubsystemstate.PubPointStore;
import twcore.bots.purepubbot.pubsystemstate.PubPointStoreOff;
import twcore.bots.purepubbot.pubsystemstate.PubPointStoreOn;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.KotHReset;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.lvz.Objset;
import twcore.core.util.Point;
import twcore.core.util.Tools;

/**
 * "Pure" pub bot that can enforce ship restrictions, freq restrictions, and run
 * a timed pub game using a flag. (Note that for non-TW zones, the warp points for
 * flag time games must be set up before use.)
 *
 * Restrictions for any ship can be easily enforced using this bot.  Each restriction
 * should be marked in this format in the CFG: (BotName)Ship(#)=(Value), e.g., if
 * the bot's name is MyPurePub, to completely restrict ship 1, one would use
 * MyPurePubShip1=0, and to allow ship 3, one would use MyPurePubShip3=1.  All
 * playable ships 1-8 must be defined for each bot.  Ship 0 is autodefined as 1.
 *
 *   Values:
 *   0  - No ships of this type allowed
 *   1  - Unlimited number of ships of this type are allowed
 *   #  - If the number of current ships of the type on this frequency is
 *        greater than the total number of people on the frequency divided
 *        by this number (ships of this type > total ships / weight), then the
 *        ship is not allowed.  The exception to this rule is if the player is
 *        the only one on the freq currently in the ship.
 *
 * For example, to say that only half the ships on a freq are allowed to be javs:
 * MyPurePub2=2, and for only a fifth of the ships allowed to be terrs, MyPurePub=5.
 * See JavaDocs of the checkPlayer(int) method for more information.
 *
 *
 * (NOTE: purepubbot is different than the pub bot module and hub system.  Pubhub /
 * Pubbot answers queries about player aliases, spies for certain words, monitors
 * TKs, informs people of messages received, and can perform any other task necessary
 * in a pub -- particularly ones that require a way to verify when a person logs on.)
 *
 * @author qan / original idea and bot by Cpt. Guano!
 * @see pubbot; pubhub
 */
public class purepubbot extends ItemObserver
{
    private boolean nextCantBuy;
    private PubPointStore pubStoreSystem;
    
    private Map<String, PubPlayer> players;
    
    private Map<Integer, Integer> shipPoints;
    private Map<String, Integer> areaPoints;
    private BotSettings botSets;

 
    
    public static final int SPEC = 0;                   // Number of the spec ship
    public static final int FREQ_0 = 0;                 // Frequency 0
    public static final int FREQ_1 = 1;                 // Frequency 1
    private static final int FLAG_CLAIM_SECS = 3;		// Seconds it takes to fully
                                                        // claim a flag
    private static final int INTERMISSION_SECS = 90;	// Seconds between end of round
                                                        // and start of next
    private static final int NUM_WARP_POINTS_PER_SIDE = 6; // Total number of warp points
                                                           // per side of FR
    private static final int MAX_FLAGTIME_ROUNDS = 5;   // Max # rounds (odd numbers only)

    private static final int KEEP_MVP_FREQSIZE_DIFF = 2;// Max # difference in size of freqs required
                                                        //   for a player to keep MVP/get bonus on switching.
    private static final int MSG_AT_FREQSIZE_DIFF = 4;  // Max # difference in size of freqs before
                                                        //   bot requests players even frequencies.
                                                        //   Value of -1 disables this feature.
    private static final int NICEGUY_BOUNTY_AWARD = 25; // Bounty given to those that even freqs/ships

    private static final int TERR_QUOTA = 2;            // "Ideal" number of ships.  Used in order to
    private static final int SHARK_QUOTA = 2;           // allow players to change to needed ships
    private static final int SPIDER_QUOTA = 3;          // w/o losing MVP status.
    private static final int JAV_TOOMANY = 3;           // Point at which !team complains that there
    private static final int WB_TOOMANY = 4;            // are too many of a given ship.  Used to
    private static final int WEASEL_TOOMANY = 3;        // better evaluate the ideal team.
    private static final int TERR_TOOMANY = 4;

    private static final int TOP_FR = 248;              // Coords forming boxes in which players
    private static final int BOTTOM_FR = 292;           // may be located: FR, mid and lower.  Spawn
    private static final int LEFT_FR = 478;             // and roof are defined by single Y coords
    private static final int RIGHT_FR = 546;            // and are checked after other boxes to determine
    private static final int TOP_MID = 287;             // a player's location.  Boxes can overlap.
    private static final int BOTTOM_MID = 334;
    private static final int LEFT_MID = 463;
    private static final int RIGHT_MID = 561;
    private static final int TOP_LOWER = 335;
    private static final int BOTTOM_LOWER = 395;
    private static final int LEFT_LOWER = 424;
    private static final int RIGHT_LOWER = 600;
    private static final int TOP_SPAWN_AREA = 396;
    private static final int BOTTOM_ROOF = 271;

    private OperatorList opList;                        // Admin rights info obj
    private HashSet <String>freq0List;                  // Players on freq 0
    private HashSet <String>freq1List;                  // Players on freq 1
    private HashMap <String,Integer>playerTimes;        // Roundtime of player on freq
    private boolean started;                            // True if pure pub is enabled
    private boolean privFreqs;                          // True if priv freqs are allowed
    private boolean flagTimeStarted;                    // True if flag time is enabled
    private boolean strictFlagTime;                     // True for autowarp in flag time
    private boolean teamsUneven;                        // True if teams are uneven as given in MAX_FREQSIZE_DIFF
    private boolean allEntered;                         // True after bot has completed entering arena
    private boolean autoWarp;                           // Whether to add players to !warp by default
    private boolean warpAllowed;                        // Whether !warp is allowed or not
    private int[] freqSizeInfo = {0, 0};                // Index 0: size difference; 1: # of smaller freq
    private FlagCountTask flagTimer;                    // Flag time main class
    private StartRoundTask startTimer;                  // TimerTask to start round
    private IntermissionTask intermissionTimer;         // TimerTask for round intermission

    private AuxLvzTask scoreDisplay;					// Displays score lvz
    private AuxLvzTask scoreRemove;						// Removes score lvz

    private ToggleTask toggleTask;                      // Toggles commands on and off at a specified interval
    private TimerTask entranceWaitTask;
    private int flagMinutesRequired;                    // Flag minutes required to win
    private int freq0Score, freq1Score;                 // # rounds won
    private boolean initLogin = true;                   // True if first arena login
    private int initialPub;                             // Order of pub arena to defaultjoin
    private String initialSpawn;                        // Arena initially spawned in
    private Vector <Integer>shipWeights;                // "Weight" restriction per ship
    private List <String>warpPlayers;                   // Players that wish to be warped
    private List <String>authorizedChangePlayers;       // Players authorized to change ship & not lose MVP
    private List <String>mineClearedPlayers;            // Players who have already cleared mines this round
    private Objset objs;                                // For keeping track of counter
    private LvzHandler lvzPubPointsHandler;
    // X and Y coords for warp points.  Note that the first X and Y should be
    // the "standard" warp; in TW this is the earwarp.  These coords are used in
    // strict flag time mode.
    private int warpPtsLeftX[]  = { 487, 505, 502, 499, 491, 495 };
    private int warpPtsLeftY[]  = { 255, 260, 267, 274, 279, 263 };
    private int warpPtsRightX[] = { 537, 519, 522, 525, 529, 533 };
    private int warpPtsRightY[] = { 255, 260, 267, 274, 263, 279 };

    // April fools map warp points
    private int warpPtsLeftX_April1[]  = { 487, 505, 502, 499, 491, 495 };
    private int warpPtsLeftY_April1[]  = { 255, 260, 267, 274, 279, 263 };
    private int warpPtsRightX_April1[] = { 537, 519, 522, 525, 529, 533 };
    private int warpPtsRightY_April1[] = { 255, 260, 267, 274, 263, 279 };

    private boolean useAprilFoolsPoints = false;

    // Warp coords for safes (for use in strict flag time mode)
    private static final int SAFE_LEFT_X = 306;
    private static final int SAFE_LEFT_Y = 482;
    private static final int SAFE_RIGHT_X = 717;
    private static final int SAFE_RIGHT_Y = 482;

    // Voting system
    boolean m_votingEnabled = false;                // True if players can vote on gametype
    long m_lastVote = 0;                            // Time at which last vote was started
    // Min time between the last vote before the next can be started
    private static final int MIN_TIME_BETWEEN_VOTES    =  5 * Tools.TimeInMillis.MINUTE;
    private static final int TIME_BETWEEN_VOTE_ADVERTS = 20 * Tools.TimeInMillis.MINUTE;
    // How long a vote runs for before it's stopped and the results are tallied
    private static final int VOTE_RUN_TIME             = 40 * Tools.TimeInMillis.SECOND;
    HashMap <String,Integer>m_votes  = new HashMap<String,Integer>(); // Mapping of playernames
                                                                      // to # voted.
    Vector <VoteOption>m_voteOptions = new Vector<VoteOption>();      // Options allowed for voting
    int m_currentVoteItem = -1;     // Current # being voted on; -1 if none
    TimerTask m_voteInfoAdvertTask;

    // Challenge    
    boolean m_challengeEnabled = false;
    LinkedList <PubChallenge>m_challenges = new LinkedList<PubChallenge>();

    /**
     * Creates a new instance of purepub bot and initializes necessary data.
     *
     * @param Reference to bot utility class
     */
    public purepubbot(BotAction botAction)
    {
        super(botAction);
        requestEvents();
        botSets = m_botAction.getBotSettings();
        
        this.allowedPlayersToUseItem = new Vector<String>();
        this.pubStoreSystem = new PubPointStoreOff();
        this.players = new HashMap<String, PubPlayer>();
       
        this.shipPoints = new HashMap<Integer, Integer>();
        this.areaPoints = new HashMap<String, Integer>();
        initializePoints();
        
        opList = m_botAction.getOperatorList();
        freq0List = new HashSet<String>();
        freq1List = new HashSet<String>();
        playerTimes = new HashMap<String,Integer>();
        started = false;
        privFreqs = true;
        flagTimeStarted = false;
        strictFlagTime = false;
        teamsUneven = false;
        allEntered = false;
        autoWarp = true;
        warpAllowed = true;
        warpPlayers = Collections.synchronizedList( new LinkedList<String>() );
        authorizedChangePlayers = Collections.synchronizedList( new LinkedList<String>() );
        mineClearedPlayers = Collections.synchronizedList( new LinkedList<String>() );
        shipWeights = new Vector<Integer>();
        objs = m_botAction.getObjectSet();

        this.lvzPubPointsHandler = new LvzHandler(m_botAction);
        
        // Small hack to use different warp coordinates with april fools map.
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar april1 = new GregorianCalendar(2008,GregorianCalendar.APRIL,1,0,0,0);
        GregorianCalendar april2 = new GregorianCalendar(2008,GregorianCalendar.APRIL,2,0,0,0);
        if( now.after(april1) && now.before(april2) )
            useAprilFoolsPoints = true;

        entranceWaitTask = new TimerTask() {
            public void run() {
                allEntered = true;
            }
        };
        m_botAction.scheduleTask( entranceWaitTask, 3000 );
        setupVotingOptions();
        Integer vo = botSets.getInteger(m_botAction.getBotName() + "Voting");
        
        if( vo == null || vo == 0 )
            m_votingEnabled = false;
        else {
        	m_votingEnabled = true;
        	m_voteInfoAdvertTask = new TimerTask() {
        		public void run() {
        			m_botAction.sendArenaMessage( "[Pub Voting]  !listvotes to see options; !startvote to change." );
        		}
        	};
        	m_botAction.scheduleTask( m_voteInfoAdvertTask, 5 * Tools.TimeInMillis.MINUTE, TIME_BETWEEN_VOTE_ADVERTS );
        }
    }

    /**
     * Gets default settings for the points: area and ship
     * */
    public void initializePoints(){
        
        areaPoints.put("flagroom", botSets.getInteger("pointlocation1"));
        areaPoints.put("mid", botSets.getInteger("pointlocation2"));
        areaPoints.put("spawn", botSets.getInteger("pointlocation3"));
        
        shipPoints.put(1, botSets.getInteger("pointship1"));
        shipPoints.put(2, botSets.getInteger("pointship2"));
        shipPoints.put(3, botSets.getInteger("pointship3"));
        shipPoints.put(4, botSets.getInteger("pointship4"));
        shipPoints.put(5, botSets.getInteger("pointship5"));
        shipPoints.put(6, botSets.getInteger("pointship6"));
        shipPoints.put(7, botSets.getInteger("pointship7"));
        shipPoints.put(8, botSets.getInteger("pointship8"));
    }
   
    
    @Override
    public void update(boolean nextCanBuy){
        this.nextCantBuy = nextCanBuy;
    }
    @Override
    public void update(String playerName) {
        // TODO Auto-generated method stub
        boolean isInList = this.allowedPlayersToUseItem.contains(playerName) ? true : false;
        if(!isInList)
            this.allowedPlayersToUseItem.add(playerName);
        else
            allowedPlayersToUseItem.remove(playerName);
        
        
    }
    
    @Override
    public void update(String playerName, String whatToEnableDisable, int time) {
        // TODO Auto-generated method stub
        String split[] = whatToEnableDisable.split(" ");
        String enableOrDisable = split[1].equals("1")? "enabled":"disabled";
        doSetCmd(playerName, whatToEnableDisable);
        m_botAction.sendPrivateMessage(playerName, "You've "+enableOrDisable+" the ship "+split[0]+" for "+time+" minutes.");
    
    }

    public void buyItem(String playerName, String itemName, int shipType){
        try{
       
            boolean isInSystem = players.containsKey(playerName)? true: false;
            
            if(isInSystem){
                PubPlayer playerBought = pubStoreSystem.buyItem(itemName, players.get(playerName), shipType);
                PubItem lastItem = playerBought.getLastItem();
                boolean arenaItem = lastItem.isArenaItem()? true:false;
                //System.out.println("Item q.."+lastItem.isArenaItem());
                
                m_botAction.sendPrivateMessage(playerName, playerBought.getLastItemDetail());
               
                if(arenaItem)
                    m_botAction.sendArenaMessage("Player "+playerName+" has purchased "+playerBought.getLastItemDetail(), 21); //put price on levi tostring
                
                int playerId = m_botAction.getPlayerID(playerName);
                playerBought = this.lvzPubPointsHandler.handleLvzMoney(playerBought, playerId, String.valueOf( playerBought.getPoint() ), false);
                
                players.put(playerName, playerBought);
                m_botAction.specificPrize(playerBought.getP_name(), lastItem.getItemNumber());
            } else
                m_botAction.sendPrivateMessage(playerName, "You're not in the system to use !buy.");
            
        }
        catch(NoSuchElementException e){
            e.printStackTrace();
        }
        catch(RuntimeException e){
            m_botAction.sendPrivateMessage(playerName, "Store is off today, please come back tomorrow!");
        }
        catch(Exception e){
            e.printStackTrace();
            //e.printStackTrace();
            //throw new RuntimeException("You've bought too many items and reached the limit. It'll be reseted after you die.");
        }
        
    }
    /**
     * Sets up the voting options, if voting is enabled in the CFG.
     */
    public void setupVotingOptions() {
        VoteOption v = new VoteOption( "", "", 0, 0 );   // 0th position; don't use to make it easy
        m_voteOptions.add( v );

        v = new VoteOption( "ti1",    "Start the timed game", 60, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "ti2",    "Stop the timed game", 60, 3 );
        m_voteOptions.add( v );

        // Allow the option to prevent players from voting on Levis
        Integer vo = m_botAction.getBotSettings().getInteger(m_botAction.getBotName() + "DisableLeviVoting");
        if( vo == null || vo == 0 ) {
            v = new VoteOption( "le1",    "Allow Leviathans in the arena", 55, 2 );
            m_voteOptions.add( v );
            v = new VoteOption( "le2",    "Disallow Leviathans in the arena", 55, 2 );
            m_voteOptions.add( v );
        }
        v = new VoteOption( "ja1",    "Set max # Javs allowed to 20% of the team size", 65, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "ja2",    "Unrestrict Javelins", 55, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "we1",    "Set max # Weasels allowed to 20% of the team size", 65, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "we2",    "Unrestrict Weasels", 55, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "pf1",    "Allow Private Frequencies in the arena", 55, 2 );
        m_voteOptions.add( v );
        v = new VoteOption( "pf2",    "Disallow Private Frequencies in the arena", 55, 2 );
        m_voteOptions.add( v );
        if( vo == null || vo == 0 ) {
            v = new VoteOption( "st1",    "Warp players to earwarps in timed game", 80, 3 );
            m_voteOptions.add( v );
            v = new VoteOption( "st2",    "Stop warping players to earwarps in timed game", 55, 3 );
            m_voteOptions.add( v );
        }
        v = new VoteOption( "w1",     "Allow players to use !warp in timed game", 55, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "w2",     "Disable players from using !warp in timed game", 70, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "aw1",    "Set !warp on players who enter arena", 65, 3 );
        m_voteOptions.add( v );
        v = new VoteOption( "aw2",    "Stop setting !warp on players who enter arena", 55, 3 );
        m_voteOptions.add( v );
    }

    public boolean isIdle() {
        if( flagTimer != null && flagTimer.isRunning() )
            return false;
        return true;
    }

    /**
     * Requests all of the appropriate events.
     */
    private void requestEvents()
    {
        EventRequester eventRequester = m_botAction.getEventRequester();
        eventRequester.request(EventRequester.MESSAGE);
        eventRequester.request(EventRequester.PLAYER_LEFT);
        eventRequester.request(EventRequester.PLAYER_ENTERED);
        eventRequester.request(EventRequester.FLAG_CLAIMED);
        eventRequester.request(EventRequester.FREQUENCY_CHANGE);
        eventRequester.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(EventRequester.LOGGED_ON);
        eventRequester.request(EventRequester.ARENA_LIST);
        eventRequester.request(EventRequester.ARENA_JOINED);
        eventRequester.request(EventRequester.KOTH_RESET);
        eventRequester.request(EventRequester.PLAYER_DEATH);
    }


    /* **********************************  EVENTS  ************************************ */

    /**
     * Retreives all necessary settings for the bot to operate.
     *
     * @param event is the event to process.
     */
    public void handleEvent(LoggedOn event)
    {
        BotSettings botSettings = m_botAction.getBotSettings();
        initialSpawn = botSettings.getString("InitialArena");
        initialPub = (botSettings.getInt(m_botAction.getBotName() + "Pub") - 1);
        m_botAction.joinArena(initialSpawn);
        shipWeights.add( new Integer(1) );		// Allow unlimited number of spec players
        for( int i = 1; i < 9; i++ )
            shipWeights.add( new Integer( botSettings.getInt(m_botAction.getBotName() + "Ship" + i) ) );
        m_botAction.setPlayerPositionUpdating(500);
        m_botAction.receiveAllPlayerDeaths();
    }


    /**
     * Requests arena list to move to appropriate pub automatically, if the arena
     * is the first arena joined.
     *
     * @param event is the event to process.
     */
    public void handleEvent(ArenaJoined event)
    {
    	if(!initLogin)
    		return;

    	initLogin = false;
    	m_botAction.requestArenaList();
    }


    /**
     * Sends bot to public arena specified in CFG.
     *
     * @param event is the event to process.
     */
    public void handleEvent(ArenaList event)
    {
    	String[] arenaNames = event.getArenaNames();

    	/** 
    	 * GammaBot5 will be used to beta-test the new pub system
    	 */
    	if (m_botAction.getBotName().equals("GammaBot5")) {
    		m_botAction.changeArena("PubTest");
    		return;
    	}
    	
        Comparator <String>a = new Comparator<String>()
        {
            public int compare(String a, String b)
            {
                if (Tools.isAllDigits(a) && !a.equals("") ) {
                    if (Tools.isAllDigits(b) && !b.equals("") ) {
                        if (Integer.parseInt(a) < Integer.parseInt(b)) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        return -1;
                    }
                } else if (Tools.isAllDigits(b)) {
                    return 1;
                } else {
                    return a.compareToIgnoreCase(b);
				}
            };
        };

        Arrays.sort(arenaNames, a);

    	String arenaToJoin = arenaNames[initialPub];// initialPub+1 if you spawn it in # arena
    	if(Tools.isAllDigits(arenaToJoin))
    	{
    		m_botAction.changeArena(arenaToJoin);
    		startBot();
    	}
    }


    /**
     * Handles the FrequencyShipChange event.
     * Checks players for appropriate ships/freqs.
     * Resets their MVP timer if they spec or change ships (new rule).
     *
     * @param event is the event to process.
     */
    public void handleEvent(FrequencyShipChange event)
    {
        int playerID = event.getPlayerID();
        int freq = event.getFrequency();
		int ship = event.getShipType();

        Player p = m_botAction.getPlayer( playerID );
		// ship = p.getShipType(); // for old times' sake
        if( p == null )
            return;
        
        if(ship == 5)
            m_botAction.sendOpposingTeamMessageByFrequency(freq, "Player "+p.getPlayerName()+" is now a terr; you may attach");
        
        try {
            if( flagTimeStarted && flagTimer != null && flagTimer.isRunning() ) {
                // Remove player if spec'ing
                if( ship == Tools.Ship.SPECTATOR ) {
                    String pname = p.getPlayerName();
                	playerTimes.remove( pname );
                // Reset player if shipchanging
                } else {
                    String pname = p.getPlayerName();
                    
                    /**It'll keep MVP by any esc+#ship change*/
                    boolean authd = true ;//authorizedChangePlayers.remove( pname );
                    
                    // If player is switching to the smaller team, they maintain any MVP status
                    /*m_botAction.sendPrivateMessage(pname,
                    "You'll keep any MVP status by any ESC+# change.");
                     */
                    if( teamsUneven && freq == freqSizeInfo[1] ) {
                        boolean freq0cont = freq0List.contains(pname);
                        boolean freq1cont = freq1List.contains(pname);
                        if( (freq0cont || freq1cont ) ) {   // Only for those who were on the other freq before!
                            if( (freq == 0 && !freq0cont) ||
                                (freq == 1 && !freq1cont)) {
                                authd = true;
                                m_botAction.sendPrivateMessage(pname, "For evening the teams, you keep any MVP status you had on your prior freq and earn " + NICEGUY_BOUNTY_AWARD + " bounty." );
                                m_botAction.giveBounty(pname, NICEGUY_BOUNTY_AWARD);
                            }
                        }
                    }

					// If player changes to a ship needed by the team, they maintain MVP status
					if	(ship == Tools.Ship.SPIDER || ship == Tools.Ship.TERRIER || ship == Tools.Ship.SHARK) {
						ArrayList<Vector<String>> team = getTeamData(freq);
						int numOfShipNeeded = 0;
						
						switch (ship) {
							case Tools.Ship.SPIDER:		numOfShipNeeded = SPIDER_QUOTA - team.get(Tools.Ship.SPIDER).size(); break;
							case Tools.Ship.TERRIER:	numOfShipNeeded = TERR_QUOTA - team.get(Tools.Ship.TERRIER).size();	break;
							case Tools.Ship.SHARK:		numOfShipNeeded = SHARK_QUOTA - team.get(Tools.Ship.SHARK).size(); break;
							default: break;
						}
						
						if (numOfShipNeeded > 0) {
							authd = true;
							m_botAction.giveBounty(pname, NICEGUY_BOUNTY_AWARD);
						}
					}
					
                    if( !authd ) {
                        playerTimes.remove( pname );
                        playerTimes.put( pname, new Integer( flagTimer.getTotalSecs() ) );
                    }
                    
                    if( autoWarp ) {
                        if( !freq0List.contains(pname) && !freq1List.contains(pname) && !warpPlayers.contains(pname) )
                            if( ship != Tools.Ship.SPECTATOR )
                                doWarpCmd(pname); 
                    }

                    // Terrs and Levis can't warp into base if Levis are enabled
                    if( shipWeights.get(Tools.Ship.LEVIATHAN) > 0 ) {                        
                        if( ship == Tools.Ship.LEVIATHAN || ship == Tools.Ship.TERRIER )            
                            warpPlayers.remove( pname );
                    }

                }
            }
        } catch (Exception e) {
        }
        
        boolean isAuth = this.allowedPlayersToUseItem.contains(p.getPlayerName()) ? true : false;
        //Adapt the buying system here
        if(started) {
            if(!isAuth) //he didn't buy the item
             checkPlayer(playerID);
            if(!privFreqs) {
                checkFreq(playerID, freq, true);
            }
        }
    }


    /**
     * Checks if freq is valid (if private frequencies are disabled), and prevents
     * freq-hoppers from switching freqs for end round prizes.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(FrequencyChange event)
    {
        int playerID = event.getPlayerID();
        int freq = event.getFrequency();

        Player p = m_botAction.getPlayer( playerID );
        if( p == null )
            return;

        try {
            if( flagTimeStarted && flagTimer != null && flagTimer.isRunning() ) {
                String pname = p.getPlayerName();
                boolean authd = authorizedChangePlayers.remove( pname );
                // If player is switching to the smaller team, they maintain any MVP status
                if( teamsUneven && freq == freqSizeInfo[1] ) {
                    boolean freq0cont = freq0List.contains(pname);
                    boolean freq1cont = freq1List.contains(pname);
                    if( (freq0cont || freq1cont ) ) {   // Only for those who were on the other freq before!
                        if( (freq == 0 && !freq0cont) ||
                            (freq == 1 && !freq1cont)) {
                            authd = true;
                            m_botAction.sendPrivateMessage(pname, "For evening the teams, you keep any MVP status you had on your prior freq and earn " + NICEGUY_BOUNTY_AWARD + " bounty." );
                            m_botAction.giveBounty(pname, NICEGUY_BOUNTY_AWARD);
                        }
                    }
                }
                if( !authd ) {
                    playerTimes.remove( pname );
                    playerTimes.put( pname, new Integer( flagTimer.getTotalSecs() ) );
                }
                if( autoWarp )
                    if( !freq0List.contains(pname) && !freq1List.contains(pname) && !warpPlayers.contains(pname) )
                        if( p.getShipType() != Tools.Ship.SPECTATOR )
                            doWarpCmd(pname);
                // Terrs and Levis can't warp into base if Levis are enabled
                if( shipWeights.get(Tools.Ship.LEVIATHAN) > 0 ) {                        
                    if( p.getShipType() == Tools.Ship.LEVIATHAN || p.getShipType() == Tools.Ship.TERRIER )            
                        warpPlayers.remove( pname );
                }
            }
        } catch (Exception e) {
        }

        if(started) {
            checkPlayer(playerID);
            if(!privFreqs) {
                checkFreq(playerID, freq, true);
                checkFreqSizes();
            }
        }
    }


    /**
     * When a player enters, displays necessary information, and checks
     * their ship & freq.
     *
     * @param event is the event to process.
     */
    public void handleEvent(PlayerEntered event)
    {
        try {
            int playerID = event.getPlayerID();
            Player player = m_botAction.getPlayer(playerID);
            String playerName = m_botAction.getPlayerName(playerID);

            if(started) {
                m_botAction.sendPrivateMessage(playerName, "Welcome to Pub.  Private Freqs: [" + (privFreqs ? "OK" : "NO") + "]" + "  Timed pub: [" + (flagTimeStarted ? "ON" : "OFF") + "]" );

                String restrictions = "";
                int weight;

                for( int i = 1; i < 9; i++ ) {
                    weight = shipWeights.get( i ).intValue();
                    if( weight == 0 )
                        restrictions += Tools.shipName( i ) + "s disabled.  ";
                    if( weight > 1 )
                        restrictions += Tools.shipName( i ) + "s limited.  ";
                }

                if( restrictions != "" )
                    m_botAction.sendPrivateMessage(playerName, "Ship restrictions: " + restrictions );

                checkPlayer(playerID);
                if(!privFreqs) {
                    checkFreq(playerID, player.getFrequency(), false);
                    checkFreqSizes();
                }
                String cmds = "";
                if( warpAllowed )
                    cmds += "!warp ";
                cmds += "!terr !team !clearmines";
                if( m_votingEnabled )
                    cmds += " !listvotes !startvote";
                if( m_challengeEnabled )
                    cmds += " !challenge !end";
                m_botAction.sendPrivateMessage(playerName, "Commands:  " + cmds );
                
                
            }
            if(flagTimeStarted) {
                if( flagTimer != null)
                    m_botAction.sendPrivateMessage(playerName, flagTimer.getTimeInfo() );
                if( autoWarp )      // Autowarp is "opt out" warping rather than "opt in"
                    if( player.getShipType() != Tools.Ship.SPECTATOR )
                        doWarpCmd(playerName);
              //Point system
                if( !this.players.containsKey(playerName) ){
                    players.put( playerName, new PubPlayer(playerName) );
                    //Tools.printLog("Added "+playerName);    
                }
            }
        } catch (Exception e) {
        }

    }


    /**
     * Removes a player from all tracking lists when they leave the arena.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(PlayerLeft event)
    {
        int playerID = event.getPlayerID();
        String playerName = m_botAction.getPlayerName(playerID);

        removeFromLists(playerName);
        removeFromWarpList(playerName);
        playerTimes.remove( playerName );
        m_votes.remove(playerName);
        checkFreqSizes();

        for( PubChallenge pc : m_challenges ) {
            if( pc.getPlayer1().getPlayerID() == event.getPlayerID() ||
                pc.getPlayer2().getPlayerID() == event.getPlayerID() ) {
                pc.endChallenge(pc.getPlayer1());
            }
        }

    }

    /**
     * Handles restarting of the KOTH game
     *
     * @param event is the event to handle.
     */
    public void handleEvent(KotHReset event) {
        if(event.isEnabled() && event.getPlayerID()==-1) {
            // Make the bot ignore the KOTH game (send that he's out immediately after restarting the game)
            m_botAction.endKOTH();
        }
    }

    /**
     * If flag time mode is running, register with the flag time game that the
     * flag has been claimed.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(FlagClaimed event) {
        if(!flagTimeStarted)
            return;

        int playerID = event.getPlayerID();
        Player p = m_botAction.getPlayer(playerID);

        try {
            if( p != null && flagTimer != null ) {
                flagTimer.flagClaimed( p.getFrequency(), playerID );
            }
        } catch (Exception e) {
        }
    }

    /**
     * Handles deaths in player challenges.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(PlayerDeath event) {
        Player killer = m_botAction.getPlayer( event.getKillerID() );
        Player killed = m_botAction.getPlayer( event.getKilleeID() );
        if( killer == null || killed == null )
            return;
        boolean challengeFound = false;     // Players can only be in one at once
        
        //buying system of ships
        //update(killed.getPlayerName());
        boolean wasAuth = this.allowedPlayersToUseItem.contains(killed.getPlayerName())? true : false;
        if(wasAuth){
            this.allowedPlayersToUseItem.remove(killed.getPlayerName());
            m_botAction.specWithoutLock(killed.getPlayerName());
            m_botAction.sendPrivateMessage(killed.getPlayerName(), "You just died. Now, buy an other ship please or get in any allowed ship.");
        }
        //--
        
        /*reset limit bought items per life
        boolean isIn = players.containsKey(killed.getPlayerName())? true:false;
        if(isIn){
            PubPlayer pubPlayer = players.get(killed.getPlayerName());
            pubPlayer.setItemsBoughtPerLife(0);
            players.put(pubPlayer.getP_name(), pubPlayer);
        }
         */
        
        /** Point System */
        try{
            int points = 1;
            
            /**
             * starts decorating the point
            */
            //decorates by ship number point
           // System.out.println("SHIP TYPE: "+killer.getShipType());
            
            points+=shipPoints.get((int)killer.getShipType());
            
            Point pointXY = new Point(killer.getXTileLocation(), killer.getYTileLocation());
            //System.out.println("X,Y: "+pointXY.x+", "+pointXY.y);
            int location;
            String loc;
            
            loc = pubStoreSystem.getLocation(pointXY);//chain of responsibility
            
            if(loc == null)
                return;
            
            location = areaPoints.get(loc);
            /*if(flagRoomLocation.isInside(pointXY)){
                location = areaPoints.get("flagroom");
                //System.out.println(killer.getPlayerName()+" killed flagroom");
            }
            else if(midBaseLocation.isInside(pointXY)){
                location = areaPoints.get("mid");
                //System.out.println(killer.getPlayerName()+" killed mid");
            }
            else{
                location = areaPoints.get("spawn");
                //System.out.println(killer.getPlayerName()+" killed spawn");
            }
            */
            points+=(int)location;
            
            //update on the map the player
            PubPlayer pubPlayer;
            String playerName = killer.getPlayerName();
            if(players.containsKey(playerName))
                pubPlayer = players.get(playerName);
            else{
                pubPlayer = new PubPlayer(playerName);
                resetObjons(pubPlayer.getP_name());
                
                /*for(int i = 0; i < 7; i++){
                    for(int j = 0; j < 7; j++){
                     m_botAction.sendUnfilteredPrivateMessage(pubPlayer.getP_name(), "*objoff "+502+i+j);
                     System.out.println("Objoff: "+502+i+j);
                    }
                }extracted method to resetObjons ( reafactoring )
                */
                int i[] = {0,0,0,0,0,0};
                pubPlayer.setObjon(i);
            }
            pubPlayer.setPoint(pubPlayer.getPoint()+points);
            
            pubPlayer = lvzPubPointsHandler.handleLvzMoney(pubPlayer, killer.getPlayerID(), String.valueOf(pubPlayer.getPoint()), true);
            players.put(playerName, pubPlayer);

            Tools.printLog("Added "+points+" to "+playerName+" TOTAL POINTS: "+pubPlayer.getPoint());
           
            //--
        } catch(RuntimeException e){
          //system is on off state, won't calculate anything and returns null   
        } catch(Exception e){
            Tools.printLog("Exception: "+e.getMessage());
            e.printStackTrace();
        }
        
        for( PubChallenge pc : m_challenges ) {
            if( pc.challengeActive() ) {
                if(        killer.getPlayerID() == pc.getPlayer1().getPlayerID() ) {
                    if(    killed.getPlayerID() == pc.getPlayer2().getPlayerID() ) {
                        // P1 killed P2
                        pc.reportKill(1);
                        challengeFound = true;
                    }
                } else if( killer.getPlayerID() == pc.getPlayer2().getPlayerID() ) {
                    if(    killed.getPlayerID() == pc.getPlayer1().getPlayerID() ) {
                        // P2 killed P1
                        pc.reportKill(2);
                        challengeFound = true;
                    }
                }
            }
            if( challengeFound )
                return;
        }
    }

    private void resetObjons(String playerName){
        for(int i = 0; i < 7; i++){
            for(int j = 0; j < 7; j++){
             m_botAction.sendUnfilteredPrivateMessage(playerName, "*objoff "+502+i+j);
             //System.out.println("Objoff: "+502+i+j);
            }
        }
    }
    /**
     * Handles all messages received.
     *
     * @param event is the message event to handle.
     */
    public void handleEvent(Message event) {
        String sender = getSender(event);
        int messageType = event.getMessageType();
        String message = event.getMessage().trim();

        if( message == null || sender == null )
            return;

        if( !message.startsWith("!") ) {
        	if( m_currentVoteItem == -1 ) {
        		return;
        	} else {
        		if( message.equals("1") || message.equals("2") )
        			handleVote( sender, Integer.parseInt(message) );
        	}
        }

        message = message.toLowerCase();
        if((messageType == Message.PRIVATE_MESSAGE || messageType == Message.PUBLIC_MESSAGE ) )
            handlePublicCommand(sender, message);
        if ( opList.isHighmod(sender) || sender.equals(m_botAction.getBotName()) )
            if((messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE) )
                handleModCommand(sender, message);
    }



    /* **********************************  COMMANDS  ************************************ */

    /**
     * Handles public commands sent to the bot, either in PM or pub chat.
     *
     * @param sender is the person issuing the command.
     * @param command is the command that is being sent.
     */
    public void handlePublicCommand(String sender, String command) {
        try {
            if(command.equals("!time"))
                doTimeCmd(sender);
            else if(command.equals("!help"))
                doHelpCmd(sender);
            else if(command.startsWith("!whereis "))
                doWhereIsCmd(sender, command.substring(9), opList.isBot(sender));
            else if(command.startsWith("!w"))
                doWarpCmd(sender);
            else if(command.equals("!restrictions"))
                doRestrictionsCmd(sender);
            else if(command.startsWith("!tea"))
                doShowTeamCmd(sender);
            else if(command.startsWith("!t"))
                doTerrCmd(sender);
            //else if(command.startsWith("!ship "))
              // doShipCmd(sender, command.substring(6));
            else if(command.startsWith("!challenge "))
                doChallengeCmd(sender, command.substring(11));
            else if(command.startsWith("!end"))
                doEndCmd(sender);
            else if(command.startsWith("!cl"))
                doClearMinesCmd(sender);
            else if(command.startsWith("!startvote "))
                doStartVoteCmd(sender, command.substring(11));
            else if(command.equals("!listvotes"))
                doListVotesCmd(sender);
            else if(command.equals("!loc"))
            {
                for(Integer i: this.areaPoints.values())
                    Tools.printLog("Area Values: "+i);
                
                for(int i = 1; i < 9; i++)
                    Tools.printLog("Ship Values "+shipPoints.get(i) );
            }
            else if(command.equals("!$"))
            {
                doCmdDisplayMoney(sender);
                
            }
            else if(command.startsWith("!b ")){
                try{
                    doCmdBuy(sender, command);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else if(command.startsWith("!buy ")){
                try{
                    doCmdBuy2(sender, command);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            /*else if(command.equals("!buy"))
                doDisplayItems(sender);
            */
            else if(command.equals("!about"))
                doDisplayExplanation(sender);
            
        } catch(RuntimeException e) {
            if( e != null && e.getMessage() != null )
                m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }


    /**
     * Handles mod-only commands sent to the bot.
     *
     * @param sender is the person issuing the command.
     * @param command is the command that is being sent.
     */
    public void handleModCommand(String sender, String command) {
        try {
            if(command.startsWith("!go "))
                doGoCmd(sender, command.substring(4));
            //else if(command.equals("!start"))
            //    doStartCmd(sender);
            //else if(command.equals("!stop"))
            //    doStopCmd(sender);
            else if(command.equals("!privfreqs"))
                doPrivFreqsCmd(sender);
            else if(command.startsWith("!starttime "))
                doStartTimeCmd(sender, command.substring(11));
            else if(command.equals("!stricttime"))
                doStrictTimeCmd(sender);
            else if(command.equals("!stoptime"))
                doStopTimeCmd(sender);
            else if(command.startsWith("!set "))
                doSetCmd(sender, command.substring(5));
            else if(command.equals("!autowarp"))
                doAutowarpCmd(sender);
            else if(command.equals("!allowwarp"))
                doAllowWarpCmd(sender);
            else if(command.equals("!die"))
                doDieCmd(sender);
            else if(command.equals("!storeon"))
                enableStore(sender);
            else if(command.equals("!storeoff"))
                disableStore(sender);
            
        } catch(RuntimeException e) {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }


    /**
     * Handles a vote, if vote is being counted.  Does not PM player unless the vote
     * is being changed.
     * @param sender
     * @param vote Number player is voting for.  1=yes, 2=no.
     */
    public void handleVote( String sender, Integer vote ) {
    	if( m_votes.containsKey(sender) ) {
    		Integer lastVote = m_votes.get(sender);
    		if( lastVote != vote ) {
    			m_botAction.sendPrivateMessage( sender, "Vote changed to " + (vote==1?"YES.":"NO.") );
    		} else {
                m_botAction.sendPrivateMessage( sender, "Vote already counted." );		    
    		}
    	}
    	m_votes.put(sender, vote);
    }
    
    private void doDisplayExplanation(String sender){
        String[] explanation = {
                
                "Hi, I'm a new store that sells lots of items to your ship",
                "Depending on your kill, you get money added into your cash",
                "This money, depends on the location you are(flagroom, mid, spawn) and what ship you were using",
                "So, you may get rich and buy items that are being sold by me. by Dex"
                
        };
        m_botAction.smartPrivateMessageSpam(sender, explanation);
    }
    
    private void enableStore(String sender){
        this.pubStoreSystem = new PubPointStoreOn(this);
        m_botAction.sendPrivateMessage(sender, "Welcome, thanks for logging me on! Now I'm selling item. Want to buy?");
    }
    
    private void disableStore(String sender){
        this.pubStoreSystem = new PubPointStoreOff();
        m_botAction.sendPrivateMessage(sender, "It was a good day of work, but now the store is ...shutting down! Off.");
    }
    
    private void doDisplayItems(String sender){
        try{
            List list = pubStoreSystem.displayAvailableItems();
            m_botAction.smartPrivateMessageSpam(sender, (String[]) list.toArray(new String[list.size()]));
        }catch(Exception e){
            m_botAction.sendPrivateMessage(sender, "?");
        }
    }
    private void doCmdBuy(String sender, String command){
        Player p = m_botAction.getPlayer(sender);
        String itemName;
        if(p == null)
            return;
        //!b <item>
        //0123
        itemName = command.substring(3);
        buyItem(sender, itemName, p.getShipType());
    }
    
    private void doCmdBuy2(String sender, String command){
        Player p = m_botAction.getPlayer(sender);
        String itemName;
        if(p == null)
            return;
        //!b 
        //0123
        //!buy 
        //012345
        itemName = command.substring(5);
        buyItem(sender, itemName, p.getShipType());
    }
    private void doCmdDisplayMoney(String sender){
        if(players.containsKey(sender)){
            PubPlayer pubPlayer = this.players.get(sender);
            m_botAction.sendPrivateMessage(sender, "You have $"+pubPlayer.getPoint());
        }else
            m_botAction.sendPrivateMessage(sender, "You're still not in the point system. Wait a bit to be added");
    }

    /**
     * Moves the bot from one arena to another.  The bot must not be
     * started for it to move.
     *
     * @param sender is the person issuing the command.
     * @param argString is the new arena to go to.
     * @throws RuntimeException if the bot is currently running.
     * @throws IllegalArgumentException if the bot is already in that arena.
     */
    public void doGoCmd(String sender, String argString)
    {
        String currentArena = m_botAction.getArenaName();

        if(started || flagTimeStarted)
            throw new RuntimeException("Bot is currently running pub settings in " + currentArena + ".  Please !Stop and/or !Endtime before trying to move.");
        if(currentArena.equalsIgnoreCase(argString))
            throw new IllegalArgumentException("Bot is already in that arena.");

        m_botAction.changeArena(argString);
        m_botAction.sendSmartPrivateMessage(sender, "Bot going to: " + argString);
    }


    /**
     * Starts the pure pub settings.
     *
     * @param sender is the person issuing the command.
     * @throws RuntimeException if the bot is already running pure pub settings.
     */
    /* With !set command, !start and !stop are obsolete.
    public void doStartCmd(String sender)
    {
        if(started)
            throw new RuntimeException("Bot is already running pure pub settings.");

        started = true;
        specRestrictedShips();
        m_botAction.sendArenaMessage("Pure pub settings enabled.  Ship restrictions are now in effect.", 2);
        m_botAction.sendSmartPrivateMessage(sender, "Pure pub succesfully enabled.");
    }
    */


    /**
     * Stops the pure pub settings.
     *
     * @param sender is the person issuing the command.
     * @throws RuntimeException if the bot is not currently running pure pub
     * settings.
     */
    /* With !set command, !start and !stop are obsolete.
    public void doStopCmd(String sender)
    {
        if(!started)
            throw new RuntimeException("Bot is not currently running pure pub settings.");

        started = false;
        m_botAction.sendArenaMessage("Pure pub settings disabled.  Ship restrictions are no longer in effect.", 2);
        m_botAction.sendSmartPrivateMessage(sender, "Pure pub succesfully disabled.");
    }
    */


    /**
     * Toggles if private frequencies are allowed or not.
     *
     * @param sender is the sender of the command.
     */
    public void doPrivFreqsCmd(String sender)
    {
        if(!privFreqs)
        {
            m_botAction.sendArenaMessage("Private Frequencies enabled.", 2);
            m_botAction.sendSmartPrivateMessage(sender, "Private frequencies succesfully enabled.");
        }
        else
        {
            fixFreqs();
            m_botAction.sendArenaMessage("Private Frequencies disabled.", 2);
            m_botAction.sendSmartPrivateMessage(sender, "Private frequencies succesfully disabled.");
        }
        privFreqs = !privFreqs;
    }


    /**
     * Starts a "flag time" mode in which a team must hold the flag for a certain
     * consecutive number of minutes in order to win the round.
     *
     * @param sender is the person issuing the command.
     * @param argString is the number of minutes to hold the game to.
     */
    public void doStartTimeCmd(String sender, String argString )
    {
        if(flagTimeStarted)
            throw new RuntimeException( "Flag Time mode has already been started." );

        int min = 0;

        try {
            min = (Integer.valueOf( argString )).intValue();
        } catch (Exception e) {
            throw new RuntimeException( "Bad input.  Please supply a number." );
        }

        if( min < 1 || min > 120 )
            throw new RuntimeException( "The number of minutes required must be between 1 and 120." );

        flagMinutesRequired = min;

        m_botAction.sendArenaMessage( "Flag Time mode has been enabled." );

        m_botAction.sendArenaMessage( "Object: Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win a round.  Best " + ( MAX_FLAGTIME_ROUNDS + 1) / 2 + " of "+ MAX_FLAGTIME_ROUNDS + " wins the game." );
        if( strictFlagTime )
            m_botAction.sendArenaMessage( "Round 1 begins in 60 seconds.  All players will be warped at round start." );
        else
            if( autoWarp )
                m_botAction.sendArenaMessage( "Round 1 begins in 60 seconds.  You will be warped into flagroom at round start (type !warp to change). -" + m_botAction.getBotName() );
            else
                m_botAction.sendArenaMessage( "Round 1 begins in 60 seconds.  PM me with !warp to warp into flagroom at round start. -" + m_botAction.getBotName() );

        flagTimeStarted = true;
        freq0Score = 0;
        freq1Score = 0;

        m_botAction.scheduleTask( new StartRoundTask(), 60000 );
    }


    /**
     * Toggles "strict" flag time mode in which all players are first warped
     * automatically into safe (must be set), and then warped into base.
     *
     * @param sender is the person issuing the command.
     */
    public void doStrictTimeCmd(String sender ) {
        if( strictFlagTime ) {
            strictFlagTime = false;
            if( flagTimeStarted )
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode disabled.  Changes will go into effect next round.");
            else
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode disabled.  !startflagtime <minutes> to begin a normal flag time game.");
        } else {
            strictFlagTime = true;
            if(flagTimeStarted) {
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode enabled.  All players will be warped into base next round.");
            } else {
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode enabled.  !startflagtime <minutes> to begin a strict flag time game.");
            }
        }
    }


    /**
     * Ends "flag time" mode.
     *
     * @param sender is the person issuing the command.
     */
    public void doStopTimeCmd(String sender )
    {
        if(!flagTimeStarted)
            throw new RuntimeException( "Flag Time mode is not currently running." );

        m_botAction.sendSmartPrivateMessage( sender, "Flag Time mode disabled." );
        m_botAction.sendArenaMessage( "Flag Time mode has been disabled." );

        try {
            flagTimer.endGame();
            m_botAction.cancelTask(flagTimer);
            m_botAction.cancelTask(intermissionTimer);
            m_botAction.cancelTask(startTimer);
        } catch (Exception e ) {
        }

        flagTimeStarted = false;
        strictFlagTime = false;
    }


    /**
     * Displays info about time remaining in flag time round, if applicable.
     *
     * @param sender is the person issuing the command.
     */
    public void doTimeCmd( String sender )
    {
        if( flagTimeStarted )
            if( flagTimer != null )
                flagTimer.sendTimeRemaining( sender );
            else
                throw new RuntimeException( "Flag time mode is just about to start." );
        else
            throw new RuntimeException( "Flag time mode is not currently running." );
    }


    /**
     * Adds player to next round's warp list.
     *
     * @param sender is the person issuing the command.
     */
    public void doWarpCmd( String sender )
    {
        if(!flagTimeStarted)
            throw new RuntimeException( "Flag Time mode is not currently running." );
        if( strictFlagTime )
            throw new RuntimeException( "You do not need to !warp in Strict Flag Time mode.  You will automatically be warped." );
        if( !warpAllowed )
            throw new RuntimeException( "Warping into base at round start is not currently allowed." );
        
        // Terrs and Levis can't warp into base if Levis are enabled
        if( shipWeights.get(Tools.Ship.LEVIATHAN) > 0 ) {
            Player p = m_botAction.getPlayer( sender );
            if( p.getShipType() == Tools.Ship.LEVIATHAN )            
                throw new RuntimeException( "Leviathans can not warp in to base at round start." );
            if( p.getShipType() == Tools.Ship.TERRIER )
                throw new RuntimeException( "Terriers can not warp into base at round start while Leviathans are enabled." );                
        }

        if( warpPlayers.contains( sender ) ) {
            warpPlayers.remove( sender );
            m_botAction.sendSmartPrivateMessage( sender, "You will NOT be warped inside FR at every round start.  !warp again to turn back on." );
        } else {
            warpPlayers.add( sender );
            //m_botAction.sendSmartPrivateMessage( sender, "You will be warped inside FR at every round start.  Type !warp to turn off." );
        }
    }


    /**
     * Logs the bot off if not enabled.
     *
     * @param sender is the person issuing the command.
     * @throws RuntimeException if the bot is running pure pub settings.
     */
    public void doDieCmd(String sender)
    {
        m_botAction.sendSmartPrivateMessage(sender, "Bot logging off.");
        objs.hideAllObjects();
        m_botAction.setObjects();
        m_botAction.scheduleTask(new DieTask(), 300);
    }


    /**
     * Lists any ship restrictions in effect.
     *
     * @param sender is the person issuing the command.
     */
    public void doRestrictionsCmd(String sender) {
        int weight;
        m_botAction.sendSmartPrivateMessage(sender, "Ship limitations/restrictions (if any)" );
        for( int i = 1; i < 9; i++ ) {
            weight = shipWeights.get( i ).intValue();
            if( weight == 0 )
                m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( i ) + "s disabled." );
            else if( weight > 1 )
                m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( i ) + "s limited to 1/" + weight + " of the size of a frequency (but 1 always allowed).");
        }
        m_botAction.sendSmartPrivateMessage(sender, "Private frequencies are " + (privFreqs ? "enabled." : "disabled.") );
    }


    /**
     * Sets a given ship to a particular restriction.
     *
     * @param sender is the person issuing the command.
     */
    public void doSetCmd(String sender, String argString) {
        String[] args = argString.split(" ");
        if( args.length != 2 )
            throw new RuntimeException("Usage: !set <ship#> <weight#>");

        try {
            Integer ship = Integer.valueOf(args[0]);
            ship = ship.intValue();
            Integer weight = Integer.valueOf(args[1]);
            if( ship > 0 && ship < 9 ) {
                if( weight >= 0 ) {
                    shipWeights.set( ship.intValue(), weight );
                    if( weight == 0 )
                        m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( ship ) + "s: disabled." );
                    if( weight == 1 )
                        m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( ship ) + "s: unrestricted." );
                    if( weight > 1 )
                        m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( ship ) + "s: limited to 1/" + weight + " of the size of a frequency (but 1 always allowed).");
                    specRestrictedShips();
                } else
                    throw new RuntimeException("Weight must be >= 0.");
            } else
                throw new RuntimeException("Invalid ship number.");
        } catch (Exception e) {
            throw new RuntimeException("Usage: !set <ship#> <weight#>");
        }
    }

    /**
     * Turns on or off "autowarp" mode, where players opt out of warping into base,
     * rather than opting in.
     *
     * @param sender is the person issuing the command.
     */
    public void doAutowarpCmd(String sender) {
        if( autoWarp ) {
            m_botAction.sendPrivateMessage(sender, "Players will no longer automatically be added to the !warp list when they enter the arena.");
            autoWarp = false;
        } else {
            m_botAction.sendPrivateMessage(sender, "Players will be automatically added to the !warp list when they enter the arena.");
            autoWarp = true;
        }
    }

    /**
     * Turns on or off allowing players to use !warp to get into base at the start of a round.
     *
     * @param sender is the person issuing the command.
     */
    public void doAllowWarpCmd(String sender) {
        if( warpAllowed ) {
            m_botAction.sendPrivateMessage(sender, "Players will no longer be able to use !warp.");
            warpAllowed = false;
            warpPlayers.clear();
        } else {
            m_botAction.sendPrivateMessage(sender, "Players will be allowed to use !warp.");
            warpAllowed = true;
        }
    }

    /**
     * Shows who on the team is in which ship.
     *
     * @param sender is the person issuing the command.
     */
    public void doShowTeamCmd(String sender) {
        Player p = m_botAction.getPlayer(sender);
        if( p == null )
            throw new RuntimeException("Can't find you.  Please report this to staff.");
        if( p.getShipType() == 0 )
            throw new RuntimeException("You must be in a ship for this command to work.");
        ArrayList<Vector<String>>  team = getTeamData( p.getFrequency() );
        int players = 0;
        for(int i = 1; i < 9; i++ ) {
            int num = team.get(i).size();
            String text = num + Tools.formatString( (" " + Tools.shipNameSlang(i) + (num==1 ? "":"s")), 8 );

            if(         i == Tools.Ship.SPIDER && num < SPIDER_QUOTA ||
                        i == Tools.Ship.TERRIER && num < TERR_QUOTA ||
                        i == Tools.Ship.SHARK && num < SHARK_QUOTA )
                text += "+  ";
            else if(    i == Tools.Ship.WARBIRD && num >= WB_TOOMANY ||
                        i == Tools.Ship.JAVELIN && num >= JAV_TOOMANY ||
                        i == Tools.Ship.WEASEL && num >= WEASEL_TOOMANY ||
                        i == Tools.Ship.TERRIER && num >= TERR_TOOMANY )
                text += "-  ";
            else
                text += "   ";
            for( int j = 0; j < team.get(i).size(); j++) {
               text += (j+1) + ") " + team.get(i).get(j) + "  ";
               players++;
            }
            m_botAction.sendPrivateMessage(sender, text);
        }

        // Begin team analysis
        m_botAction.sendPrivateMessage(sender, "Total " + players + " players.  Team needs (+ above shows a need; - shows excess):");
        int terrsNeeded = TERR_QUOTA - team.get(Tools.Ship.TERRIER).size();
        int sharksNeeded = SHARK_QUOTA - team.get(Tools.Ship.SHARK).size();
        int spidersNeeded = SPIDER_QUOTA - team.get(Tools.Ship.SPIDER).size();
        boolean needs = false;

        // If team is small, only need to ensure we have at least 1 terr and 1 shark
        if( players < 10 ) {
            if( terrsNeeded != TERR_QUOTA )
                terrsNeeded = 0;
            if( sharksNeeded != SHARK_QUOTA )
                sharksNeeded = 0;
            spidersNeeded = 0;
        }

        if( terrsNeeded == TERR_QUOTA ) {
            m_botAction.sendPrivateMessage(sender, "NO TERRIER!  A terr (ship 5) is needed ASAP.");
            needs = true;
        } else if( terrsNeeded > 0 ) {
            m_botAction.sendPrivateMessage(sender, terrsNeeded + " terrier" + (terrsNeeded>1 ? "s":"") + " needed.");
            needs = true;
        }
        if( sharksNeeded == SHARK_QUOTA ) {
            m_botAction.sendPrivateMessage(sender, "NO SHARK.  A shark is needed ASAP.");
            needs = true;
        } else if( sharksNeeded > 0 ) {
            m_botAction.sendPrivateMessage(sender, sharksNeeded + " shark" + (sharksNeeded>1 ? "s":"") + " needed.");
            needs = true;
        }
        if( spidersNeeded == SPIDER_QUOTA ) {
            m_botAction.sendPrivateMessage(sender, "NO SPIDERS.  A spider is needed ASAP.");
            needs = true;
        } else if( spidersNeeded > 0 ) {
            m_botAction.sendPrivateMessage(sender, spidersNeeded + " spider" + (spidersNeeded>1 ? "s":"") + " needed.");
            needs = true;
        }
        String tooMany = "";
        if( team.get(Tools.Ship.WARBIRD).size() >= WB_TOOMANY ) {
            tooMany += "WBs  ";
            needs = true;
        }
        if( team.get(Tools.Ship.JAVELIN).size() >= JAV_TOOMANY ) {
            tooMany += "Javs  ";
            needs = true;
        }
        if( team.get(Tools.Ship.WEASEL).size() >= WEASEL_TOOMANY ) {
            tooMany += "Weasels  ";
            needs = true;
        }
        if( team.get(Tools.Ship.TERRIER).size() >= TERR_TOOMANY ) {
            tooMany += "Terrs  ";
            needs = true;
        }
        if( tooMany != "" ) {
            m_botAction.sendPrivateMessage(sender, "Team has too many of the following:   " + tooMany );
        } else if( !needs ) {
            m_botAction.sendPrivateMessage(sender, "Your team appears to be well-balanced!");
            return;
        }
        m_botAction.sendPrivateMessage(sender, "->  Use !ship <ship#> to change ships & keep MVP.  <-");
    }


    /**
     * Places the player in a particular ship, if the ship is needed on the freq and
     * the player is not already in a
     * @param sender Player sending
     * @param argString Ship to change to
     */
    
    /*
    public void doShipCmd(String sender, String argString ) {
		throw new RuntimeException("!ship <ship#> is deprecated, you can change ship normally without losing any MVP status.");

		/*
        String[] args = argString.split(" ");
        if( args.length != 1 )
            throw new RuntimeException("Usage: !ship <ship#>, where <ship#> is the number of the ship to change to.");

        int ship = 0;
        try {
            ship = Integer.valueOf(args[0]);
        } catch (Exception e) {
            throw new RuntimeException("Usage: !ship <ship#>, where <ship#> is the number of the ship to change to.");
        }

        Player p = m_botAction.getPlayer(sender);
        if( p == null )
            throw new RuntimeException("Can't find you.  Please report this to staff.");
        if( p.getShipType() == 0 )
            throw new RuntimeException("You must be in a ship for this command to work.");

        // If the flag timer isn't currently active, just change them over
        if( flagTimer != null )
            if( !flagTimer.isRunning() )
                m_botAction.setShip(p.getPlayerID(), ship);

        if( ship != Tools.Ship.TERRIER && ship != Tools.Ship.SHARK && ship != Tools.Ship.SPIDER )
            throw new RuntimeException("This command only works for changing to a ship necessary for the team (Spider:3, Terr:5, or Shark:8).  Use !team to see which ships are needed.");
        if( ship == p.getShipType() )
            throw new RuntimeException("You're already in that ship.");

        ArrayList<Vector<String>> team = getTeamData( p.getFrequency() );
        int numOfShipNeeded;
        switch (p.getShipType()) {
        case 5:
            numOfShipNeeded = TERR_QUOTA - team.get(Tools.Ship.TERRIER).size();
            if( numOfShipNeeded >= 0 )
                throw new RuntimeException("Sorry, you're still needed by the team as a terr.  Ask someone else on your team to switch.");
            break;
        case 8:
            // Only restrict shark to spider movement in this instance
            numOfShipNeeded = SHARK_QUOTA - team.get(Tools.Ship.SHARK).size();
            if( numOfShipNeeded >= 0 && ship == Tools.Ship.SPIDER )
                throw new RuntimeException("Sorry, you're still needed by the team as a shark.  Ask someone else on your team to switch.");
            break;
        default:
            // If you're a spider, you can still change to terr or shark
            break;
        }

        switch (ship) {
        case 3: numOfShipNeeded = SPIDER_QUOTA - team.get(Tools.Ship.SPIDER).size(); break;
        case 5: numOfShipNeeded = TERR_QUOTA - team.get(Tools.Ship.TERRIER).size(); break;
        default: numOfShipNeeded = SHARK_QUOTA - team.get(Tools.Ship.SHARK).size();
        }
        if( numOfShipNeeded <= 0 )
            throw new RuntimeException("More "+ Tools.shipName(ship) + "s are not currently needed.  Use !team to see which ships are needed.");

        authorizedChangePlayers.add( p.getPlayerName() );
        int bounty = p.getBounty();
        m_botAction.setShip( p.getPlayerID(), ship );
        m_botAction.giveBounty( p.getPlayerID(), bounty + NICEGUY_BOUNTY_AWARD - 3 );  // -3 to compensate for new ship bty
        m_botAction.sendPrivateMessage( p.getPlayerID(), "For changing to a ship needed by your team, you keep any MVP status and gain 25 bounty in addition to your old bounty of " + bounty + "." );
		*/
    
    

    /**
     * Clears all of player's mines, and restores any MVP status, but only once per round.
     * @param sender Sender of command
     */
    public void doClearMinesCmd(String sender ) {
        Player p = m_botAction.getPlayer(sender);
        if( p == null )
            throw new RuntimeException("Can't find you.  Please report this to staff.");
        if( mineClearedPlayers.contains(p.getPlayerName()))
            throw new RuntimeException("You've already cleared your mines once, and can't do it again until next round (except, of course, manually).");
        if( p.getShipType() != Tools.Ship.SHARK && p.getShipType() != Tools.Ship.LEVIATHAN )
            throw new RuntimeException("You must be in a mine-laying ship in order for me to clear your mines.");
        boolean easyClear = false;
        if( flagTimer == null || !flagTimer.isRunning() )
            easyClear = true;

        int bounty = p.getBounty();
        int ship = p.getShipType();
        if( !easyClear)
            authorizedChangePlayers.add( p.getPlayerName() );
        
        m_botAction.setShip( sender, 1 );
        
        if( !easyClear)
            authorizedChangePlayers.add( p.getPlayerName() );
        
        m_botAction.setShip( sender, ship );
        m_botAction.giveBounty( sender, bounty - 3 );
        if( !easyClear) {
            mineClearedPlayers.add(p.getPlayerName());
            m_botAction.sendPrivateMessage( sender, "Your mines have been reset without changing MVP status.  You may only do this once per round." );
        }
    }


    /**
     * Shows terriers on the team and their last observed locations.
     */
    public void doTerrCmd( String sender ) {
        Player p = m_botAction.getPlayer(sender);
        if( p == null )
            throw new RuntimeException("Can't find you.  Please report this to staff.");
        if( p.getShipType() == 0 )
            throw new RuntimeException("You must be in a ship for this command to work.");
        Iterator<Player> i = m_botAction.getFreqPlayerIterator(p.getFrequency());
        if( !i.hasNext() )
            throw new RuntimeException("ERROR: No players detected on your frequency!");
        m_botAction.sendPrivateMessage(sender, "Name of Terrier          Last seen");
        while( i.hasNext() ) {
            Player terr = (Player)i.next();
            if( terr.getShipType() == Tools.Ship.TERRIER )
                m_botAction.sendPrivateMessage( sender, Tools.formatString(terr.getPlayerName(), 25) + getPlayerLocation(terr, false) );
        }
    }


    /**
     * Shows last seen location of a given individual.
     */
    public void doWhereIsCmd( String sender, String argString, boolean isStaff ) {
        Player p = m_botAction.getPlayer(sender);
        if( p == null )
            throw new RuntimeException("Can't find you.  Please report this to staff.");
        if( p.getShipType() == 0 && !isStaff )
            throw new RuntimeException("You must be in a ship for this command to work.");
        Player p2;
        p2 = m_botAction.getPlayer( argString );
        if( p2 == null )
            p2 = m_botAction.getFuzzyPlayer( argString );
        if( p2 == null )
            throw new RuntimeException("I can't find the player '" + argString + "'.  Tough shit, bucko.");
        if( p.getFrequency() != p2.getFrequency() && !isStaff )
            throw new RuntimeException(p2.getPlayerName() + " is not on your team!");
        m_botAction.sendPrivateMessage( sender, p2.getPlayerName() + " last seen: " + getPlayerLocation( p2, isStaff ));
    }


    /**
     * Based on provided coords, returns location of player as a String.
     * @return Last location recorded of player, as a String
     */
    public String getPlayerLocation( Player p, boolean isStaff ) {
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        String exact = "";
        if( isStaff )
            exact = "  (" + x + "," + y + ")";
        if( x==0 && y==0 )
            return "Not yet spotted" + exact;
        if( y >= TOP_FR  &&  y <= BOTTOM_FR  &&  x >= LEFT_FR  &&  x <= RIGHT_FR )
            return "in Flagroom" + exact;
        if( y >= TOP_MID  &&  y <= BOTTOM_MID  &&  x >= LEFT_MID  &&  x <= RIGHT_MID )
            return "in Mid Base" + exact;
        if( y >= TOP_LOWER  &&  y <= BOTTOM_LOWER  &&  x >= LEFT_LOWER  &&  x <= RIGHT_LOWER )
            return "in Lower Base" + exact;
        if( y <= BOTTOM_ROOF )
            return "Roofing ..." + exact;
        if( y >= TOP_SPAWN_AREA )
            return "in spawn" + exact;
        return "Outside base" + exact;
    }


    /**
     * Collects names of players on a freq into a Vector ArrayList by ship.
     * @param freq Frequency to collect info on
     * @return Vector array containing player names on given freq
     */
    public ArrayList<Vector<String>> getTeamData( int freq ) {
        ArrayList<Vector<String>> team = new ArrayList<Vector<String>>();
        // 8 ships plus potential spectators
        for( int i = 0; i < 9; i++ ) {
            team.add( new Vector<String>() );
        }
        Iterator<Player> i = m_botAction.getFreqPlayerIterator(freq);
        while( i.hasNext() ) {
            Player p = (Player)i.next();
            team.get(p.getShipType()).add(p.getPlayerName());
        }
        return team;
    }


    /**
     * Displays a help message depending on access level.
     *
     * @param sender is the person issuing the command.
     */
    public void doHelpCmd(String sender)
    {
        String[] helpMessage =
        {
                "!go <ArenaName>   -- Moves the bot to <ArenaName>.",
                //"!start            -- Starts pure pub settings.",
                //"!stop             -- Stops pure pub settings.",
                "!privfreqs        -- Toggles private frequencies & check for imbalances.",
                "!starttime <#>    -- Starts Flag Time game to <#> minutes",
                "!stoptime         -- Ends Flag Time mode.",
                "!stricttime       -- Toggles strict mode (all players warped)",
                "!autowarp         -- Enables and disables 'opt out' warping style",
                "!restrictions     -- Lists all current ship restrictions.",
                "!set <ship> <#>   -- Sets <ship> to restriction <#>.",
                "                     0=disabled; 1=any amount; other=weighted:",
                "                     2 = 1/2 of freq can be this ship, 5 = 1/5, ...",
                "!die              -- Logs the bot off of the server.",
                "!team             -- Tells you which ships your team members are in.",
                "!restrictions     -- Lists all current ship restrictions.",
                "!time             -- Provides time remaining when Flag Time mode.",
                "!warp             -- Warps you into flagroom at start of next round (flagtime)",
                //"!ship <ship#>     -- Puts you in ship <ship#>, keeping MVP status.",
                "!clearmines       -- Clears all mines you have laid, keeping MVP status.",
                "!terr             -- Shows terriers on the team & their last seen locations",
                "!whereis <name>   -- Shows last seen location of <name>",
                "!listvotes        -- Lists issues you can vote on to change the way pub's played",
                "!startvote <num>  -- Starts voting on issue <num>.  See !listvotes for numbers.",
                "!challenge <name> -- Issues a challenge (records kills vs eachother) to <name>",
                "!end              -- Ends your current challenge",
                "------- Pub Store (NEW) ------------------------- By Dexter ---------------------",
                "!storeon          -- Turns the store on",
                "!storeoff         -- Turns the store off",
                "!buy              -- Checks the list of items",
                "!b <itemNumber>   -- Buys an item of # Number",
                "!$                -- Checks how rich you are",
                "!about            -- Explains my System"
        
        };

        String[] playerHelpMessage =
        {
                "Hi.  I'm a bot that controls features in public arenas.",
                "I restrict ships, manage private frequencies, handle votes, run Flag Time mode.",
                "Commands:",
                "!time             -- Provides time remaining when Flag Time mode.",
                "!warp             -- Warps you into flagroom at start of next round (flagtime)",
                "!terr             -- Shows terriers on the team & their last seen locations",
                "!whereis <name>   -- Shows last seen location of <name> (if on your team)",
                "!team             -- Tells you which ships your team members are in.",
                //"!ship <ship#>     -- Puts you in ship <ship#>, keeping MVP status.",
                "!clearmines       -- Clears all mines you have laid, keeping MVP status.",
                "!restrictions     -- Lists all current ship restrictions.",
                "!listvotes        -- Lists issues you can vote on to change the way pub's played",
                "!startvote <num>  -- Starts voting on issue <num>.  See !listvotes for numbers.",
                "!challenge <name> -- Issues a challenge (records kills vs eachother) to <name>",
                "!end              -- Ends your current challenge",
                /*"------- Pub Store (NEW) ---- By Dexter --------------------------------------------",
                "!buy              -- Checks the list of items",
                "!b <itemNumber>   -- Buys an item of # Number",
                "!$                -- Checks how rich you are",
                "!about            -- Explains my System"*/
        };

        if( opList.isHighmod( sender ) )
            m_botAction.smartPrivateMessageSpam(sender, helpMessage);
        else
            m_botAction.smartPrivateMessageSpam(sender, playerHelpMessage);
    }

    /**
     * Begins voting on a particular issue, if voting is enabled.
     * @param sender Player sending
     * @param argString ID of issue on which to start voting
     */
    public void doStartVoteCmd(String sender, String argString ) {
        if( !m_votingEnabled ) {
            m_botAction.sendPrivateMessage( sender, "Voting on issues in pub is not currently enabled." );
            return;
        }
        if( m_currentVoteItem != -1 ) {
            m_botAction.sendPrivateMessage( sender, "Sorry, there's already a vote going.  Try again later." );
            return;
        }
        if( m_voteOptions.isEmpty() ) {
            m_botAction.sendPrivateMessage( sender, "Unfortunately, there are no issues on which to vote." );
            return;
        }
        Integer i;
        try {
            i = Integer.parseInt( argString );
        } catch( NumberFormatException e ) {
            m_botAction.sendPrivateMessage( sender, "Start a vote like this: !startvote #, where # is one of these vote numbers:" );
            doListVotesCmd(sender);
            return;
        }
        VoteOption v;
        try {
            v = m_voteOptions.get(i);
        } catch( ArrayIndexOutOfBoundsException e ) {
            m_botAction.sendPrivateMessage( sender, "That's not a valid vote number.  Use !startvote #, where # is a vote number found in !listvotes." );
            return;
        }
        if( v == null || i == 0 ) {
            m_botAction.sendPrivateMessage( sender, "That's not a valid vote number.  Use !startvote #, where # is a vote number found in !listvotes." );
            return;
        }
        long timeTillVote = (m_lastVote + MIN_TIME_BETWEEN_VOTES) - System.currentTimeMillis();
        if( timeTillVote > 0) {
            m_botAction.sendPrivateMessage( sender, "Sorry, there hasn't been enough time since the last vote.  You'll need to wait another " + getTimeString((int)(timeTillVote/1000)) + " before you can vote again." );
            return;
        }

        boolean canSet = setVoteOption( v, true );

        if( !canSet ) {
            m_botAction.sendPrivateMessage( sender, "That option is already set the way you want it!" );
            return;
        }

        // Success.  Let's vote
        m_votes.clear();
        m_currentVoteItem = i;

        m_botAction.sendArenaMessage("VOTE: " + v.displayText + "?  Type 1 for yes, 2 for no.", 1);

        TimerTask t = new TimerTask() {
        	public void run() {
        		doEndVote();
        	}
        };
        m_botAction.scheduleTask(t, VOTE_RUN_TIME );
    }

    /**
     * Begins voting on a particular issue, if voting is enabled.
     * @param sender Player sending
     * @param argString ID of issue on which to start voting
     */
    public void doListVotesCmd(String sender ) {
        if( !m_votingEnabled ) {
            m_botAction.sendPrivateMessage( sender, "Voting on issues in pub is not currently enabled." );
            return;
        }
        if( m_voteOptions.isEmpty() ) {
            m_botAction.sendPrivateMessage( sender, "Unfortunately, there are no issues on which to vote." );
            return;
        }
        LinkedList <String>spam = new LinkedList<String>();

        for( int i=1; i<m_voteOptions.size(); i++ ) {
            VoteOption v = m_voteOptions.get(i);
            if( setVoteOption( v, true ) )     // Only display options that can be set
                spam.add( "#" + i + (i>9?".   ":".    ") + v.displayText + "  (Requires " + v.percentRequired + "% majority/min " + v.minVotesRequired + " votes)" );
        }
        spam.add( "To start a vote, use !startvote #  (where # is the number you want to vote on)");
        long timeTillVote = (m_lastVote + MIN_TIME_BETWEEN_VOTES) - System.currentTimeMillis();
        if( timeTillVote > 0) {
            m_botAction.sendPrivateMessage( sender, "NOTE: There has been a vote within the last " + (MIN_TIME_BETWEEN_VOTES / Tools.TimeInMillis.MINUTE) + " minutes; you need to wait " + getTimeString((int)(timeTillVote/1000)) + " to start another." );
            return;
        }
        m_botAction.privateMessageSpam(sender, spam);
    }

    /**
     * Set a voting option once it's been voted in, or check to see if voting on it is necessary.
     * @param v
     * @param justChecking
     * @return True if option was set or can be set; false if it does not need to be set or had trouble being set
     */
    public boolean setVoteOption( VoteOption v, boolean justChecking ) {
        if( v == null )
            return false;
        String optionName = v.name;
        if( optionName.equals("ti1") ) {
            if( flagTimeStarted == true )
                return false;
            if( justChecking )
                return true;
            doStartTimeCmd(m_botAction.getBotName(), "3");
            return true;
        } else if( optionName.equals("ti2") ) {
            if( flagTimeStarted == false )
                return false;
            if( justChecking )
                return true;
            doStopTimeCmd(m_botAction.getBotName());
            return true;
        }else if( optionName.equals("st1") ) {
            if( strictFlagTime == true )
                return false;
            if( justChecking )
                return true;
            doStrictTimeCmd(m_botAction.getBotName());
            return true;
        } else if( optionName.equals("st2") ) {
            if( strictFlagTime == false )
                return false;
            if( justChecking )
                return true;
            doStrictTimeCmd(m_botAction.getBotName());
            return true;
        } else if( optionName.equals("le1") ) {
            if( shipWeights.get(4) == 1 )
                return false;
            if( justChecking )
                return true;
            doSetCmd(m_botAction.getBotName(), "4 1");
            return true;
        } else if( optionName.equals("le2") ) {
            if( shipWeights.get(4) == 0 )
                return false;
            if( justChecking )
                return true;
            doSetCmd(m_botAction.getBotName(), "4 0");
            return true;
        } else if( optionName.equals("ja1") ) {
            if( shipWeights.get(2) == 5 )
                return false;
            if( justChecking )
                return true;
            doSetCmd(m_botAction.getBotName(), "2 5");
            return true;
        } else if( optionName.equals("ja2") ) {
            if( shipWeights.get(2) == 1 )
                return false;
            if( justChecking )
                return true;
            doSetCmd(m_botAction.getBotName(), "2 1");
            return true;
        } else if( optionName.equals("we1") ) {
            if( shipWeights.get(6) == 5 )
                return false;
            if( justChecking )
                return true;
            doSetCmd(m_botAction.getBotName(), "6 5");
            return true;
        } else if( optionName.equals("we2") ) {
            if( shipWeights.get(6) == 1 )
                return false;
            if( justChecking )
                return true;
            doSetCmd(m_botAction.getBotName(), "6 1");
            return true;
        } else if( optionName.equals("pf1") ) {
            if( privFreqs == true )
                return false;
            if( justChecking )
                return true;
            doPrivFreqsCmd(m_botAction.getBotName());
            return true;
        } else if( optionName.equals("pf2") ) {
            if( privFreqs == false )
                return false;
            if( justChecking )
                return true;
            doPrivFreqsCmd(m_botAction.getBotName());
            return true;
        } else if( optionName.equals("w1") ) {
            if( warpAllowed == true )
                return false;
            if( justChecking )
                return true;
            doAllowWarpCmd(m_botAction.getBotName());
            return true;
        } else if( optionName.equals("w2") ) {
            if( warpAllowed == false )
                return false;
            if( justChecking )
                return true;
            doAllowWarpCmd(m_botAction.getBotName());
            return true;
        } else if( optionName.equals("aw1") ) {
            if( autoWarp == true )
                return false;
            if( justChecking )
                return true;
            doAutowarpCmd(m_botAction.getBotName());
            return true;
        } else if( optionName.equals("aw2") ) {
            if( autoWarp == false )
                return false;
            if( justChecking )
                return true;
            doAutowarpCmd(m_botAction.getBotName());
            return true;
        }
        return false;
    }

    /**
     * Ends the vote, tallying all votes cast, changing options as needed, and displaying results.
     */
    public void doEndVote() {
        m_lastVote = System.currentTimeMillis();
    	if( m_currentVoteItem < 1 || m_currentVoteItem > m_voteOptions.size() - 1 ) {
    		m_botAction.sendRemotePrivateMessage("MessageBot", "!lmessage qan:Invalid vote found in PurePub voting system!" + m_currentVoteItem );
        	m_currentVoteItem = -1;
    		return;
    	}

    	float yes = 0, no = 0;
    	for( Integer vote : m_votes.values() ) {
    		if( vote != null ) {
    			if( vote == 1 )
    				yes++;
    			else if( vote == 2 )
    				no++;
    		}
    	}
    	int percentage;
    	if( yes == 0 )
    		percentage = 0;
    	else if( no == 0 )
    		percentage = 100;
    	else
    		percentage = Math.round( (yes / (yes + no)) * 100 );
    	String results = "Y:" + (int)yes + " v N:" + (int)no + "  (" + percentage + "%";

    	VoteOption v = m_voteOptions.get(m_currentVoteItem);
    	if( v != null ) {
    		if( percentage >= v.percentRequired ) {
    			if( yes >= v.minVotesRequired ) {
    				m_botAction.sendArenaMessage("[" + v.displayText + "]  Vote PASSED.  " + results + "; needed " + v.percentRequired + "%)" );
    				setVoteOption(v, false);
    			} else {
        			m_botAction.sendArenaMessage("[" + v.displayText + "]  Vote FAILED.  " + results + "; needed " + v.percentRequired + "% and " + v.minVotesRequired + " votes)" );
    			}
    		} else {
    			m_botAction.sendArenaMessage(    "[" + v.displayText + "]  Vote FAILED.  " + results + "; needed " + v.percentRequired + "%)" );
    		}
    	}
    	m_currentVoteItem = -1;
    }

    /**
     * Challenges another player in pub to a tracked-kill competition.
     * @param sender Player sending
     * @param argString Name of the player to challenge
     */
    public void doChallengeCmd(String sender, String argString ) {
        if( !m_challengeEnabled )
            throw new RuntimeException( "Player challenges are not currently allowed." );
        if( argString == null || argString.equals("") )
            throw new RuntimeException( "Use !challenge <name> to challenge <name> to an informal pub duel (such as !challenge qan)" ); 
        Player p1 = m_botAction.getPlayer(sender);
        if( p1 == null )
            return;
        Player p2 = m_botAction.getPlayer(argString);
        if( p2 == null )
            p2 = m_botAction.getFuzzyPlayer(argString);
        if( p2 == null )
            throw new RuntimeException( "Can't locate player by name of '" + argString + "'." );

        PubChallenge runningChal = null;
        for( PubChallenge chal : m_challenges ) {
            if( chal.challengeActive() ) {
                if( chal.getPlayer1().getPlayerID() == p1.getPlayerID() || chal.getPlayer2().getPlayerID() == p1.getPlayerID() ) {
                    runningChal = chal;
                } else if( chal.getPlayer1().getPlayerID() == p2.getPlayerID() || chal.getPlayer2().getPlayerID() == p2.getPlayerID() ) {
                    throw new RuntimeException( p2.getPlayerName() + " is already involved in a challenge.  Try again later." );
                }
            } else {
                if( chal.getPlayer2().getPlayerID() == p1.getPlayerID() ) {
                    // Challenge needs confirmation; do it
                    chal.activateChallenge();
                    return;
                }
            }
        }

        if( runningChal != null ) {
            m_botAction.sendPrivateMessage( p1.getPlayerID(), "Your current challenge has been ended to run this one." );
            m_challenges.remove(runningChal);
            runningChal.endChallenge( p1 );
            runningChal = null;
        }

        m_challenges.add( new PubChallenge(p1,p2) );
        m_botAction.sendPrivateMessage( p1.getPlayerID(), "Challenge recorded.  " + p2.getPlayerName() + " will now need to !challenge " + p1.getPlayerName() + " to start the challenge." );
    }

    /**
     * Ends the current challenge.
     * @param sender Player sending
     * @param argString Name of the player to challenge
     */
    public void doEndCmd( String sender ) {
        if( !m_challengeEnabled )
            throw new RuntimeException( "Player challenges are not currently allowed." );
        Player p1 = m_botAction.getPlayer(sender);
        if( p1 == null )
            return;
        LinkedList <PubChallenge>deadchals = new LinkedList<PubChallenge>();

        PubChallenge runningChal = null;
        for( PubChallenge chal : m_challenges ) {
            if( chal.getPlayer1() == null )
                deadchals.add(chal);
            else if( chal.challengeActive() ) {
                if( chal.getPlayer2() == null )
                    deadchals.add(chal);
                else if( chal.getPlayer1().getPlayerID() == p1.getPlayerID() ||
                    chal.getPlayer2().getPlayerID() == p1.getPlayerID() ) {
                    m_challenges.remove(chal);
                    chal.endChallenge( p1 );
                    runningChal = chal;
                }
            }
        }
        for( PubChallenge dc : deadchals ) {
            m_challenges.remove(dc);
            dc = null;
        }
        if( runningChal == null )
            throw new RuntimeException( "Challenge not found.  (Are you sure you have one running?)" );
    }

    /* **********************************  SUPPORT METHODS  ************************************ */

    /**
     * This method returns the name of the player that sent the message regardless
     * of whether or not the message is a remote private message or a private
     * message.
     *
     * @param event is the message event.
     * @return the name of the sender is returned.  If the name of the sender
     * cannot be determined then null is returned.
     */
    private String getSender(Message event)
    {
        if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
            return event.getMessager();

        int senderID = event.getPlayerID();
        return m_botAction.getPlayerName(senderID);
    }


    /**
     * This method checks to see if a player is in a restricted ship, or the
     * weight for the given ship has been reached.  If either is true, then the
     * player is specced.
     *
     * Weights can be thought of as a denominator (bottom number) of a fraction,
     * the fraction saying how much of the freq can be made up of ships of this
     * type.  If the weight is 0, no ships of a type are allowed.  Weight of 1
     * gives a fraction of 1/1, or a whole -- the entire freq can be made up of
     * this ship.  Following that, 2 is half, 3 is a third, 4 is a fourth, etc.
     * Play with what weight seems right to you.
     *
     * Note that even with a very small freq, if a weight is 1 or greater, 1 ship
     * of this type is ALWAYS allowed.
     *
     * Value for ship "weights":
     *
     * 0  - No ships of this type allowed
     * 1  - Unlimited number of ships of this type are allowed
     * #  - If the number of current ships of the type on this frequency is
     *      greater than the total number of people on the frequency divided
     *      by this number (ships of this type > total ships / weight), then the
     *      ship is not allowed.  Exception to this rule is if the player is the
     *      only one on the freq currently in the ship.
     *
     * @param playerName is the player to be checked.
     * @param specMessage enables the spec message.
     */
    private void checkPlayer(int playerID)
    {
        Player player = m_botAction.getPlayer(playerID);
        if( player == null )
            return;

        int weight = shipWeights.get(player.getShipType()).intValue();

        // If weight is 1, unlimited number of that shiptype is allowed.  (Spec is also set to 1.)
        if( weight == 1 )
            return;

        // If weight is 0, ship is completely restricted.
        if( weight == 0 ) {
            
            int randomShip = player.getShipType();
            
            while( randomShip == player.getShipType() 
                    && shipWeights.get(randomShip) == 0){
                randomShip = new Random().nextInt(8);
                if(randomShip == 0)
                    randomShip = player.getShipType();
            }
            m_botAction.setShip(playerID, randomShip);
       	    m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "That ship has been restricted in this arena.");  
       	    m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "Please choose another, or type ?arena to select another arena. You've been put randomly in ship "+randomShip);
       	    return;
        }

        // For all other weights, we must decide whether they can play based on the
        // number of people on freq who are also using the ship.
        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        if( i == null)
            return;

        int freqTotal = 0;
        int numShipsOfType = 0;

        Player dummy;
        while( i.hasNext() ) {
            dummy = (Player)i.next();
            if( dummy != null) {
                if( dummy.getFrequency() == player.getFrequency() ) {
                    freqTotal++;
                    if( dummy.getShipType() == player.getShipType() )
                        numShipsOfType++;
                }
            }
        }

    	// Free pass if you're the only one on the freq, regardless of weight.
    	if( numShipsOfType <= 1 )
    	    return;

    	if( freqTotal == 0 ) {
            m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "Problem locating your freq!  Please contact a mod with ?help.");
            return;
    	}

        if( numShipsOfType > freqTotal / weight ) {
            // If unlimited spiders are allowed, set them to spider; else spec
            if( shipWeights.get(3).intValue() == 1 ) {
                m_botAction.setShip(playerID, 3);
                m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "There are too many ships of that kind (" + (numShipsOfType - 1) + "), or not enough people on the freq to allow you to play that ship.");
            } else {
                m_botAction.spec(playerID);
                m_botAction.spec(playerID);
                m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "There are too many ships of that kind (" + (numShipsOfType - 1) + "), or not enough people on the freq to allow you to play that ship.  Please choose another.");
            }
        }
    }


    /**
     * Removes a playerName from the freq tracking lists.
     *
     * @param playerName is the name of the player to remove.
     */
    private void removeFromLists(String playerName)
    {
        String lowerName = playerName.toLowerCase();

        freq0List.remove(lowerName);
        freq1List.remove(lowerName);
    }


    /**
     * Removes a playerName from the warp list.
     */
    private void removeFromWarpList(String playerName)
    {
        warpPlayers.remove( playerName );
    }


    /**
     * Sets a player to a freq and updates the freq lists.
     *
     * @param playerName is the name of the player to add.
     * @param freq is the new freq.
     */
    private void addToLists(String playerName, int freq)
    {
        String lowerName = playerName.toLowerCase();

        if(freq == FREQ_0)
            freq0List.add(lowerName);
        if(freq == FREQ_1)
            freq1List.add(lowerName);
    }


    /**
     * Checks to see if a player is on a private freq.  If they are then
     * they are changed to the pub freq with the fewest number of players.
     *
     * @param Player player is the player to check.
     * @param changeMessage is true if a changeMessage will be displayed.
     */
    private void checkFreq(int playerID, int freq, boolean changeMessage)
    {
        Player player = m_botAction.getPlayer(playerID);
        String playerName = player.getPlayerName();
        if( player == null )
            return;

        int ship = player.getShipType();
        int newFreq = freq;

        if( playerName == null )
            return;

        removeFromLists(playerName);

        if(ship != SPEC)
        {
            if(player != null && freq != FREQ_0 && freq != FREQ_1)
            {
                if(freq0List.size() <= freq1List.size())
                    newFreq = FREQ_0;
                else
                    newFreq = FREQ_1;
                if(changeMessage)
                    m_botAction.sendSmartPrivateMessage(playerName, "Private Frequencies are currently disabled.  You have been placed on a public Frequency.");
                m_botAction.setFreq(playerName, newFreq);
            }
            addToLists(playerName, newFreq);
        }
    }

    /**
     * Checks for imbalance in frequencies, and requests the stacked freq to even it up
     * if there's a significant gap.
     */
    private void checkFreqSizes() {
        if( MSG_AT_FREQSIZE_DIFF == -1 || !allEntered || privFreqs )
            return;
        int freq0 = m_botAction.getPlayingFrequencySize(0);
        int freq1 = m_botAction.getPlayingFrequencySize(1);
        int diff = java.lang.Math.abs( freq0 - freq1 );
        if( diff == freqSizeInfo[0] )
            return;
        freqSizeInfo[0] = diff;
        if( freqSizeInfo[0] >= MSG_AT_FREQSIZE_DIFF ) {
            if( freq0 > freq1 ) {
                m_botAction.sendOpposingTeamMessageByFrequency(0, "Teams unbalanced: " + freq0 + "v" + freq1 + ".  Volunteers requested; type =1 to switch to freq 1.  (Keep MVP status + earn " + NICEGUY_BOUNTY_AWARD + " bounty.)" );
                freqSizeInfo[1] = 1;
            } else {
                m_botAction.sendOpposingTeamMessageByFrequency(1, "Teams unbalanced: " + freq1 + "v" + freq0 + ".  Volunteers requested; type =0 to switch to freq 0.  (Keep MVP status + earn " + NICEGUY_BOUNTY_AWARD + " bounty.)" );
                freqSizeInfo[1] = 0;
            }
        }
        if( freqSizeInfo[0] >= KEEP_MVP_FREQSIZE_DIFF )
            teamsUneven = true;
        else
            teamsUneven = false;
    }

    /**
     * Specs all ships in the arena that are over the weighted restriction limit.
     */
    private void specRestrictedShips()
    {
        Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
        Player player;

        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            checkPlayer(player.getPlayerID());
        }
    }


    /**
     * Fills the freq lists for freqs 1 and 0.
     */
    private void fillFreqLists()
    {
        Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
        Player player;
        String lowerName;

        freq0List.clear();
        freq1List.clear();
        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            lowerName = player.getPlayerName().toLowerCase();
            if(player.getFrequency() == FREQ_0)
                freq0List.add(lowerName);
            if(player.getFrequency() == FREQ_1)
                freq1List.add(lowerName);
        }
    }


    /**
     * Fixes the freq of each player.
     */
    private void fixFreqs()
    {
        Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
        Player player;

        fillFreqLists();
        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            checkFreq(player.getPlayerID(), player.getFrequency(), false);
        }
    }


    /**
     * Starts the bot with CFG-specified setup commands.
     */
    public void startBot()
    {
        try{
        	
            String commands[] = botSets.getString(m_botAction.getBotName() + "Setup").split(",");
        	for(int k = 0; k < commands.length; k++) {
        		handleModCommand(m_botAction.getBotName(), commands[k]);
    		}
            String toggleInfoString = m_botAction.getBotSettings().getString(m_botAction.getBotName() + "Toggle");
            if( toggleInfoString != null && !toggleInfoString.trim().equals("") ) {
                String toggleSplit[] = toggleInfoString.split(":");
                if( toggleSplit.length == 2 ) {
                    try {
                        Integer toggleTime = Integer.parseInt(toggleSplit[1]);
                        String toggles[] = toggleSplit[0].split(";");
                        if( toggles.length == 2 ) {
                            toggleTask = new ToggleTask( toggles[0].split(","),toggles[1].split(",") );
                            m_botAction.scheduleTaskAtFixedRate(toggleTask, toggleTime * Tools.TimeInMillis.MINUTE, toggleTime * Tools.TimeInMillis.MINUTE );
                        } else {
                            Tools.printLog("Must have two toggles (did not find semicolon)");
                        }
                    } catch(NumberFormatException e) {
                        Tools.printLog("Unreadable time in toggle.");
                    }
                } else {
                    Tools.printLog("Must have both toggles and number of minutes defined (!toggle;!toggle2:mins)");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        started = true;
    }


    /* **********************************  FLAGTIME METHODS  ************************************ */
    /**
     * Starts a game of flag time mode.
     */
    private void doStartRound() {
        if(!flagTimeStarted)
            return;

        try {
            flagTimer.endGame();
            m_botAction.cancelTask(flagTimer);
        } catch (Exception e ) {
        }

        mineClearedPlayers.clear();
        flagTimer = new FlagCountTask();
        m_botAction.showObject(2300); //Turns on coutdown lvz
        m_botAction.hideObject(1000); //Turns off intermission lvz
        m_botAction.scheduleTaskAtFixedRate( flagTimer, 100, 1000);

        //To point system
        for(Iterator<Player> i = m_botAction.getPlayingPlayerIterator(); i.hasNext(); ){
            Player p = i.next();
            String playerName = p.getPlayerName();
            
            if(!players.containsKey(playerName)){
                this.players.put(playerName, new PubPlayer(playerName) );
                //Tools.printLog("Round starts: Added "+playerName);
            }
        }
    }


    /**
     * Displays rules and pauses for intermission.
     */
    private void doIntermission() {
        if(!flagTimeStarted)
            return;

        int roundNum = freq0Score + freq1Score + 1;

        String roundTitle = "";
        switch( roundNum ) {
        case 1:
            m_botAction.sendArenaMessage( "Object: Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win a round.  Best " + ( MAX_FLAGTIME_ROUNDS + 1) / 2 + " of "+ MAX_FLAGTIME_ROUNDS + " wins the game." );
            roundTitle = "The next game";
            break;
        case MAX_FLAGTIME_ROUNDS:
            roundTitle = "Final Round";
            break;
        default:
            roundTitle = "Round " + roundNum;
        }

        m_botAction.sendArenaMessage( roundTitle + " begins in " + getTimeString( INTERMISSION_SECS ) + ".  (Score: " + freq0Score + " - " + freq1Score + ")" + (strictFlagTime?"":("  Type !warp to set warp status, or send !help")) );

        m_botAction.cancelTask(startTimer);

        startTimer = new StartRoundTask();
        m_botAction.scheduleTask( startTimer, INTERMISSION_SECS * 1000 );
    }


    /**
     * Ends a round of Flag Time mode & awards prizes.
     * After, sets up an intermission, followed by a new round.
     */
    private void doEndRound( ) {
        if( !flagTimeStarted || flagTimer == null )
            return;

        HashSet <String>MVPs = new HashSet<String>();
        boolean gameOver     = false;       // Game over, man.. game over!
        int flagholdingFreq  = flagTimer.getHoldingFreq();
        int maxScore         = (MAX_FLAGTIME_ROUNDS + 1) / 2;  // Score needed to win
        int secs = flagTimer.getTotalSecs();
        int mins = secs / 60;
        int weight = (secs * 3 ) / 60;

        try {

            // Incremental bounty bonuses
            if( mins >= 90 )
                weight += 150;
            else if( mins >= 60 )
                weight += 100;
            else if( mins >= 30 )
                weight += 45;
            else if( mins >= 15 )
                weight += 20;


            if( flagholdingFreq == 0 || flagholdingFreq == 1 ) {
                if( flagholdingFreq == 0 )
                    freq0Score++;
                else
                    freq1Score++;

                if( freq0Score >= maxScore || freq1Score >= maxScore ) {
                    gameOver = true;
                } else {
                    int roundNum = freq0Score + freq1Score;
                    m_botAction.sendArenaMessage( "END OF ROUND " + roundNum + ": Freq " + flagholdingFreq + " wins after " + getTimeString( flagTimer.getTotalSecs() ) +
                            " (" + weight + " bounty bonus)  Score: " + freq0Score + " - " + freq1Score, 1 );
                }

            } else {
                if( flagholdingFreq < 100 )
                    m_botAction.sendArenaMessage( "END ROUND: Freq " + flagholdingFreq + " wins the round after " + getTimeString( flagTimer.getTotalSecs() ) + " (" + weight + " bounty bonus)", 1 );
                else
                    m_botAction.sendArenaMessage( "END ROUND: A private freq wins the round after " + getTimeString( flagTimer.getTotalSecs() ) + " (" + weight + " bounty bonus)", 1 );
            }

            int special = 0;
            // Special prizes for longer battles (add more if you think of any!)
            if( mins > 15 ) {
                Random r = new Random();
                int chance = r.nextInt(100);

                if( chance == 99 ) {
                    special = 8;
                } else if( chance == 98 ) {
                    special = 7;
                } else if( chance >= 94 ) {
                    special = 6;
                } else if( chance >= 90 ) {
                    special = 5;
                } else if( chance >= 75 ) {
                    special = 4;
                } else if( chance >= 60 ) {
                    special = 3;
                } else if( chance >= 35 ) {
                    special = 2;
                } else {
                    special = 1;
                }
            }

            Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
            Player player;
            while(iterator.hasNext()) {
                player = (Player) iterator.next();
                if( player != null ) {
                    if(player.getFrequency() == flagholdingFreq ) {
                        String playerName = player.getPlayerName();

                        Integer i = playerTimes.get( playerName );

                        if( i != null ) {
                            // Calculate amount of time actually spent on freq

                            int timeOnFreq = secs - i.intValue();
                            int percentOnFreq = (int)( ( (float)timeOnFreq / (float)secs ) * 100 );
                            int modbounty = (int)(weight * ((float)percentOnFreq / 100));

                            if( percentOnFreq == 100 ) {
                                MVPs.add( playerName );
                                m_botAction.sendPrivateMessage( playerName, "For staying with the same freq the entire match, you are an MVP and receive the full bonus: " + modbounty );
                                int grabs = flagTimer.getFlagGrabs( playerName );
                                if( special == 4 ) {
                                    m_botAction.sendPrivateMessage( playerName, "You also receive an additional " + weight + " bounty as a special prize!" );
                                    modbounty *= 2;
                                }
                                if( grabs != 0 ) {
                                    modbounty += (modbounty * ((float)grabs / 10.0));
                                    m_botAction.sendPrivateMessage( playerName, "For your " + grabs + " flag grabs, you also receive an additional " + grabs + "0% bounty, for a total of " + modbounty );
                                }

                            } else {
                                m_botAction.sendPrivateMessage( playerName, "You were with the same freq and ship for the last " + getTimeString(timeOnFreq) + ", and receive " + percentOnFreq  + "% of the bounty reward: " + modbounty );
                            }

                            m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize " + modbounty);
                        }

                        if( MVPs.contains( playerName ) ) {
                            switch( special ) {
                            case 1:  // "Refreshments" -- replenishes all essentials + gives anti
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #6");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #15");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #20");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #21");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #21");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #21");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #22");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #27");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #28");
                                break;
                            case 2:  // "Full shrap"
                                for(int j = 0; j < 5; j++ )
                                    m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #19");
                                break;
                            case 3:  // "Trophy" -- decoy given
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                break;
                            case 4:  // "Double bounty reward"
                                break;
                            case 5:  // "Triple trophy" -- 3 decoys
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                break;
                            case 6:  // "Techno Dance Party" -- plays victory music :P
                                break;
                            case 7:  // "Sore Loser's Revenge" -- engine shutdown!
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #14");
                                break;
                            case 8:  // "Bodyguard" -- shields
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #18");
                                break;
                            }
                        }
                    }
                }
            }

            String[] leaderInfo = flagTimer.getTeamLeader( MVPs );
            if( leaderInfo.length != 3 )
                return;
            String name, MVplayers = "";
            MVPs.remove( leaderInfo[0] );
            if( !leaderInfo[2].equals("") ) {
                String otherleaders[] = leaderInfo[2].split(", ");
                for( int j = 0; j<otherleaders.length; j++ )
                    MVPs.remove( otherleaders[j] );
            }
            Iterator<String> i = MVPs.iterator();

            if( i.hasNext() ) {
                switch( special ) {
                case 1:  // "Refreshments" -- replenishes all essentials + gives anti
                    m_botAction.sendArenaMessage( "Prize for MVPs: Refreshments! (+ AntiWarp for all loyal spiders)" );
                    break;
                case 2:  // "Full shrap"
                    m_botAction.sendArenaMessage( "Prize for MVPs: Full shrap!" );
                    break;
                case 3:  // "Trophy" -- decoy given
                    m_botAction.sendArenaMessage( "Prize for MVPs: Life-size Trophies of Themselves!" );
                    break;
                case 4:  // "Double bounty reward"
                    m_botAction.sendArenaMessage( "Prize for MVPs: Double Bounty Bonus!  (MVP bounty: " + weight * 2 + ")" );
                    break;
                case 5:  // "Triple trophy" -- 3 decoys
                    m_botAction.sendArenaMessage( "Prize for MVPs: The Triple Platinum Trophy!" );
                    break;
                case 6:  // "Techno Dance Party" -- plays victory music :P
                    m_botAction.sendArenaMessage( "Prize for MVPs: Ultimate Techno Dance Party!", 102);
                    break;
                case 7:  // "Sore Loser's Revenge" -- engine shutdown!
                    m_botAction.sendArenaMessage( "Prize for MVPs: Sore Loser's REVENGE!" );
                    break;
                case 8:  // "Bodyguard" -- shields
                    m_botAction.sendArenaMessage( "Prize for MVPs: Personal Body-Guard!" );
                    break;
                }

                MVplayers = (String)i.next();
                int grabs = flagTimer.getFlagGrabs(MVplayers);
                if( grabs > 0 )
                    MVplayers += "(" + grabs + ")";
            }
            int grabs = 0;
            while( i.hasNext() ) {
                name = (String)i.next();
                grabs = flagTimer.getFlagGrabs(name);
                if( grabs > 0 )
                    MVplayers = MVplayers + ", " + name + "(" + grabs + ")";
                else
                    MVplayers = MVplayers + ", " + name;
            }

            if( leaderInfo[0] != "" ) {
                if( leaderInfo[2] == "" )
                    m_botAction.sendArenaMessage( "Team Leader was " + leaderInfo[0] + "!  (" + leaderInfo[1] + " flag claim(s) + MVP)" );
                else
                    m_botAction.sendArenaMessage( "Team Leaders were " + leaderInfo[2] + "and " + leaderInfo[0] + "!  (" + leaderInfo[1] + " flag claim(s) + MVP)" );
            }
            if( MVplayers != "" )
                m_botAction.sendArenaMessage( "MVPs (+ claims): " + MVplayers );

        } catch(Exception e) {
            Tools.printStackTrace( e );
        }

        int intermissionTime = 10000;

        if( gameOver ) {
            intermissionTime = 20000;
            doScores(intermissionTime);

            int diff = 0;
            String winMsg = "";
            if( freq0Score >= maxScore ) {
                if( freq1Score == 0 )
                    diff = -1;
                else
                    diff = freq0Score - freq1Score;
            } else if( freq1Score >= maxScore ) {
                if( freq0Score == 0 )
                    diff = -1;
                else
                    diff = freq1Score - freq0Score;
            }
            switch( diff ) {
            case -1:
                winMsg = " for their masterful victory!";
                break;
            case 1:
                winMsg = " for their close win!";
                break;
            case 2:
                winMsg = " for a well-executed victory!";
                break;
            default:
                winMsg = " for their win!";
                break;
            }
            m_botAction.sendArenaMessage( "GAME OVER!  Freq " + flagholdingFreq + " has won the game after " + getTimeString( flagTimer.getTotalSecs() ) +
                    " (" + weight + " bounty bonus)  Final score: " + freq0Score + " - " + freq1Score, 2 );
            m_botAction.sendArenaMessage( "Give congratulations to FREQ " + flagholdingFreq + winMsg );

            freq0Score = 0;
            freq1Score = 0;
        }	else
        		doScores(intermissionTime);


        try {
            flagTimer.endGame();
            m_botAction.cancelTask(flagTimer);
            m_botAction.cancelTask(intermissionTimer);
        } catch (Exception e ) {
        }

        intermissionTimer = new IntermissionTask();
        m_botAction.scheduleTask( intermissionTimer, intermissionTime );
    }


    /**
     * Adds all players to the hashmap which stores the time, in flagTimer time,
     * when they joined their freq.
     */
    public void setupPlayerTimes() {
        playerTimes = new HashMap<String,Integer>();

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        Player player;

        try {
            while( i.hasNext() ) {
                player = (Player)i.next();
                playerTimes.put( player.getPlayerName(), new Integer(0) );
            }
        } catch (Exception e) {
        }
    }


    /**
     * Formats an integer time as a String.
     * @param time Time in seconds.
     * @return Formatted string in 0:00 format.
     */
    public String getTimeString( int time ) {
        if( time <= 0 ) {
            return "0:00";
        } else {
            int minutes = time / 60;
            int seconds = time % 60;
            return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
    }


    /**
     * Warps all players who have PMed with !warp into FR at start.
     * Ensures !warpers on freqs are warped all to 'their' side, but not predictably.
     */
    private void warpPlayers() {
        if( !warpAllowed )
            return;

        if( useAprilFoolsPoints ) {
            warpPtsLeftX = warpPtsLeftX_April1;
            warpPtsLeftY = warpPtsLeftY_April1;
            warpPtsRightX = warpPtsRightX_April1;
            warpPtsRightY = warpPtsRightY_April1;
        }

        Iterator<?> i;

        if( strictFlagTime )
            i = m_botAction.getPlayingPlayerIterator();
        else
            i = warpPlayers.iterator();

        Random r = new Random();
        int rand;
        Player p;
        String pname;
        LinkedList <String>nullPlayers = new LinkedList<String>();

        int randomside = r.nextInt( 2 );

        while( i.hasNext() ) {
            if( strictFlagTime ) {
                p = (Player)i.next();
                pname = p.getPlayerName();
            } else {
                pname = (String)i.next();
                p = m_botAction.getPlayer( pname );
            }

            if( p != null ) {
                if( strictFlagTime )
                    rand = 0;           // Warp freqmates to same spot in strict mode.
                                        // The warppoints @ index 0 must be set up
                                        // to default/earwarps for this to work properly.
                else
                    rand = r.nextInt( NUM_WARP_POINTS_PER_SIDE );
                if( p.getFrequency() % 2 == randomside )
                    doPlayerWarp( pname, warpPtsLeftX[rand], warpPtsLeftY[rand] );
                else
                    doPlayerWarp( pname, warpPtsRightX[rand], warpPtsRightY[rand] );
            } else {
                if( !strictFlagTime ) {
                    nullPlayers.add( pname );
                }
            }
        }

        if( ! nullPlayers.isEmpty() ) {
            i = nullPlayers.iterator();
            while( i.hasNext() ) {
                warpPlayers.remove( (String)i.next() );
            }
        }
    }


    /**
     * In Strict Flag Time mode, warp all players to a safe 10 seconds before
     * starting.  This gives a semi-official feeling to the game, and resets
     * all mines, etc.
     */
    private void safeWarp() {
        // Prevent pre-laid mines and portals in strict flag time by setting to WB and back again (slightly hacky)
        HashMap<String,Integer> players = new HashMap<String,Integer>();
        HashMap<String,Integer> bounties = new HashMap<String,Integer>();
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        Player p;
        while( it.hasNext() ) {
            p = it.next();
            if( p != null ) {
                if( p.getShipType() == Tools.Ship.SHARK || p.getShipType() == Tools.Ship.TERRIER || p.getShipType() == Tools.Ship.LEVIATHAN ) {
                    players.put( p.getPlayerName(), new Integer(p.getShipType()) );
                    bounties.put( p.getPlayerName(), new Integer(p.getBounty()) );
                    m_botAction.setShip(p.getPlayerName(), 1);
                }
            }
        }
        Iterator<String> it2 = players.keySet().iterator();
        String name;
        Integer ship, bounty;
        while( it2.hasNext() ) {
            name = it2.next();
            ship = players.get(name);
            bounty = bounties.get(name);
            if( ship != null )
                m_botAction.setShip( name, ship.intValue() );
            if( bounty != null )
                m_botAction.giveBounty( name, bounty.intValue() - 3 );
        }

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        while( i.hasNext() ) {
            p = i.next();
            if( p != null ) {
                if( p.getFrequency() % 2 == 0 )
                    m_botAction.warpTo( p.getPlayerID(), SAFE_LEFT_X, SAFE_LEFT_Y );
                else
                    m_botAction.warpTo( p.getPlayerID(), SAFE_RIGHT_X, SAFE_RIGHT_Y );
            }
        }
    }


    /**
     * Warps a player within a radius of 2 tiles to provided coord.
     *
     * @param playerName
     * @param xCoord
     * @param yCoord
     * @param radius
     * @author Cpt.Guano!
     */
    private void doPlayerWarp(String playerName, int xCoord, int yCoord ) {
        int radius = 2;
        double randRadians;
        double randRadius;
        int xWarp = -1;
        int yWarp = -1;

        randRadians = Math.random() * 2 * Math.PI;
        randRadius = Math.random() * radius;
        xWarp = calcXCoord(xCoord, randRadians, randRadius);
        yWarp = calcYCoord(yCoord, randRadians, randRadius);

        m_botAction.warpTo(playerName, xWarp, yWarp);
    }


    private int calcXCoord(int xCoord, double randRadians, double randRadius)
    {
        return xCoord + (int) Math.round(randRadius * Math.sin(randRadians));
    }


    private int calcYCoord(int yCoord, double randRadians, double randRadius)
    {
        return yCoord + (int) Math.round(randRadius * Math.cos(randRadians));
    }

    /**
     * Shows and hides scores (used at intermission only).
     * @param time Time after which the score should be removed
     */
    private void doScores(int time) {
        int[] objs1 = {2000,(freq0Score<10 ? 60 + freq0Score : 50 + freq0Score), (freq0Score<10 ? 80 + freq1Score : 70 + freq1Score)};
        boolean[] objs1Display = {true,true,true};
    	scoreDisplay = new AuxLvzTask(objs1, objs1Display);
        int[] objs2 = {2200,2000,(freq0Score<10 ? 60 + freq0Score : 50 + freq0Score), (freq0Score<10 ? 80 + freq1Score : 70 + freq1Score)};
        boolean[] objs2Display = {true,false,false,false};
    	scoreRemove = new AuxLvzTask(objs2, objs2Display);
    	m_botAction.scheduleTask(scoreDisplay, 1000);		// Do score display
    	m_botAction.scheduleTask(scoreRemove, time-1000);	// do score removal
    	m_botAction.showObject(2100);

    }


    /* **********************************  TIMERTASK CLASSES  ************************************ */

    /**
     * This private class logs the bot off.  It is used to give a slight delay
     * to the log off process.
     */
    private class DieTask extends TimerTask
    {

        /**
         * This method logs the bot off.
         */
        public void run()
        {
            m_botAction.die();
        }
    }


    /**
     * This private class starts the round.
     */
    private class StartRoundTask extends TimerTask {

        /**
         * Starts the round when scheduled.
         */
        public void run() {
            doStartRound();
        }
    }


    /**
     * This private class provides a pause before starting the round.
     */
    private class IntermissionTask extends TimerTask {

        /**
         * Starts the intermission/rule display when scheduled.
         */
        public void run() {
            doIntermission();
            m_botAction.showObject(1000); //Shows intermission lvz
        }
    }

    /**
     * Used to turn on/off a set of LVZ objects at a particular time.
     */
    private class AuxLvzTask extends TimerTask {
        public int[] objNums;
        public boolean[] showObj;

        /**
         * Creates a new AuxLvzTask, given obj numbers defined in the LVZ and whether
         * or not to turn them on or off.  Cardinality of the two arrays must be the same.
         * @param objNums Numbers of objs defined in the LVZ to turn on or off
         * @param showObj For each index, true to show the obj; false to hide it
         */
        public AuxLvzTask(int[] objNums, boolean[] showObj)	{
            if( objNums.length != showObj.length )
                throw new RuntimeException("AuxLvzTask constructor error: Arrays must have same cardinality.");
            this.objNums = objNums;
            this.showObj = showObj;
        }

        /**
         * Shows and hides set objects.
         */
        public void run() {
        	for(int i=0 ; i<objNums.length ; i++)	{
                if(showObj[i])
                    m_botAction.showObject(objNums[i]);
                else
                	m_botAction.hideObject(objNums[i]);
            }
        }
    }

    /**
     * This private class counts the consecutive flag time an individual team racks up.
     * Upon reaching the time needed to win, it fires the end of the round.
     */
    private class FlagCountTask extends TimerTask {
        int flagHoldingFreq, flagClaimingFreq;
        int secondsHeld, totalSecs, claimSecs, preTimeCount;
        int claimerID;
        boolean isStarted, isRunning, isBeingClaimed;
        HashMap <String,Integer>flagClaims;

        /**
         * FlagCountTask Constructor
         */
        public FlagCountTask() {
            flagHoldingFreq = -1;
            secondsHeld = 0;
            totalSecs = 0;
            claimSecs = 0;
            isStarted = false;
            isRunning = false;
            isBeingClaimed = false;
            flagClaims = new HashMap<String,Integer>();
        }

        /**
         * This method is called by the FlagClaimed event, and tracks who currently
         * has or is in the process of claiming the flag.  While the flag can physically
         * be claimed in the game, 3 seconds are needed to claim it for the purpose of
         * the game.
         * @param freq Frequency of flag claimer
         * @param pid PlayerID of flag claimer
         */
        public void flagClaimed( int freq, int pid ) {
            if( isRunning == false || freq == -1 )
                return;

            // Return the flag back to the team that had it if the claim attempt
            // is unsuccessful (countered by the holding team)
            if( freq == flagHoldingFreq ) {
                isBeingClaimed = false;
                claimSecs = 0;
                return;
            }

            if( freq != flagHoldingFreq ) {
                if( (!isBeingClaimed) || (isBeingClaimed && freq != flagClaimingFreq) ) {
                    claimerID = pid;
                    flagClaimingFreq = freq;
                    isBeingClaimed = true;
                    claimSecs = 0;
                }
            }
        }

        /**
         * Assigns flag (internally) to the claiming frequency.
         *
         */
        public void assignFlag() {
            flagHoldingFreq = flagClaimingFreq;

            int remain = (flagMinutesRequired * 60) - secondsHeld;


            Player p = m_botAction.getPlayer( claimerID );
            if( p != null ) {

                addFlagClaim( p.getPlayerName() );

                if( remain < 60 ) {
                    if( remain < 4 )
                        {m_botAction.sendArenaMessage( "INCONCIEVABLE!!: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with just " + remain + " second" + (remain == 1 ? "" : "s") + " left!", 65 );m_botAction.showObject(2500);m_botAction.showObject(2600);} //'Hot Freaking Daym!!' lvz
                    else if( remain < 11 )
                        {m_botAction.sendArenaMessage( "AMAZING!: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with just " + remain + " sec. left!" );m_botAction.showObject(2600);} // 'Daym!' lvz
                    else if( remain < 25 )
                        m_botAction.sendArenaMessage( "SAVE!: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with " + remain + " sec. left!" );
                    else
                        m_botAction.sendArenaMessage( "Save: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with " + remain + " sec. left." );
                }
            }

            m_botAction.showObject(2400); // Shows flag claimed lvz
            isBeingClaimed = false;
            flagClaimingFreq = -1;
            secondsHeld = 0;

        }

        /**
         * Increments a count for player claiming the flag.
         * @param name Name of player.
         */
        public void addFlagClaim( String name ) {
            Integer count = flagClaims.get( name );
            if( count == null ) {
                flagClaims.put( name, new Integer(1) );
            } else {
                flagClaims.remove( name );
                flagClaims.put( name, new Integer( count.intValue() + 1) );
            }
        }

        /**
         * Ends the game for the timer's internal purposes.
         */
        public void endGame() {
            objs.hideAllObjects();
            m_botAction.setObjects();
            isRunning = false;
        }

        /**
         * Sends time info to requested player.
         * @param name Person to send info to
         */
        public void sendTimeRemaining( String name ) {
            m_botAction.sendSmartPrivateMessage( name, getTimeInfo() );
        }

        /**
         * @return True if a game is currently running; false if not
         */
        public boolean isRunning() {
            return isRunning;
        }

        /**
         * Gives the name of the top flag claimers out of the MVPs.  If there is
         * a tie, does not care because it's only bragging rights anyway. :P
         * @return Array of size 2, index 0 being the team leader and 1 being # flaggrabs
         */
        public String[] getTeamLeader( HashSet<String> MVPs ) {
            String[] leaderInfo = {"", "", ""};
            HashSet <String>ties = new HashSet<String>();

            if( MVPs == null )
                return leaderInfo;
            try {
                Iterator<String> i = MVPs.iterator();
                Integer dummyClaim, highClaim = new Integer(0);
                String leader = "", dummyPlayer;

                while( i.hasNext() ) {
                    dummyPlayer = i.next();
                    dummyClaim = flagClaims.get( dummyPlayer );
                    if( dummyClaim != null ) {
                        if( dummyClaim.intValue() > highClaim.intValue() ) {
                            leader = dummyPlayer;
                            highClaim = dummyClaim;
                            ties.clear();
                        } else if ( dummyClaim.intValue() == highClaim.intValue() ) {
                            ties.add(dummyPlayer);
                        }
                    }
                }
                leaderInfo[0] = leader;
                leaderInfo[1] = highClaim.toString();
                i = ties.iterator();
                while( i.hasNext() )
                    leaderInfo[2] += i.next() + ", ";
                return leaderInfo;

            } catch (Exception e ) {
                Tools.printStackTrace( e );
                return leaderInfo;
            }

        }

        /**
         * Returns number of flag grabs for given player.
         * @param name Name of player
         * @return Flag grabs
         */
        public int getFlagGrabs( String name ) {
            Integer grabs = flagClaims.get( name );
            if( grabs == null )
                return 0;
            else
                return grabs;
        }

        /**
         * @return Time-based status of game
         */
        public String getTimeInfo() {
            int roundNum = freq0Score + freq1Score + 1;

            if( isRunning == false ) {
                if( roundNum == 1 )
                    return "Round 1 of a new game is just about to start.";
                else
                    return "We are currently in between rounds (round " + roundNum + " starting soon).  Score: " + freq0Score + " - " + freq1Score;
            }
            return "ROUND " + roundNum + " Stats: " + (flagHoldingFreq == -1 || flagHoldingFreq > 99 ? "?" : "Freq " + flagHoldingFreq ) + " holding for " + getTimeString(secondsHeld) + ", needs " + getTimeString( (flagMinutesRequired * 60) - secondsHeld ) + " more.  [Time: " + getTimeString( totalSecs ) + "]  Score: " + freq0Score + " - " + freq1Score;
        }

        /**
         * @return Total number of seconds round has been running.
         */
        public int getTotalSecs() {
            return totalSecs;
        }

        /**
         * @return Frequency that currently holds the flag
         */
        public int getHoldingFreq() {
            return flagHoldingFreq;
        }

        /**
         * Timer running once per second that handles the starting of a round,
         * displaying of information updates every 5 minutes, the flag claiming
         * timer, and total flag holding time/round ends.
         */
        public void run() {
            if( isStarted == false ) {
                int roundNum = freq0Score + freq1Score + 1;
                if( preTimeCount == 0 ) {
                    m_botAction.sendArenaMessage( "Next round begins in 10 seconds . . ." );
                    if( strictFlagTime )
                        safeWarp();
                }
                preTimeCount++;

                if( preTimeCount >= 10 ) {
                    isStarted = true;
                    isRunning = true;
                    m_botAction.sendArenaMessage( ( roundNum == MAX_FLAGTIME_ROUNDS ? "FINAL ROUND" : "ROUND " + roundNum) + " START!  Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win the round.", 1 );
                    m_botAction.resetFlagGame();
                    setupPlayerTimes();
                    warpPlayers();
                    return;
                }
            }

            if( isRunning == false )
                return;

            totalSecs++;

            // Display mode info at 5 min increments, unless we are near the end of a game
            if( (totalSecs % (5 * 60)) == 0 && ( (flagMinutesRequired * 60) - secondsHeld > 30) ) {
                m_botAction.sendArenaMessage( getTimeInfo() );
            }

            if( isBeingClaimed ) {
                claimSecs++;
                if( claimSecs >= FLAG_CLAIM_SECS ) {
                    claimSecs = 0;
                    assignFlag();
                }
                return;
            }

            if( flagHoldingFreq == -1 )
                return;

            secondsHeld++;

            do_updateTimer();

            int flagSecsReq = flagMinutesRequired * 60;
            if( secondsHeld >= flagSecsReq ) {
                endGame();
                doEndRound();
            } else if( flagSecsReq - secondsHeld == 60 ) {
                m_botAction.sendArenaMessage( (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "Private freq" ) + " will win in 60 seconds." );
            } else if( flagSecsReq - secondsHeld == 10 ) {
                m_botAction.sendArenaMessage( (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "Private freq" ) + " will win in 10 seconds . . ." );
            }
        }

        /**
         * Runs the LVZ-based timer.
         */
        private void do_updateTimer() {
            int secsNeeded = flagMinutesRequired * 60 - secondsHeld;
            objs.hideAllObjects();
            int minutes = secsNeeded / 60;
            int seconds = secsNeeded % 60;
            if( minutes < 1 ) objs.showObject( 1100 );
            if( minutes > 10 )
                objs.showObject( 10 + ((minutes - minutes % 10)/10) );
            objs.showObject( 20 + (minutes % 10) );
            objs.showObject( 30 + ((seconds - seconds % 10)/10) );
            objs.showObject( 40 + (seconds % 10) );
            m_botAction.setObjects();
        }
    }

    /**
     * Task used to toggle bot options on or off.  (Define toggles inside CFG.)
     */
    private class ToggleTask extends TimerTask {
        String[] toggleOn;
        String[] toggleOff;
        boolean stateOn = false;

        public ToggleTask( String[] on, String[] off ) {
            toggleOn = on;
            toggleOff = off;
        }

        public void run() {
            if( stateOn ) {
                stateOn = false;
                for(int k = 0; k < toggleOff.length; k++) {
                    if( toggleOff[k].startsWith("*") ) {
                        m_botAction.sendUnfilteredPublicMessage( toggleOff[k] );
                    } else {
                        handleModCommand( m_botAction.getBotName(), toggleOff[k] );
                    }
                }
            } else {
                stateOn = true;
                for(int k = 0; k < toggleOn.length; k++) {
                    if( toggleOn[k].startsWith("*") ) {
                        m_botAction.sendUnfilteredPublicMessage( toggleOn[k] );
                    } else {
                        handleModCommand( m_botAction.getBotName(), toggleOn[k] );
                    }
                }
            }
        }
    }

    private class VoteOption {
        String name;                    // Name used internally by the game for this option
        String displayText;             // Text displayed to players listing voting options
        int percentRequired = 0;        // 50 to 100
        int minVotesRequired = 0;       // Min # votes required, regardless of percentage

        public VoteOption( String name, String display, int percent, int minVotes ) {
            this.name = name;
            displayText = display;
            percentRequired = percent;
            minVotesRequired = minVotes;
        }
    }

    /**
     * Class to track pub challenges.
     * @author affirmative
     */
    private class PubChallenge {
        Player p1;
        Player p2;
        boolean challengeActive = false;
        int p1Points = 0;
        int p2Points = 0;

        public PubChallenge( Player challenger, Player challenged ) {
            p1 = challenger;
            p2 = challenged;
        }

        public void activateChallenge() {
            if( p1 == null ) {
                if( p2 == null )
                    return;
                m_botAction.sendPrivateMessage( p2.getPlayerID(), "Challenge can't be activated; can't find Player 1." );
                return;
            }
            else if( p2 == null ) {
                if( p1 == null )
                    return;
                m_botAction.sendPrivateMessage( p1.getPlayerID(), "Challenge can't be activated; can't find Player 2." );
                return;
            }

            challengeActive = true;
            m_botAction.sendPrivateMessage( p1.getPlayerID(), "Challenge has begun.  " + p1.getPlayerName() + " 0  -  0 " + p2.getPlayerName() );
            m_botAction.sendPrivateMessage( p2.getPlayerID(), "Challenge has begun.  " + p1.getPlayerName() + " 0  -  0 " + p2.getPlayerName() );
        }

        public void endChallenge( Player p ) {
            challengeActive = false;
            if( p1 == null ) {
                if( p2 == null )
                    return;
                m_botAction.sendPrivateMessage( p2.getPlayerID(), "Challenge END.  (Player 1 left)" );
            }
            else if( p2 == null ) {
                if( p1 == null )
                    return;
                m_botAction.sendPrivateMessage( p1.getPlayerID(), "Challenge END.  (Player 2 left)" );
            }

            if( p1Points > p2Points ) {
                m_botAction.sendPrivateMessage( p1.getPlayerID(), "Challenge END by " + p.getPlayerName() + ":  " + p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() + "  " + p1.getPlayerName().toUpperCase() + " WINS!" );
                m_botAction.sendPrivateMessage( p2.getPlayerID(), "Challenge END by " + p.getPlayerName() + ":  " + p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() + "  " + p1.getPlayerName().toUpperCase() + " WINS!" );
            } else if( p2Points > p1Points ) {
                m_botAction.sendPrivateMessage( p1.getPlayerID(), "Challenge END by " + p.getPlayerName() + ":  " + p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() + "  " + p2.getPlayerName().toUpperCase() + " WINS!" );
                m_botAction.sendPrivateMessage( p2.getPlayerID(), "Challenge END by " + p.getPlayerName() + ":  " + p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() + "  " + p1.getPlayerName().toUpperCase() + " WINS!" );
            } else {
                m_botAction.sendPrivateMessage( p1.getPlayerID(), "Challenge END by " + p.getPlayerName() + ":  " + p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() + "  TIE!" );
                m_botAction.sendPrivateMessage( p2.getPlayerID(), "Challenge END by " + p.getPlayerName() + ":  " + p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() + "  " + p1.getPlayerName().toUpperCase() + " WINS!" );
            }
        }

        public void reportKill( int whoDunIt ) {
            if( whoDunIt == 1 ) {
                p1Points++;
            } else {
                p2Points++;
            }
            m_botAction.sendPrivateMessage( p1.getPlayerID(), p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() );
            m_botAction.sendPrivateMessage( p2.getPlayerID(), p1.getPlayerName() + " " + p1Points + "  -  " + p2Points + " " + p2.getPlayerName() );
        }

        public boolean challengeActive() {
            return challengeActive;
        }

        public Player getPlayer1() {
            return p1;
        }

        public Player getPlayer2() {
            return p2;
        }
    }


}
