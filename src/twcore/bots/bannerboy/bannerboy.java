package twcore.bots.bannerboy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaList;
import twcore.core.events.KotHReset;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerBanner;
import twcore.core.stats.DBPlayerData;
import twcore.core.util.Tools;

/**
    A bot designed to collect banners from players and place them in a database.

    @author 2dragons
*/
public class bannerboy extends SubspaceBot {

    //mySQL database to use
    private String m_sqlHost = "website";

    //Keep track of the time since last personal banner change
    private long m_lastBannerSet = 0;

    //queue for banner checks
    private Vector<BannerCheck> m_toCheck;

    //Boolean to track if 'talking' mode is on
    private boolean m_talk;

    // PreparedStatements
    PreparedStatement psGetBannerID;
    PreparedStatement psSaveBanner;
    PreparedStatement psSeenBanner;
    PreparedStatement psCheckSeen;
    PreparedStatement psBannerBannedUpdate;

    public bannerboy(BotAction botAction) {
        super(botAction);

        EventRequester req = botAction.getEventRequester();
        req.request(EventRequester.PLAYER_BANNER);
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_LIST);
        req.request(EventRequester.KOTH_RESET);

        m_toCheck = new Vector<BannerCheck>();
        m_talk = false;
        psGetBannerID = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "SELECT fnBannerID, fnBanned FROM tblBanner WHERE fcBanner = ? LIMIT 0,1");
        psSaveBanner = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "INSERT INTO tblBanner (fnUserID, fcBanner, fdDateFound) VALUES ( ? , ? , NOW())");
        psSeenBanner = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "INSERT INTO tblWore (fnUserId, fnBannerId) VALUES ( ? , ? )");
        psCheckSeen = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "SELECT fnUserId FROM tblWore WHERE fnUserID = ? AND fnBannerID = ? LIMIT 0,1");
        psBannerBannedUpdate = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "UPDATE tblBanner SET fnBanned = ? , fnBannedByID = ? WHERE fnBannerID = ?");

    }

    public void handleEvent(PlayerBanner event) {

        String player = m_botAction.getPlayerName(event.getPlayerID());

        //Ignore banner changes by bot
        if (player == m_botAction.getBotName())
            return;

        byte[] banner = event.getBanner();

        m_toCheck.add(new BannerCheck(player, banner));
    }

    /**
        Based on an arena list, select a new arena to travel to.
    */
    public void handleEvent(ArenaList event) {

        String currentPick = "#robopark";
        // If arena name starts with # or less than X players are in it, pick a random arena again.
        // Every 10 iterations, X is reduced by 1, so that we settle for smaller and smaller iterations.
        // Shame on 2d for creating this kind of loop!  Took 5 minutes to fix! -qan
        int iterations = 0;
        int idealSize = 10;

        while ((currentPick.startsWith("#") || currentPick.equals("") || currentPick.equalsIgnoreCase("DeathStarBattle")
                || currentPick.equalsIgnoreCase("ExtremeGames") || currentPick.equalsIgnoreCase("ChaosZone")
                || currentPick.equalsIgnoreCase("Devastation") || currentPick.equalsIgnoreCase("MetalGear") || event.getSizeOfArena(currentPick) < idealSize)
                && idealSize > 0) {
            String[] arenaNames = event.getArenaNames();
            int arenaIndex = (int) (Math.random() * arenaNames.length);
            currentPick = arenaNames[arenaIndex];
            iterations++;

            if (iterations % 10 == 0)
                idealSize--;
        }

        if (idealSize > 0 && !currentPick.equals(""))
            m_botAction.changeArena(currentPick);

    }

    public void handleDisconnect() {
        m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psCheckSeen);
        m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psGetBannerID);
        m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psSaveBanner);
        m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psSeenBanner);
        m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psBannerBannedUpdate);
        m_botAction.cancelTasks();
    }

    private boolean bannerExists(BannerCheck bc) {
        try {
            psGetBannerID.setString(1, getBannerString(bc.getBanner()));
            ResultSet rs = psGetBannerID.executeQuery();

            if (rs.next()) {
                //Report banner if banner is a banned banner. <-Tongue Twister?
                if (rs.getInt("fnBanned") == 1) {
                    m_botAction.sendCheaterMessage("I see " + bc.getPlayer() + " " + "wearing a banned banner. (#" + rs.getInt("fnBannerID") + ")");
                }

                return true;
            } else
                return false;
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
            return true;
        }
    }

    private void saveBanner(String player, byte[] b) {
        try {
            psSaveBanner.setInt(1, getPlayerID(player));
            psSaveBanner.setString(2, getBannerString(b));
            psSaveBanner.execute();
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    private int getPlayerID(String player) {
        DBPlayerData dbPlayer = new DBPlayerData(m_botAction, m_sqlHost, player, true);
        return dbPlayer.getUserID();
    }

    private int getBannerID(byte[] b) {
        int id = -1;

        try {
            psGetBannerID.setString(1, getBannerString(b));
            ResultSet rs = psGetBannerID.executeQuery();

            if (rs.next()) {
                id = rs.getInt("fnBannerID");
            }

            return id;
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
            return -1;
        }
    }

    /*  private void createPlayer( String player ) {

        player = Tools.addSlashesToString( player );
        try {
            String query = "INSERT INTO tblUser (fcUserName) VALUES ('"+player+"')";
                         m_botAction.SQLQueryAndClose( m_sqlHost, query );
        } catch (Exception e) {
            Tools.printStackTrace( e );
        }
        }*/

    private void markSeen(String player, byte[] banner) {

        int bannerId = getBannerID(banner);
        int userId = getPlayerID(player);

        if (bannerId <= 0)
            return;

        if (alreadyMarked(userId, bannerId))
            return;

        try {
            psSeenBanner.setInt(1, userId);
            psSeenBanner.setInt(2, bannerId);
            psSeenBanner.execute();
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
        }
    }

    private boolean alreadyMarked(int userId, int bannerId) {
        try {
            psCheckSeen.setInt(1, userId);
            psCheckSeen.setInt(2, bannerId);
            ResultSet rs = psCheckSeen.executeQuery();

            if (rs.next())
                return true;
            else
                return false;
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
            return true;
        }
    }

    private String getBannerString(byte[] banner) {

        String b = "";

        for (int i = 0; i < 96; i++) {
            if ((banner[i] & 0xf0) == 0) {
                b += 0 + Integer.toHexString(banner[i] & 0xFF);
            } else {
                b += Integer.toHexString(banner[i] & 0xFF);
            }
        }

        return b;
    }

    private byte[] getBannerFromDatabase(int bannerID) {
        String bannerString = null;
        byte[] banner = new byte[96];

        try {
            ResultSet rs = m_botAction.SQLQuery(m_sqlHost, "SELECT fcBanner FROM tblBanner WHERE fnBannerID = '" + bannerID + "' LIMIT 0,1");

            if (rs != null && rs.next()) {
                bannerString = rs.getString("fcBanner");
            } else {
                banner = null;
                return banner;
            }

            rs.close();
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
        }

        for (int i = 0; i < 190; i += 2) {
            banner[i / 2] = (byte) Integer.parseInt(bannerString.substring(i, i + 2), 16);
        }

        return banner;
    }

    private boolean banBanner(int bannerID, int userIDOfBanningModerator) {
        try {
            psBannerBannedUpdate.setInt(1, 1);
            psBannerBannedUpdate.setInt(2, userIDOfBanningModerator);
            psBannerBannedUpdate.setInt(3, bannerID);
            int rowCount = psBannerBannedUpdate.executeUpdate();

            if (rowCount == 1)
                return true;
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
        }

        return false;
    }

    private boolean unbanBanner(int bannerID, int userIDOfBanningModerator) {
        try {
            psBannerBannedUpdate.setInt(1, 0);
            psBannerBannedUpdate.setInt(2, userIDOfBanningModerator);
            psBannerBannedUpdate.setInt(3, bannerID);
            int rowCount = psBannerBannedUpdate.executeUpdate();

            if (rowCount == 1)
                return true;
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
        }

        return false;
    }

    private void listbannedBanners(String player) {
        try {
            ResultSet rs = m_botAction.SQLQuery(m_sqlHost, "SELECT tblUser.fcUserName, tblBanner.fnBannerID " + "FROM tblUser INNER JOIN tblBanner "
                                                + "ON tblUser.fnUserID = tblBanner.fnBannedByID " + "WHERE tblBanner.fnBanned = '1'");

            if (!rs.isBeforeFirst()) {
                m_botAction.sendSmartPrivateMessage(player, "No banners are currently banned.");
                rs.close();
                return;
            }

            while (rs.next()) {
                m_botAction.sendSmartPrivateMessage(player, rs.getInt(2) + " - banned by " + rs.getString(1));
            }

            rs.close();
        } catch (SQLException sqle) {
            Tools.printStackTrace(sqle);
        }
    }

    public void handleEvent(Message event) {
        if (event.getMessageType() != Message.PRIVATE_MESSAGE && event.getMessageType() != Message.REMOTE_PRIVATE_MESSAGE)
            return;

        String player = m_botAction.getPlayerName(event.getPlayerID());
        String message = event.getMessage();

        if (event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
            player = event.getMessager();

        if (m_botAction.getOperatorList().isSmod(player)) {
            if (message.startsWith("!help")) {
                String[] help = { "Available commands:", " !go <arena>                 - Makes the bot move to <arena>",
                                  " !say <playername>:<message> - Sends PM with <message> to <playername>",
                                  " !tsay <message>             - Sends team <message>",
                                  " !talk                       - Toggles if the bot talks to the player",
                                  "                               when copying/wearing his banner",
                                  " !wearbanner <bannerID>      - Wears banner based on bannerID",
                                  " !banbanner  <bannerID>      - Removes banner from website based on bannerID",
                                  " !unbanbanner <bannerID>     - Unbans a banner", " !listbannedbanners          - Lists bannerIDs that are banned",
                                  " !default                    - Resets to default banner, use when bot is wearing a naughty banner",
                                  " !die                        - Disconnects the bot"
                                };
                m_botAction.smartPrivateMessageSpam(player, help);
            } else

                if (message.startsWith("!die")) {
                    this.handleDisconnect();
                    m_botAction.die("Disconnected by " + player);
                } else

                    if (message.startsWith("!go ")) {
                        String arena = message.substring(4);

                        if (arena.length() > 0) {
                            m_botAction.sendSmartPrivateMessage(player, "Going to " + arena);
                            m_botAction.joinArena(arena);
                        }
                    } else

                        if (message.startsWith("!say ")) {
                            if (message.indexOf(':') == -1)
                                return;

                            //!say <>:<>
                            //012345
                            String pieces[] = message.split(":");

                            if (pieces.length == 2) {
                                m_botAction.sendSmartPrivateMessage(pieces[0].substring(5), pieces[1]);
                                m_botAction.sendSmartPrivateMessage(player, "PM send to " + pieces[0].substring(5));
                            }
                        } else

                            if (message.startsWith("!tsay ")) {
                                if (message.substring(6).length() > 0)
                                    m_botAction.sendTeamMessage(message.substring(6));
                            } else

                                if (message.startsWith("!talk")) {
                                    m_talk = !m_talk;

                                    if (m_talk)
                                        m_botAction.sendSmartPrivateMessage(player, "Talk on");
                                    else
                                        m_botAction.sendSmartPrivateMessage(player, "Talk off");
                                } else

                                    if (message.startsWith("!default")) {
                                        // Don't ask...
                                        m_botAction.setBanner(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0xc3, (byte) 0xa5, (byte) 0x0b, (byte) 0xaa,
                                                                           (byte) 0xa8, (byte) 0xcd, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0x0c, (byte) 0xa5,
                                                                           (byte) 0x36, (byte) 0x00, (byte) 0xe9, (byte) 0x98, (byte) 0xac, (byte) 0xca, (byte) 0x00, (byte) 0x00, (byte) 0x36,
                                                                           (byte) 0x9d, (byte) 0xb1, (byte) 0x36, (byte) 0xca, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xca, (byte) 0xb5,
                                                                           (byte) 0xca, (byte) 0x00, (byte) 0xa8, (byte) 0xb5, (byte) 0x00, (byte) 0x1e, (byte) 0xe9, (byte) 0x00, (byte) 0x00,
                                                                           (byte) 0x00, (byte) 0x00, (byte) 0xca, (byte) 0xb4, (byte) 0x00, (byte) 0xac, (byte) 0x00, (byte) 0x00, (byte) 0xca,
                                                                           (byte) 0xe9, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xca, (byte) 0x00, (byte) 0xb2,
                                                                           (byte) 0x00, (byte) 0xe9, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                                           (byte) 0xca, (byte) 0x00, (byte) 0xb3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                                           (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                                           (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                                                                         });
                                        m_botAction.sendSmartPrivateMessage(player, "It has been done.");
                                    } else

                                        if (message.startsWith("!wearbanner ")) {
                                            int bannerID = 0;

                                            try {
                                                bannerID = Integer.parseInt(message.substring(12));
                                            } catch (NumberFormatException e) {
                                                m_botAction.sendSmartPrivateMessage(player, "Please input a number for the banner ID.");
                                                return;
                                            }

                                            byte[] banner = getBannerFromDatabase(bannerID);

                                            if (banner != null) {
                                                m_botAction.setBanner(banner);
                                                m_botAction.sendSmartPrivateMessage(player, "Banner set.");
                                            } else {
                                                m_botAction.sendSmartPrivateMessage(player, "Banner does not exist in database.");
                                            }
                                        } else

                                            if (message.startsWith("!banbanner ")) {

                                                int bannerID = 0;

                                                try {
                                                    bannerID = Integer.parseInt(message.substring(11));
                                                } catch (NumberFormatException e) {
                                                    m_botAction.sendSmartPrivateMessage(player, "Please input a number for the banner ID.");
                                                    return;
                                                }

                                                if (bannerID != 0) {
                                                    boolean successfulBan = banBanner(bannerID, getPlayerID(player));

                                                    if (successfulBan)
                                                        m_botAction.sendSmartPrivateMessage(player, "Banner removed/banned successfully.");
                                                    else
                                                        m_botAction.sendSmartPrivateMessage(player, "Banner not found/error.");
                                                }

                                            } else if (message.startsWith("!unbanbanner ")) {
                                                int bannerID = 0;

                                                try {
                                                    bannerID = Integer.parseInt(message.substring(13));
                                                } catch (NumberFormatException e) {
                                                    m_botAction.sendSmartPrivateMessage(player, "Please input a number for the banner ID.");
                                                    return;
                                                }

                                                if (bannerID != 0) {
                                                    boolean successfulLift = unbanBanner(bannerID, getPlayerID(player));

                                                    if (successfulLift)
                                                        m_botAction.sendSmartPrivateMessage(player, "Banner ban lifted successfully.");
                                                    else
                                                        m_botAction.sendSmartPrivateMessage(player, "Banner not found/error.");
                                                }
                                            } else if (message.startsWith("!listbannedbanners")) {
                                                listbannedBanners(player);
                                            }

        } else if (!m_botAction.getOperatorList().isSmod(player) && message.equalsIgnoreCase("!help")) {
            String[] helpmsg = { "Hello, I'm a bot that collects banner information from players. I store all of the ",
                                 "information I find at http://www.trenchwars.org/SSBE. If you would like me to store",
                                 "your banner on the site simply stay in the same arena as me and I'll be happy to do", "so. Have fun in Trench Wars!"
                               };
            m_botAction.smartPrivateMessageSpam(player, helpmsg);
        } else if (!m_botAction.getOperatorList().isBotExact(player)) {
            m_botAction.sendChatMessage(player + "> " + message);
        }
    }

    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botAction.getGeneralSettings().getString("Arena"));
        m_botAction.sendUnfilteredPublicMessage("?chat=" + m_botAction.getGeneralSettings().getString("Chat Name"));

        if (psGetBannerID == null || psSaveBanner == null || psSeenBanner == null || psCheckSeen == null || psBannerBannedUpdate == null) {
            //Something went wrong, we can't continue
            m_botAction.sendChatMessage("Error while creating PreparedStatements, disconnecting...");
            handleDisconnect();
            m_botAction.die("Error while creating PreparedStatements");
            return;
        }

        TimerTask changeArenas = new TimerTask() {
            public void run() {
                m_botAction.requestArenaList();
            }
        };
        m_botAction.scheduleTaskAtFixedRate(changeArenas, 60000, 240000);

        //Every 3 seconds check another banner
        TimerTask checkBanners = new TimerTask() {
            public void run() {

                if (m_toCheck.size() <= 0)
                    return;

                BannerCheck bc = m_toCheck.remove(0);
                byte banner[] = bc.getBanner();
                String player = bc.getPlayer();

                //If banner isn't in db save it
                if (!bannerExists(bc)) {
                    saveBanner(player, banner);

                    // And start wearing it if has been more than 5 minutes since the last banner got set.
                    if (System.currentTimeMillis() - m_lastBannerSet >= 5 * Tools.TimeInMillis.MINUTE) {
                        m_botAction.setBanner(bc.getBanner());
                        m_lastBannerSet = System.currentTimeMillis();

                        if (m_talk)
                            m_botAction.sendSmartPrivateMessage(bc.getPlayer(), "Hope you don't mind if I wear your banner.  Looks good on me, doesn't it?  See http://www.trenchwars.org/ssbe/ to see what I'll do with it.");
                    }
                }

                markSeen(player, banner);
            }
        };
        m_botAction.scheduleTaskAtFixedRate(checkBanners, 2000, 3000);
    }

    /**
        Handles restarting of the KOTH game

        @param event is the event to handle.
    */
    public void handleEvent(KotHReset event) {
        if (event.isEnabled() && event.getPlayerID() == -1) {
            // Make the bot ignore the KOTH game (send that he's out immediately after restarting the game)
            m_botAction.endKOTH();
        }
    }
}

class BannerCheck {

    private String pName;
    private byte[] banner;

    public BannerCheck(String player, byte[] b) {
        pName = player;
        banner = b;
    }

    public String getPlayer() {
        return pName;
    }

    public byte[] getBanner() {
        return banner;
    }
}
