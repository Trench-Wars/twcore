package twcore.bots.elim;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.lvz.CoordType;
import twcore.core.lvz.Layer;
import twcore.core.lvz.LvzManager;
import twcore.core.util.Tools;

/**
 * Simple class that keeps a cache of the leader board for a specific ship type.
 * <p>
 * <ul><b>Main features:</b>
 *  <li>Loads only the info that is needed from the SQL database;
 *  <li>Formats one or multiple ranking lists;
 *  <li>Uses object modifications to dynamically create a LVZ splash image;
 *  <li>Regulates the displaying of the images to the players;
 *  <li>Most settings, including rank formatting, is done through the config file.
 * </ul>
 * This class is written with the intent of being general purpose. If you wish to adapt this to
 * work in another system, the main things you will need to be aware of are the following:
 * <ul><b>Initial setup</b>
 *  <li>Modify the initial private static final variables at the start of this class;
 *  <li>Create a new or modify the current elim.lvz to get the background you want;
 *  <li>Ensure there are enough objects in the elim.lvz for the amount of data you want to display. Spare objects is recommended;
 *  <li>If you create a complete new LVZ, this module assumes that the image IDs of the letters are mapped on an ASCII base;
 *  <li>Copy over the splash settings in elim.cfg and adjust where needed.
 * </ul>
 * <ul><b>Using this class</b>
 *  <li>Create and initialize this class only once, through its constructor {@link #ElimLeaderBoard(BotAction, String, String)};
 *  <li>If you ignore the above, make sure you do a {@link #die()} on this object first, to ensure the {@link PreparedStatement}s are closed and freed properly;
 *  <li>To reload the configuration, call upon {@link #reloadConfig()}, this will repopulate the ranking tables as well;
 *  <li>To update only the rankings, call upon {@link #updateCache(int)}, this will recreate the LVZ tables as well;
 *  <li>To display the splash screen(s), call upon {@link #showSplash(short, int)}. Use -1 to display to all the users at once;
 *  <li>When the bot is dying, call upon {@link #die()} of this class, to ensure proper closure of the {@link PreparedStatement}s.
 * </ul>
 * <p>
 * Note: This class uses rank numbers according to what is used in the real world.
 * For example: 1 - 10, and not 0 - 9.
 *  
 * @author Trancid
 *
 */
public class ElimLeaderBoard {
    private static final int[] OFFSET_RANK      = {-300,   30}; // X-coordinates first character of ranks, left side and right side.
    private static final int[] OFFSET_NAMES     = {-285,   64}; // X-coordinates first character of names, left side and right side.
    private static final int[] OFFSET_SCORES    = { -80,  270}; // X-coordinates first character of scores, left side and right side.
    private static final int[] OFFSET_Y         = {-150,  310}; // Y-coordinates of first row and optional final text row.
    private static final int[] OFFSET_TOP       = {-500, -350}; // Offset for top left corner of background (x, y).
    private static final int[] OFFSET_TEXT      = {-150,  310}; // Offset for optional text at bottom (x, y).
    private static final int[] SPACING          = {  10,   47}; // Horizontal and vertical spacing.
    
    private static final String ELIM_QUERY = "SELECT fnRank, fcName, fnAdjRating FROM tblElim__Player WHERE fnShip = ? AND fnRank >= 1 AND fnSeason = ? ORDER BY fnRank ASC LIMIT ?";
    private static final String BOTTOM_TEXT     = "Type \":Robo Ref:!disable\" to disable this.";
    
    private BotAction m_botAction;                      // Original BotAction, used for several methods.
    private PreparedStatement m_updateStats;            // The PreparedStatement used to get the required information.
    private TreeMap<Integer, ElimRanking> m_rankings;   // Holds the ranking information.
    private ArrayList<SplashScreen> m_splashScreens;    // Holds all the splash screen info that needs to be shown.
    private ArrayList<Short> m_playersActive;           // Tracks to which player(s) the splash screens are currently shown.
    
    private LvzManager m_lvzMgrScore = new LvzManager();
    private LvzManager m_lvzMgrDisp1 = new LvzManager();
    private LvzManager m_lvzMgrDisp2 = new LvzManager();
    
    private String m_db;                                // SQL Database name.
    private String m_connectionID;                      // SQL Connection ID name.
    private int m_displayTime;                          // Display length of each splash window in milliseconds.
    private int m_displayCount;                         // Amount of displays to be shown to a player.
    
    private int currentSeason;
    
    /**
     * Main constructor.<br>
     * If you intent to create a new instance of this object in place of an old object, please run the {@link #die()} method
     * of the old object first, to ensure proper closing of the {@link PreparedStatement}s.
     * @param ba The original {@link BotAction}.
     * @param db A valid SQL Database name.
     * @param connectionID A valid, unique connection ID name.
     */
    public ElimLeaderBoard(BotAction ba, String db, String connectionID) {
        m_rankings = new TreeMap<Integer, ElimRanking>();
        m_botAction = ba;
        m_db = db;
        m_connectionID = connectionID;
        m_updateStats = ba.createPreparedStatement(db, connectionID, ELIM_QUERY);
        
        init();
       
    }
    
    /**
     * Populates the settings based on the bot's configuration file.
     */
    private void init() {
        String temp = "";
        BotSettings cfg = m_botAction.getBotSettings();
        m_displayTime = cfg.getInt("DisplayTime");          // Display time in milliseconds.
        m_displayCount = cfg.getInt("DisplayCount");        // Amount of different displays to display.
        
        currentSeason = cfg.getInt("CurrentSeason");
        
        m_splashScreens = new ArrayList<SplashScreen>();    // Place-holder for the different splash screens.
        
        // Populate the array list with splash screens and construct the main ordering.
        for(int i = 0; i < m_displayCount; i++) {
            temp = cfg.getString("Display" + (i + 1));
            if(temp != null && !temp.isEmpty()) {
                m_splashScreens.add(new SplashScreen(temp));
            }
        }
        
        m_playersActive = new ArrayList<Short>();           // List to ensure the same splash isn't shown twice at the same time to a player.
    }
    
    /**
     * Public method to allow reloading of the configuration data.
     */
    public void reloadConfig() {
        init();
    }
    
    /**
     * Contacts the SQL database to update the cached information for the specific ship type.
     * @param shipType Ship-type according to in-game numbering.
     * @see ShipType
     * @see Tools.Ship
     */
    public void updateCache(int shipType) {
        for(SplashScreen splashScreen : m_splashScreens) {
            splashScreen.update(shipType);
        }
    }
    
    public void showSplash(short playerID, int mode) {
        if(playerID != -1 && m_playersActive.contains(new Short(playerID))) {
            m_botAction.sendPrivateMessage(playerID, "Request ignored; I'm already showing you the splash screen.");
            return;
        }
        if(m_playersActive.contains(new Short((short) -1))) {
            m_botAction.sendPrivateMessage(playerID, "Request ignored; Currently showing splash screen to everyone. Please try again in a few moments.");
        }
        ShowSplash ttShowSplash = new ShowSplash(playerID, mode);
        try {
            m_botAction.scheduleTask(ttShowSplash, 0, m_displayTime + 100);
        } catch (IllegalStateException e) {}
    }
    
    public Set<Integer> getKeySet() {
        return m_rankings.keySet();
    }
    
    public boolean contains(int shipType) {
        return m_rankings.containsKey(shipType);
    }
    
    public ElimRanking getRanking(int shipType) {
        return m_rankings.get(shipType);
    }
    
    public void die() {
        m_botAction.closePreparedStatement(m_db, m_connectionID, m_updateStats);
        try {
            m_botAction.cancelTasks();
        } catch (IllegalStateException ise) {
            Tools.printStackTrace(ise);
        }
    }
    
    public class SplashScreen {
        private ArrayList<Integer> m_shipCount = new ArrayList<Integer>((Collections.nCopies(11, 0)));

        private int[][] m_order = new int[2][10];
        

        public SplashScreen(String format) {
            String[] splitFormat = format.split(";");
            
            switch(splitFormat.length) {
            default:
            case 2:
                generateShipTable(1, splitFormat[1]);
            case 1:
                generateShipTable(0, splitFormat[0]);
                break;
            case 0:
                return;
            }
            
            for(Integer i = 1; i <= 10; i++) {
                m_rankings.put(i, new ElimRanking(i));
                updateCache(i);
            }
            
            generateLVZTable();
        }
        
        public void update(Integer shipType) {
            m_rankings.put(shipType, new ElimRanking(shipType));
            updateCache(shipType);
            
            generateLVZTable();
        }
        
        private void generateShipTable(int leftRightIndex, String data) {
            if(data == null || data.isEmpty())
                return;
            
            String[] pairs = data.split(",");
            int index = 0;
            
            for(int i = 0; i < pairs.length; i++) {
                String[] item = data.split(":");
                
                if(item.length != 2)
                    continue;
                
                int shipType = Integer.parseInt(item[0]);
                int cnt = Integer.parseInt(item[1]);
                m_shipCount.set(shipType, m_shipCount.get(shipType) + cnt);
                
                for(;cnt > 0 && index < 10; cnt--, index++) {
                    m_order[leftRightIndex][index] = shipType;
                }
            }
        }
        
        public void generateLVZTable() {
            short objID = 9998;
            int xCoord = OFFSET_TOP[0];
            int yCoord = OFFSET_TOP[1];
            
            // Background stuff
            m_lvzMgrDisp1.getObjectSafely(objID);
            m_lvzMgrDisp1.setImageToChange(objID, 0);
            m_lvzMgrDisp1.setLocationToChange(objID, xCoord, yCoord);
            m_lvzMgrDisp1.setLocationTypeToChange(objID, CoordType.C, CoordType.C);
            m_lvzMgrDisp1.setLayerToChange(objID, Layer.AfterChat);
            
            m_lvzMgrDisp2.getObjectSafely(++objID);
            m_lvzMgrDisp2.setImageToChange(objID, 1);
            m_lvzMgrDisp2.setLocationToChange(objID, xCoord, yCoord);
            m_lvzMgrDisp2.setLocationTypeToChange(objID, CoordType.C, CoordType.C);
            m_lvzMgrDisp2.setLayerToChange(objID, Layer.AfterChat);
            
            // Rank, name and score.
            for(int i = 0; i < 2; i++) {
                int[] shipCount = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                objID = 0;
                xCoord = 0;
                yCoord = 0;
                for(int j = 0; j < 10; j++) {
                    Integer shipType = m_order[i][j];
                    if(shipType == Tools.Ship.SPECTATOR)
                        continue;
                    
                    if(m_rankings.get(shipType) == null)
                        continue;
                    
                    shipCount[shipType]++;
                    // Rank
                    String rank = Tools.rightString(Integer.toString(j + 1), 2, ' ');
                    objID = (short) (10000 + 1000 * i + 100 * j);
                    xCoord = OFFSET_RANK[i];
                    yCoord = OFFSET_Y[0] + SPACING[1] * j;
                    for(int k = 1; k < rank.length(); k++, objID++, xCoord += SPACING[0]) {
                        m_lvzMgrDisp2.getObjectSafely(objID);
                        m_lvzMgrDisp2.setImageToChange(objID, rank.charAt(k));
                        m_lvzMgrDisp2.setLocationToChange(objID, xCoord, yCoord);
                        m_lvzMgrDisp2.setLocationTypeToChange(objID, CoordType.C, CoordType.C);
                    }
                    
                    // Name
                    String name = Tools.formatString(m_rankings.get(shipType).getName(shipCount[shipType]), 19, " ");
                    objID = (short) (12000 + 1000 * i + 100 * j);
                    xCoord = OFFSET_NAMES[i];
                    yCoord = OFFSET_Y[0] + SPACING[1] * j;
                    for(int k = 0; k < name.length(); k++, objID++, xCoord += SPACING[0]) {
                        m_lvzMgrScore.getObjectSafely(objID);
                        m_lvzMgrScore.setImageToChange(objID, name.charAt(k));
                        m_lvzMgrScore.setLocationToChange(objID, xCoord, yCoord);
                        m_lvzMgrScore.setLocationTypeToChange(objID, CoordType.C, CoordType.C);
                    }
                    
                    // Points
                    if( m_rankings.get(shipType).getScore(shipCount[shipType]) != null ) {
                        String score = Tools.rightString(Integer.toString(m_rankings.get(shipType).getScore(shipCount[shipType])), 5, ' ');
                        objID = (short) (14000 + 1000 * i + 100 * j);
                        xCoord = OFFSET_SCORES[i];
                        yCoord = OFFSET_Y[0] + SPACING[1] * j;
                        for(int k = 0; k < score.length(); k++, objID++, xCoord += SPACING[0]) {
                            m_lvzMgrScore.getObjectSafely(objID);
                            m_lvzMgrScore.setImageToChange(objID, score.charAt(k));
                            m_lvzMgrScore.setLocationToChange(objID, xCoord, yCoord);
                            m_lvzMgrScore.setLocationTypeToChange(objID, CoordType.C, CoordType.C);
                        }
                    }
                }
            }
            
            // Optional text.
            String text = BOTTOM_TEXT;
            objID = 17000;
            xCoord = OFFSET_TEXT[0];
            yCoord = OFFSET_TEXT[1];
            for(int k = 0; k < text.length(); k++, objID++, xCoord += SPACING[0]) {
                m_lvzMgrScore.getObjectSafely(objID);
                m_lvzMgrScore.setImageToChange(objID, text.charAt(k));
                m_lvzMgrScore.setLocationToChange(objID, xCoord, yCoord);
                m_lvzMgrScore.setLocationTypeToChange(objID, CoordType.C, CoordType.C);                
            }

            
        }
        
        public void displaySplash(short playerID, int type) {
            Vector<Short> objects = new Vector<Short>();
            
            switch(type) {
            case 0:
            default:
                // Splash screen KrynetiX
                objects.addAll(m_lvzMgrDisp1.getQueue());
                m_botAction.manuallySetObjectModifications(playerID, m_lvzMgrDisp1.getObjectModifications());
                break;
            case 1:
                // Splash screen qan
                objects.addAll(m_lvzMgrDisp2.getQueue());
                m_botAction.manuallySetObjectModifications(playerID, m_lvzMgrDisp2.getObjectModifications());
                break;
            }
            
            // General data
            objects.addAll(m_lvzMgrScore.getQueue());
            m_botAction.manuallySetObjectModifications(playerID, m_lvzMgrScore.getObjectModifications());
            
            
            if(m_displayTime != 0) {
                RemoveSplash ttRemoveSplash = new RemoveSplash(playerID, objects);
                m_botAction.scheduleTask(ttRemoveSplash, m_displayTime);
            }
            
        }
        
        public void updateCache(int shipType) {
            if(m_updateStats == null) {
                m_botAction.sendChatMessage(1, "SQL Error, please contact a dev or restart me.");
                return;
            }
            
            if(!m_rankings.containsKey(shipType) || m_shipCount.get(shipType) <= 0 || shipType == Tools.Ship.SPECTATOR)
                return;
            
            try {
                m_updateStats.clearParameters();
                m_updateStats.setInt(1, shipType);
                m_updateStats.setInt(2, currentSeason);
                m_updateStats.setInt(3, m_shipCount.get(shipType));
                
                ResultSet rs = m_updateStats.executeQuery();
                
                m_rankings.get(shipType).updateList(rs);
                
                rs.close();
                
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
            
        }
        
        private class RemoveSplash extends TimerTask {
            private final int playerID;
            private final Vector<Short> objects;
            
            public RemoveSplash(short playerID, Vector<Short> objects) {
                this.playerID = playerID;
                
                this.objects = new Vector<Short>();
                this.objects.addAll(objects);

                m_botAction.setupObjects(this.playerID, this.objects, true);
                m_botAction.sendSetupObjectsForPlayer(this.playerID);
            }
            
            @Override
            public void run() {
                m_botAction.setupObjects(this.playerID, this.objects, false);
                m_botAction.sendSetupObjectsForPlayer(this.playerID);
            }
        }
    }
    
    public class ElimRanking {
        private Integer m_shipType;
        private ArrayList<String> m_nameList;
        private ArrayList<Integer> m_scoreList;
       
        /** Default ElimRanking constructor */
        public ElimRanking(int shipType) {
            m_shipType = shipType;
            m_nameList = new ArrayList<String>();
            m_scoreList = new ArrayList<Integer>();
        }
        
        /**
         * Clears all the internally kept lists.
         */
        public void clearRanks() {
            m_nameList.clear();
            m_scoreList.clear();
        }
        
        /**
         * @return the m_shipType
         */
        public Integer getShipType() {
            return m_shipType;
        }
        
        /**
         * Retrieves the name that is associated with a certain rank.
         * @param rank Number ranging from 1 to max.
         * @return Name associated with the rank, or null if the rank has no data associated with it.
         */
        public String getName(int rank) {
            if(m_nameList == null || rank < 1 || m_nameList.size() < rank)
                return null;
            
            // Convert rank being 1 - ... to 0 - ...-1
            rank--;
            
            return m_nameList.get(rank);
        }
        
        /**
         * Retrieves the score that is associated with a certain rank.
         * @param rank Number ranging from 1 to max.
         * @return Score associated with the rank, or null if the rank has no data associated with it.
         */
        public Integer getScore(int rank) {
            if(m_scoreList == null || rank < 1 || m_scoreList.size() < rank)
                return null;
            
            // Convert rank being 1 - ... to 0 - ...-1
            rank--;
            
            return m_scoreList.get(rank);
        }
        
        public void updateList(ResultSet rs) throws SQLException {
            ArrayList<String> newNames = new ArrayList<String>();
            ArrayList<Integer> newScores = new ArrayList<Integer>();
            
            while(rs.next()) {
                newNames.add(rs.getString("fcName"));
                newScores.add(rs.getInt("fnRating"));
            }
            
            if(!newNames.isEmpty() && !newScores.isEmpty()) {
                m_nameList = newNames;
                m_scoreList = newScores;
            }
        }
        
    }
    
    public class ShowSplash extends TimerTask {
        int i = 0;
        short playerID;
        int type;
        
        public ShowSplash(short playerID, int type) {
            this.playerID = playerID;
            this.type = type;
            m_playersActive.add(new Short(playerID));
        }
        
        @Override
        public void run() {
            if(i < m_displayCount) {
                m_splashScreens.get(i).displaySplash(playerID, type);
                i++;
            } else {
                m_playersActive.remove(new Short(playerID));
                this.cancel();
            }
            
            
        }
    }

}