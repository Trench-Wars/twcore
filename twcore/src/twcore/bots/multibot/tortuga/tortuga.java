package twcore.bots.multibot.tortuga;

import static twcore.core.EventRequester.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Vector;
import java.util.TimerTask;
import java.util.Comparator;
import java.util.Collections;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.OperatorList;

import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.*;
import twcore.core.game.Player;

/** 
 * Module specific to ?go tortuga -- a pirate-themed
 * zombie-style event w/ dueling arena.
 * 
 * Designed with expansion to other arenas in mind.
 * Perhaps a warzone, base, etc.
 * by alinea
 */
public class tortuga extends MultiModule {
    private static final int PRICE_DEFAULT = 1000;
    private static final double PRICE_INCREMENT = .10;
    private static final int HOUSEPRICE_DEFAULT = 10000;
    private static final int NAMECHANGE_PRICE = 1000;
    private static final double HOUSEPRICE_INCREMENT = 1.50;
    private static final int BOUNTY_COINS = 100;   // # of coins a player receives for killing another player
    private static final int TAX_COINS = 10;       // # of coins a player receives when a pirate is killed on their land
    private static final int SAVE_DELAY = 5;
    private static Boolean boolAnnounce = true;
    private static Boolean boolInTortuga = false;
    
    public HashMap<String,String> m_specials = new HashMap<String,String>();
    
    private TimerTask m_saveData;
    public HashMap<String,Pirate> m_players = new HashMap<String,Pirate>();
    public HashMap<Integer,Plot> m_plots = new HashMap<Integer,Plot>();
    public HashMap<Integer,House> m_houses = new HashMap<Integer,House>();
    public HashMap<String,ScoreSet> m_scores = new HashMap<String,ScoreSet>();
    public HashMap<String,Integer> m_freqscores = new HashMap<String,Integer>();

    public int[][] arrPlotIDs = new int[20][20];

    public String m_host = "";
    boolean isRunning = false;

    public int m_gameid = 0;
    public String              m_database;
    public CommandInterpreter  m_commandInterpreter;
    public BotSettings         m_botSettings;

    public void fatal(String errMsg) {
        Tools.printLog("FATAL: " + errMsg);
        if (m_host != "") {m_botAction.sendPrivateMessage(m_host, "FATAL: " + errMsg);}
        m_botAction.die();
    }

    public void logError(String errMsg) {
        Tools.printLog("ERROR: " + errMsg);
        if (m_host != "") {m_botAction.sendPrivateMessage(m_host, "ERROR: " + errMsg);}
    }

    public void logDebug(String errMsg) {
        Tools.printLog("DEBUG: " + errMsg);
        if (m_host != "") {m_botAction.sendPrivateMessage(m_host, "DEBUG: " + errMsg);}
    }

    public void init() {
        m_botSettings = moduleSettings;
        m_database = m_botSettings.getString("Database");

        m_botAction.setMessageLimit(12,false);
        m_botAction.setReliableKills(1);
        m_botAction.setPlayerPositionUpdating(400);

        m_commandInterpreter = new CommandInterpreter(m_botAction);

        loadPlots();
        loadHouses();
        loadSpecials();

        registerCommands();

        if (m_botAction.getArenaName().equalsIgnoreCase("tortuga") || m_botAction.getArenaName().equalsIgnoreCase("#tortuga")) {
            boolInTortuga = true;
        }
    
    }
    
    public void loadSpecials() {
        m_specials.put("H4","ISLAND");
        m_specials.put("F1","NWALL");
        m_specials.put("A5","WWALL");
        m_specials.put("B18","SWALL");
        m_specials.put("T1","EWALL");
        m_specials.put("A1","SKULL");
    }

    public void cmdOn(String name, String msg) {
        if (!m_botAction.getOperatorList().isER(name)) {return;}
        if (isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga is currently activated.  Use !shutdown to shut it down."); return;}
        m_host = name;
        startGame();
    }
    
    public void cmdShutdown(String name, String msg) {
        if (!m_botAction.getOperatorList().isER(name)) {return;}
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga is not activated."); return;}
        endGame();
        m_botAction.sendPrivateMessage(name,"Tortuga successfully de-activated.");
    }

    public void startGame() {
        // in case of a crash, make sure to merge previous games data (from tblPirateStats)
        // with the overall player data in tblPiratePlayers
        mergeData();

        m_gameid = createGame();
        isRunning = true;

        m_botAction.showObject(1);
        m_botAction.resetFlagGame();
        m_botAction.sendArenaMessage("Tortuga has been activated!",17);

        m_saveData = new TimerTask() {public void run() {saveData();}};
        m_botAction.scheduleTask(m_saveData,SAVE_DELAY*60000,SAVE_DELAY*60000);
    }
    
    public void endGame() {
        m_botAction.hideObject(1);

        if (isRunning) {
            m_saveData.cancel();
            isRunning = false;
            saveData();
            mergeData();
            m_players.clear();
            m_scores.clear();
            m_freqscores.clear();
            m_host = "";
            m_gameid = 0;
            m_botAction.sendArenaMessage("Tortuga has been shut down.",14);
        }
    }

    public void cancel() {
        endGame();
    }
    
    public void mergeData() {
        try {
            int playerID, entryID, newCoins;

            ResultSet r = m_botAction.SQLQuery(m_database,"SELECT fnEntryID,fnPlayerID,fnFoundCoins,fnBountyCoins,fnTaxCoins,fnOtherCoins from tblPirateStats where fnMerged = 0 order by fnEntryID");
            if (r != null) {
                while (r.next()) {
                    playerID = r.getInt("fnPlayerID");
                    entryID = r.getInt("fnEntryID");
                    newCoins = r.getInt("fnFoundCoins") + r.getInt("fnBountyCoins") + r.getInt("fnTaxCoins") + r.getInt("fnOtherCoins");

                    try {
                        m_botAction.SQLQueryAndClose(m_database,"UPDATE tblPiratePlayers set fnCoins=fnCoins + " + newCoins + " where fnPlayerID = " + playerID);
                    } catch (SQLException e) {fatal("Error updating playerID " + playerID + " with " + newCoins + " coins: " + e.getMessage());}

                    try {
                        m_botAction.SQLQueryAndClose(m_database,"UPDATE tblPirateStats set fnMerged = 1 where fnEntryID = " + entryID);
                    } catch (SQLException e) {fatal("Error updating fnMerged to 1 for entryID " + entryID + ": " + e.getMessage());}
                }
            }
            m_botAction.SQLClose(r);
        } catch (SQLException e) {fatal("Error reading player stats [ERR: " + e.getMessage() + "]");}
    }

    public void saveData() {
        int playerCount = 0;
        int plotCount = 0;
        int houseCount = 0;
        long startTime = System.currentTimeMillis();
        for (Pirate p : m_players.values()) {p.saveToDB(); playerCount++;}
        for (Plot p : m_plots.values()) {if (p.saveToDB()) {plotCount++;}}
        for (House p : m_houses.values()) {if (p.saveToDB()) {houseCount++;}}
        m_botAction.sendArenaMessage("AUTOSAVE: Saved " + playerCount + " players, " + plotCount + " plots, " + houseCount + " houses in " + (System.currentTimeMillis() - startTime) + " ms.");
    }
    
    public String rPadInt(Integer num, Integer len) {
        return Tools.rightString(Integer.toString(num),len);
    }

    public void cmdReset(String name, String msg) {
        m_scores.clear();
        m_freqscores.clear();
        for (String strPlayer : m_players.keySet()) {
            m_scores.put(strPlayer,new ScoreSet(strPlayer));
        }
        m_botAction.sendPrivateMessage(name,"Scores have been reset.");
    }

    public static float roundto(float oriDbl, int decplaces) {
        float p = (float)Math.pow(10,decplaces);
        return (float) Math.round(oriDbl * p) / p;
    }

    public void cmdTop(String name, String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Scores are not kept while Tortuga is shutdown."); return;}

        int most_freq0Kills = 0;
        int most_freq1Kills = 0;
        int most_kills = 0;
        int most_flagTouches = 0;
        int most_flagCaptures = 0;
        int most_foundCoins = 0;
        int most_bountyCoins = 0;
        int most_taxCoins = 0;
        int most_deaths = 0;
        float best_ratio = 0.0f;
        float curRatio = 0;
        
        String wfreq0Kills = "";
        String wfreq1Kills = "";
        String wkills = "";
        String wflagTouches = "";
        String wflagCaptures = "";
        String wfoundCoins = "";
        String wbountyCoins = "";
        String wtaxCoins = "";
        String wdeaths = "";
        String wratio = "";

        // cycle through all players to find best scores
        for (ScoreSet ss : m_scores.values()) {
            if (ss.freq0Kills > most_freq0Kills) {most_freq0Kills = ss.freq0Kills;}
            if (ss.freq1Kills > most_freq1Kills) {most_freq1Kills = ss.freq1Kills;}
            if (ss.kills > most_kills) {most_kills = ss.kills;}
            if (ss.flagTouches > most_flagTouches) {most_flagTouches = ss.flagTouches;}
            if (ss.flagCaptures > most_flagCaptures) {most_flagCaptures = ss.flagCaptures;}
            if (ss.foundCoins > most_foundCoins) {most_foundCoins = ss.foundCoins;}
            if (ss.bountyCoins > most_bountyCoins) {most_bountyCoins = ss.bountyCoins;}
            if (ss.taxCoins > most_taxCoins) {most_taxCoins = ss.taxCoins;}
            if (ss.deaths > most_deaths) {most_deaths = ss.deaths;}
            if (ss.deaths > 0) {
                if (roundto((ss.freq0Kills + ss.freq1Kills) / ss.deaths,2) > best_ratio) {best_ratio = curRatio;}
            }
        }
        
        if (!m_freqscores.isEmpty()) {
            String wfreq = "freq 1";
            if (m_freqscores.get("freq0FlagTime") > m_freqscores.get("freq1FlagTime")) {wfreq = "freq 0";}
            m_botAction.sendPrivateMessage(name,"MOST FLAG TIME           " + wfreq);
        }

        // cycle through all players to find all players with the best scores (this captures ties)
        for (String strPlayer : m_scores.keySet()) {
            if (most_freq0Kills > 0 && m_scores.get(strPlayer).freq0Kills == most_freq0Kills) {wfreq0Kills += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_freq1Kills > 0 && m_scores.get(strPlayer).freq1Kills == most_freq1Kills) {wfreq1Kills += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_kills > 0 && m_scores.get(strPlayer).kills == most_kills) {wkills += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_flagTouches > 0 && m_scores.get(strPlayer).flagTouches == most_flagTouches) {wflagTouches += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_flagCaptures > 0 && m_scores.get(strPlayer).flagCaptures == most_flagCaptures) {wflagCaptures += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_foundCoins > 0 && m_scores.get(strPlayer).foundCoins == most_foundCoins) {wfoundCoins += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_bountyCoins > 0 && m_scores.get(strPlayer).bountyCoins == most_bountyCoins) {wbountyCoins += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_taxCoins > 0 && m_scores.get(strPlayer).taxCoins == most_taxCoins) {wtaxCoins += ", " + m_players.get(strPlayer).getPirateName();}
            if (most_deaths > 0 && m_scores.get(strPlayer).deaths == most_deaths) {wdeaths += ", " + m_players.get(strPlayer).getPirateName();}
            if (best_ratio > 0.0f && m_scores.get(strPlayer).deaths > 0) {
                if (roundto((m_scores.get(strPlayer).freq0Kills + m_scores.get(strPlayer).freq1Kills) / m_scores.get(strPlayer).deaths,2) == best_ratio) {wratio += ", " + strPlayer;}
            }
        }
        
        // Announce high scores
        if (wfreq0Kills.length() > 0) {m_botAction.sendArenaMessage(  "MOST KILLS (even freq) : " + rPadInt(most_freq0Kills,6)         + "  " + wfreq0Kills.substring(2));}
        if (wfreq1Kills.length() > 0) {m_botAction.sendArenaMessage(  "MOST KILLS (odd freq)  : " + rPadInt(most_freq1Kills,6)         + "  " + wfreq1Kills.substring(2));}
        if (wkills.length() > 0) {m_botAction.sendArenaMessage(       "MOST KILLS (overall)   : " + rPadInt(most_kills,6)              + "  " + wkills.substring(2));}
        if (wflagTouches.length() > 0) {m_botAction.sendArenaMessage( "MOST FLAG TOUCHES      : " + rPadInt(most_flagTouches,6)        + "  " + wflagTouches.substring(2));}
        if (wflagCaptures.length() > 0) {m_botAction.sendArenaMessage("MOST FLAG CAPTURES     : " + rPadInt(most_flagCaptures,6)       + "  " + wflagCaptures.substring(2));}
        if (wfoundCoins.length() > 0) {m_botAction.sendArenaMessage(  "MOST COINS FOUND       : " + rPadInt(most_foundCoins,6)         + "  " + wfoundCoins.substring(2));}
        if (wbountyCoins.length() > 0) {m_botAction.sendArenaMessage( "MOST BOUNTY EARNED     : " + rPadInt(most_bountyCoins,6)        + "  " + wbountyCoins.substring(2));}
        if (wtaxCoins.length() > 0) {m_botAction.sendArenaMessage(    "MOST TAX COLLECTED     : " + rPadInt(most_taxCoins,6)           + "  " + wtaxCoins.substring(2));}
        if (wdeaths.length() > 0) {m_botAction.sendArenaMessage(      "MOST DEATHS            : " + rPadInt(most_deaths,6)             + "  " + wdeaths.substring(2));}
        if (wratio.length() > 0) {m_botAction.sendArenaMessage(       "BEST RATIO             : " + Tools.rightString(""+best_ratio,6) + "  " + wratio.substring(2));}

        if (wfreq0Kills == "" && wfreq1Kills == "" && wkills == "" && wflagTouches == "" && wflagCaptures == "" && wfoundCoins == "" && wbountyCoins == "" && wtaxCoins == "" && wdeaths == "" && wratio == "") {
            m_botAction.sendPrivateMessage(name,"There are no winning scores to display.");
        }
    }

    public void cmdKillmsgs(String name, String msg) {
        if (boolAnnounce) {
            m_botAction.sendPrivateMessage(name,"Kill messages are OFF.");
            boolAnnounce = false;
        } else {
            boolAnnounce = true;
            m_botAction.sendPrivateMessage(name,"Kill messages are ON.");
        }
    }
    
    public void cmdRecords(String name,String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}
        Pirate p = m_players.get(name);
        if (p == null) {
            loadOrCreatePlayer(name,true);
            p = m_players.get(name);
        }
        Vector<BankRecord> m_records = p.getRecords();
        if (m_records.size() == 0) {
            m_botAction.sendPrivateMessage(name,"RECORDS:  There are no bank records on file.");
        } else {
            BankRecord b;
            int startAt = m_records.size() - 10;
            if (startAt < 0) {startAt = 0;}
            Iterator<BankRecord> li = m_records.listIterator(startAt);
            m_botAction.sendPrivateMessage(name,"RECORDS:  The last 10 bank records on file for you:");
            m_botAction.sendPrivateMessage(name,"|        DATE  TYPE   AMOUNT   FOR");
            while (li.hasNext()) {
                b = li.next();
                m_botAction.sendPrivateMessage(name,"| " + Tools.rightString(b.getDate(),11) + " " + Tools.rightString(b.getType(),5) + " " + Tools.rightString(b.getAmount(),8) + "   " + b.getTrans());
            }
        }
    }
    
    public void cmdMyPlots(String name,String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}
        Pirate p = m_players.get(name);
        if (p == null) {
            loadOrCreatePlayer(name,true);
            p = m_players.get(name);
        }
        int plotID;
        List<Integer> arrPlots =  p.getPlots();
        Collections.sort(arrPlots);
        Iterator<Integer> li = arrPlots.listIterator();
        Plot cp;
        
        if (arrPlots.size() == 0) {
            m_botAction.sendPrivateMessage(name,"PLOTS: You own no plots.");
        } else {
            m_botAction.sendPrivateMessage(name,"PLOTS:  Your plots, their cost, the taxes they've earned today and total earned while under your control:");
            m_botAction.sendPrivateMessage(name,"|   ADDR   COST   TODAY   TOTAL");
            while (li.hasNext()) {
                cp = m_plots.get(li.next());
                m_botAction.sendPrivateMessage(name,"| " + Tools.rightString(cp.getCaption(),6) + " " + rPadInt(cp.getCoinsPaid(),6) + " " + rPadInt(cp.getNewEarnings(),7) + " " + rPadInt(cp.getEarnings(),7));
            }
        }
    }
    
    public void cmdMyHouses(String name,String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}
        Pirate p = m_players.get(name);
        if (p == null) {
            loadOrCreatePlayer(name,true);
            p = m_players.get(name);
        }
        int houseID;
        List<Integer> arrHouses =  p.getHouses();
        Iterator<Integer> li = arrHouses.listIterator();
        House ch;

        if (arrHouses.size() == 0) {
            m_botAction.sendPrivateMessage(name,"HOUSES: You own no houses.");
        } else {
            m_botAction.sendPrivateMessage(name,"HOUSES:  You own " + getPlural(arrHouses.size(),"house") + ":");
            m_botAction.sendPrivateMessage(name,"| ADDR     COST");
            while (li.hasNext()) {
                ch = m_houses.get(li.next());
                m_botAction.sendPrivateMessage(name,"|  " + ch.getHouseID() + " " + rPadInt(ch.getCoinsPaid(),8));
            }
        }
    }

    public void cmdListHouses(String name,String msg) {
        House[] houseList = m_houses.values().toArray(new House[m_houses.size()]);

        // order by price,houseid
        Comparator<House> compHouse = new Comparator<House>() {
            public int compare(House a, House b) {
                Integer priceA = a.getPrice();
                Integer priceB = b.getPrice();
                if (priceA < priceB) {return -1;}
                else if (priceA > priceB) {return 1;}
                else if (a.getHouseID() < b.getHouseID()) {return -1;}
                else {return 1;}
            };
        };
        
        Arrays.sort(houseList,compHouse);
        int x;
        House h1, h2;
        String strOut;
 
        if (msg.equals("high")) {
            m_botAction.sendPrivateMessage(name,"10 MOST EXPENSIVE HOUSES:");
            m_botAction.sendPrivateMessage(name,"| ADDR    PRICE OWNER                      ADDR    PRICE OWNER");
            int intTotal = m_houses.size();
            for (x = intTotal-1; x > intTotal-6; x--) {
                h1 = houseList[x];
                h2 = houseList[x-5];
                strOut = "|  " + h1.getHouseID() + " " + Tools.rightString(Integer.toString(h1.getPrice()),8) + " " + Tools.formatString(h1.getOwnerString(),25); 
                strOut += "   " + h2.getHouseID() + " " + Tools.rightString(Integer.toString(h2.getPrice()),8) + " " + h2.getOwnerString(); 
                m_botAction.sendPrivateMessage(name,strOut);
            }
        } else {
            m_botAction.sendPrivateMessage(name,"10 LEAST EXPENSIVE HOUSES:");
            m_botAction.sendPrivateMessage(name,"| ADDR    PRICE OWNER                      ADDR    PRICE OWNER");
            for (x = 0; x < 5; x++) {
                h1 = houseList[x];
                h2 = houseList[x+5];
                strOut = "|  " + h1.getHouseID() + " " + Tools.rightString(Integer.toString(h1.getPrice()),8) + " " + Tools.formatString(h1.getOwnerString(),25); 
                strOut += "   " + h2.getHouseID() + " " + Tools.rightString(Integer.toString(h2.getPrice()),8) + " " + h2.getOwnerString(); 
                m_botAction.sendPrivateMessage(name,strOut);
            }
        }
        m_botAction.sendPrivateMessage(name,"To view the 10 most expensive houses, use: !listhouses high");
    }

    public void cmdListPlots(String name,String msg) {
        Plot[] plotList = m_plots.values().toArray(new Plot[m_plots.size()]);

        // order by price, plot (alphabetically)
        Comparator<Plot> compPlot = new Comparator<Plot>() {
            public int compare(Plot a, Plot b) {
                Integer priceA = a.getPrice();
                Integer priceB = b.getPrice();
                Integer addrA = (a.getPlotX()*10)+a.getPlotY();
                Integer addrB = (b.getPlotX()*10)+b.getPlotY();

                if (priceA < priceB) {return -1;}
                else if (priceA > priceB) {return 1;}
                else if (addrA > addrB) {return -1;}
                else {return 1;}
            };
        };
        Arrays.sort(plotList,compPlot);
        int x;
        Plot p1, p2;
        String strOut;

        if (msg.equals("high")) {
            m_botAction.sendPrivateMessage(name,"10 MOST EXPENSIVE PLOTS:");
            m_botAction.sendPrivateMessage(name,"|   PLOT  PRICE OWNER                         PLOT  PRICE OWNER");
            int intTotal = m_plots.size();
            for (x = intTotal-1; x > intTotal-6; x--) {
                p1 = plotList[x];
                p2 = plotList[x-5];
                strOut = "| " + Tools.rightString(p1.getCaption(),6) + " " + Tools.rightString(Integer.toString(p1.getPrice()),6) + " " + Tools.formatString(p1.getOwnerString(),25); 
                strOut += "   " + Tools.rightString(p2.getCaption(),6) + " " + Tools.rightString(Integer.toString(p2.getPrice()),6) + " " + p2.getOwnerString();  
                m_botAction.sendPrivateMessage(name,strOut);
            }
        } else {
            m_botAction.sendPrivateMessage(name,"10 LEAST EXPENSIVE PLOTS:");
            m_botAction.sendPrivateMessage(name,"|   PLOT  PRICE OWNER                         PLOT  PRICE OWNER");
            for (x = 0; x < 5; x++) {
                p1 = plotList[x];
                p2 = plotList[x+5];
                strOut = "| " + Tools.rightString(p1.getCaption(),6) + " " + Tools.rightString(Integer.toString(p1.getPrice()),6) + " " + Tools.formatString(p1.getOwnerString(),25); 
                strOut += "   " + Tools.rightString(p2.getCaption(),6) + " " + Tools.rightString(Integer.toString(p2.getPrice()),6) + " " + p2.getOwnerString();  
                m_botAction.sendPrivateMessage(name,strOut);
            }
            m_botAction.sendPrivateMessage(name,"To view the 10 most expensive plots, use: !listplots high");
        }
    }
    
    public void cmdStatus(String name,String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}
        Pirate cp = m_players.get(name);
        if (cp == null) {
            loadOrCreatePlayer(name,true);
            cp = m_players.get(name);
        }
        int totalCoins = cp.getCoins();
        int foundCoins = cp.getFoundCoins();
        int bountyCoins = cp.getBountyCoins();
        int taxCoins = cp.getTaxCoins();
        int newCoins = foundCoins + bountyCoins + taxCoins;
        int houseCount = cp.getHouseCount();
        int plotCount = cp.getPlotCount();
        String strHouses = " ";
        String strPlots = " ";
        if (cp.getHouseCount() > 0) {strHouses = " (" + cp.getHouseList() + ") ";}
        if (cp.getPlotCount() > 0) {strPlots = " (" + cp.getPlotList() + ") ";}
        m_botAction.sendPrivateMessage(name,"You have " + totalCoins + " coins.  This game, you've earned " + newCoins + " [found: " + foundCoins + ", bounty: " + bountyCoins + ", tax: " + taxCoins + "]");
        m_botAction.sendPrivateMessage(name,"You own " + getPlural(houseCount,"house") + strHouses + "and " + getPlural(plotCount,"plot") + strPlots);
    }

    public void cmdAward(String name,String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}

        String params[] = msg.split(":");
        if (params.length != 3 && params.length != 4) {m_botAction.sendPrivateMessage(name,"Huh?  You need to use !award <player>:<amount>:<reason>"); return;}

        String awPlayer = params[0];
        int awAmount = 0;
        try {awAmount = Integer.parseInt(params[1]);} catch (NumberFormatException e) {};
        String awReason = params[2];

        if (awAmount == 0) {m_botAction.sendPrivateMessage(name,"Huh?  You need to use !award <player>:<amount>:<reason>"); return;}

        Boolean processAward = false;

        Pirate p = m_players.get(awPlayer);
        if (p == null) {
            // player isn't loaded
            Player bp = m_botAction.getPlayer(awPlayer);
            if (bp == null) {
                // player isn't in the arena
                if (params.length == 4 && params[3].equalsIgnoreCase("load")) {
                    // attempt to load from database
                    if (loadOrCreatePlayer(awPlayer,false) == 1) {
                        // player successfully loaded
                        p = m_players.get(awPlayer);
                        processAward = true;
                    } else {
                        // player doesn't exist
                        m_botAction.sendPrivateMessage(name,"The player '" + awPlayer + "' does not exist in the database and is not in the arena.  Unable to issue reward.");
                    }
                } else {
                    m_botAction.sendPrivateMessage(name,"The player '" + awPlayer + "' is not loaded and is not in the arena.  Use !award <player>:<amount>:<reason>:load if you would like me to attempt to load this player from the database in order to issue them a reward.");
                }
            } else {
                // player is in the arena, create an account for them
                p = m_players.get(awPlayer);
                processAward = true;
            }
        } else {
            processAward = true;
        }

        if (processAward) {
            p.addFoundCoins(awAmount);
            p.addRecord(new BankRecord(p.getPlayerID(),0,"",0,p.getPlayerID(),0,0,0,0,name,awAmount,awReason,true));
            m_botAction.sendArenaMessage("Cap'n " + name + " has awarded " + awPlayer + " " + awAmount + " coins fer: " + awReason);
        }
    }

    public void cmdBuyHouse(String name,String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}
        String msgs[] = msg.split(" ");
        if (msgs.length != 2) {m_botAction.sendPrivateMessage(name,"Huh?  You need to use !buyhouse <house#> <price>"); return;}
        int curHouseID = 0;
        int curOffer = 0;
        try {curHouseID = Integer.parseInt(msgs[0]);} catch (NumberFormatException e) {}

        if (curHouseID > 100 && curHouseID < 155) {
            House curHouse = m_houses.get(curHouseID);
            int curPrice = curHouse.getPrice();
            try {curOffer = Integer.parseInt(msgs[1]);} catch (NumberFormatException e) {}
            if (curOffer >= curPrice) {
                // does the player have enough coins?
                Pirate p = m_players.get(name);
                if (p == null) {loadOrCreatePlayer(name,true); p = m_players.get(name);}
                if (p.getCoins() >= curOffer) {
                    // make the sale
                    // is there a previous owner?
                    String curOwner = curHouse.getOwner();
                    int sellerID = 0;
                    if (curOwner != null) {
                        Pirate op = m_players.get(curOwner);
                        if (op == null) {
                            loadOrCreatePlayer(curOwner,true);
                            op = m_players.get(curOwner);
                        }
                        sellerID = op.getPlayerID();
                        op.addCoins(curHouse.getCoinsPaid());
                        op.removeHouse(curHouseID);
                        op.addRecord(new BankRecord(sellerID,0,"",p.getPlayerID(),sellerID,0,curHouseID,curHouse.getCoinsPaid(),curOffer,"",0,"",true));
                        if (m_botAction.getPlayer(curOwner) != null) {m_botAction.sendPrivateMessage(curOwner,"Your house at " + curHouseID + " was purchased by " + name + " for " + curOffer + " coins.  " + curHouse.getCoinsPaid() + " coins have been returned to you.");}
                    }
                    curHouse.setCoinsPaid(curOffer);
                    p.removeCoins(curOffer);
                    m_botAction.sendPrivateMessage(name,"Ye now own house number " + curHouseID);
                    p.addHouse(curHouseID);
                    curHouse.setOwner(name,p.getPirateName());
                    p.addRecord(new BankRecord(p.getPlayerID(),0,"",p.getPlayerID(),sellerID,0,curHouseID,curHouse.getCoinsPaid(),curOffer,"",0,"",true));
                } else {
                    m_botAction.sendPrivateMessage(name,"You can't afford " + curOffer + "!");
                }
            } else {
                m_botAction.sendPrivateMessage(name,"Ye be needin to offer at least " + curPrice + " for house # " + curHouseID);
            }
        } else {m_botAction.sendPrivateMessage(name,"Invalid House Number.  Valid house numbers are 101 thru 154.");}

    }
    
    public void cmdName(String name, String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}

        Pirate p = m_players.get(name);
        if (p == null) {
            loadOrCreatePlayer(name,true);
            p = m_players.get(name);
        }

        if (!msg.equalsIgnoreCase("CHANGE")) {
            if (p.getCoins() < 1000) {
                m_botAction.sendPrivateMessage(name,"Yer name is " + p.getPirateName() + ".  Yer stuck with it cuz you cannot afford the " + NAMECHANGE_PRICE + " coins to change it.  HAR HAR HAR!");
            } else {
                m_botAction.sendPrivateMessage(name,"Yer name is " + p.getPirateName() + ".  If'n you want a new name, it'll cost ye " + NAMECHANGE_PRICE + " coins.  If ye can afford it, PM me with !name CHANGE");
            }
        } else {
            if (p.getCoins() >= NAMECHANGE_PRICE) {
                p.removeCoins(NAMECHANGE_PRICE);
                p.addRecord(new BankRecord(p.getPlayerID(),0,"",0,p.getPlayerID(),0,0,0,0,"",-NAMECHANGE_PRICE,"Name Change",true));
                String newname = generatePirateName(name);
                p.setPirateName(newname);
                m_botAction.sendArenaMessage(name + " will now be called \"" + newname + "\"");
            } else {
                m_botAction.sendPrivateMessage(name,"You can't afford a new name!  Come back when you have " + NAMECHANGE_PRICE + " coins!");
            }
        }
    }
    
    public void cmdSell(String name, String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}
        int curPlotID = 0;
        curPlotID = plotToPlotID(msg);
        if (curPlotID > 0) {
            Plot curPlot = m_plots.get(curPlotID);
            // If a special chunk of land, make the user use the proper name.
            if (msg.equalsIgnoreCase(curPlot.getCaption()) == false) {
                m_botAction.sendPrivateMessage(name,msg.toUpperCase() + " is part of the " + curPlot.getCaption() + ".  Use !sell " + curPlot.getCaption() + " to sell it.");
                return;
            }
            if (curPlot.getOwner().equals(name)) {
                Pirate p = m_players.get(name);
                if (p == null) {
                    loadOrCreatePlayer(name,true);
                    p = m_players.get(name);
                }
                p.addCoins(curPlot.getCoinsPaid());
                p.removePlot(curPlotID);
                p.addRecord(new BankRecord(p.getPlayerID(),0,"",0,p.getPlayerID(),curPlotID,0,curPlot.getCoinsPaid(),0,"",0,"",true));
                curPlot.setOwner(null,null);
                m_botAction.sendPrivateMessage(name,curPlot.getCaption() + " has been sold for " + curPlot.getCoinsPaid() + " coins.");
                curPlot.setCoinsPaid(0);
            } else {
                m_botAction.sendPrivateMessage(name,"ARRR ya slimey scallywag, ye can't sell what don't belong to ya!");
            }
        //} else if (curPlotID == -2) {
        //    m_botAction.sendPrivateMessage(name,"ARRR ya slimey scallywag, ye can't sell the dueling box!");
        } else if (curPlotID == -1) {
            m_botAction.sendPrivateMessage(name,"ARRR ya slimey scallywag, ye can't sell the sea!");
        } else {
            m_botAction.sendPrivateMessage(name,"Invalid Plot.  Use !sell <Plot Address>.  PM the bot with: !guide plots for help with plots.");
        }
    }

    public void cmdBuy(String name, String msg) {
        if (!isRunning) {m_botAction.sendPrivateMessage(name,"Tortuga must be activated to use this command."); return;}
        String msgs[] = msg.split(" ");
        if (msgs.length != 2) {m_botAction.sendPrivateMessage(name,"Huh?  You need to use !buy <plot> <price>."); return;}
        int curPlotID = 0;
        int curOffer = 0;
        curPlotID = plotToPlotID(msgs[0]);
        if (curPlotID > 0) {
            Plot curPlot = m_plots.get(curPlotID);
            
            // If a special chunk of land, make the user use the proper name.
            if (msgs[0].equalsIgnoreCase(curPlot.getCaption()) == false) {
                m_botAction.sendPrivateMessage(name,msgs[0].toUpperCase() + " is part of the " + curPlot.getCaption() + ".  Use !buy " + curPlot.getCaption() + " <price> to purchase it.");
                return;
            }

            int curPrice = curPlot.getPrice();
            try {curOffer = Integer.parseInt(msgs[1]);} catch (NumberFormatException e) {}
            if (curOffer >= curPrice) {
                // does the player have enough coins?
                Pirate p = m_players.get(name);
                if (p == null) {loadOrCreatePlayer(name,true); p = m_players.get(name);}
                if (p.getCoins() >= curOffer) {
                    // make the sale
                    // is there a previous owner?
                    String curOwner = curPlot.getOwner();
                    int sellerID = 0;
                    if (curOwner != null) {
                        Pirate op = m_players.get(curOwner);
                        if (op == null) {
                            loadOrCreatePlayer(curOwner,true);
                            op = m_players.get(curOwner);
                        }
                        sellerID = op.getPlayerID();
                        op.addCoins(curPlot.getCoinsPaid());
                        op.removePlot(curPlotID);
                        op.addRecord(new BankRecord(sellerID,0,"",p.getPlayerID(),sellerID,curPlotID,0,curPlot.getCoinsPaid(),curOffer,"",0,"",true));
                        if (m_botAction.getPlayer(curOwner) != null) {m_botAction.sendPrivateMessage(curOwner,"Your plot (" + curPlot.getAddr() + ") was purchased by " + name + " for " + curOffer + " coins.  " + curPlot.getCoinsPaid() + " coins have been returned to you.");}
                    }
                    curPlot.setCoinsPaid(curOffer);
                    p.removeCoins(curOffer);
                    p.addPlot(curPlotID);
                    curPlot.setOwner(name,p.getPirateName());
                    p.addRecord(new BankRecord(p.getPlayerID(),0,"",p.getPlayerID(),sellerID,curPlotID,0,curPlot.getCoinsPaid(),curOffer,"",0,"",true));
                    m_botAction.sendPrivateMessage(name,"Ye now own " + curPlot.getCaption());
                } else {
                    m_botAction.sendPrivateMessage(name,"You can't afford " + curOffer + "!");
                }
            } else {
                m_botAction.sendPrivateMessage(name,"Ye be needin to offer at least " + curPrice + " for " + curPlot.getCaption());
            }
        //} else if (curPlotID == -2) {
        //    m_botAction.sendPrivateMessage(name,msgs[0].toUpperCase() + " is part of the dueling box and it cannot be purchased.");
        } else if (curPlotID == -1) {
            m_botAction.sendPrivateMessage(name,msgs[0].toUpperCase() + " is the sea and it belongs to all pirates.");
        } else {
            m_botAction.sendPrivateMessage(name,"Invalid Plot.  Use !buy <plot> <price>.  PM the bot with: !guide plots for help with plots.");
        }
    }
    
    public void cmdHouseInfo(String name, String msg) {
        int curHouseID = 0;
        if (msg.length() > 0) {
            try {curHouseID = Integer.parseInt(msg);} catch (NumberFormatException e) {}
        }
        if (curHouseID > 100 && curHouseID < 155) {
            House curHouse = m_houses.get(curHouseID);
            String curOwner = curHouse.getOwner();
            if (curOwner == null) {curOwner = "unowned";} else {curOwner = "owned by " + curOwner;}
            m_botAction.sendPrivateMessage(name,"House #" + curHouseID + " is " + curOwner + " (buy for: " + curHouse.getPrice() + "+)");
        } else {
            m_botAction.sendPrivateMessage(name,"Invalid House Number.  Valid house numbers are 101 thru 154.");
        }
    }

    public void cmdInfo(String name, String msg) {
        int curPlotID = 0;
        int playerX = 0;
        int playerY = 0;
        if (msg.length() > 0) {curPlotID = plotToPlotID(msg);}
        if (curPlotID > 0) {
            Plot curPlot = m_plots.get(curPlotID);
            String plotAddr = (char)(curPlot.getPlotX()+65) + "" + (curPlot.getPlotY()+1);
            String curOwner = curPlot.getOwner();
            if (curOwner == null) {curOwner = "unowned";} else {curOwner = "owned by " + curOwner;}
            m_botAction.sendPrivateMessage(name,curPlot.getCaption() + " is " + curOwner + " (buy for: " + curPlot.getPrice() + "+)");
        } else if (curPlotID == -1) {
            m_botAction.sendPrivateMessage(name,msg.toUpperCase() + " is the sea and it belongs to all pirates.");
        } else {
            m_botAction.sendPrivateMessage(name,"Invalid Plot.  Use !info <Plot Address>.  PM the bot with: !guide plots for help with plots.");
        }
    }

    public void loadPlots() {
        try {
            ResultSet r = m_botAction.SQLQuery(m_database,"SELECT fnPlotID, fnPlotX, fnPlotY, fnEarned, fcPlayerName, fcPirateName, fnCoinsPaid from tblPiratePlots plots left join tblPiratePlayers players on plots.fnPlayerID=players.fnPlayerID");
            if (r == null) {fatal("Error reading plots: NULL resultset returned.");}

            int plotID, plotX, plotY;
            while (r.next()) {
                plotID = r.getInt("fnPlotID");
                plotX = r.getInt("fnPlotX");
                plotY = r.getInt("fnPlotY");
                m_plots.put(plotID,new Plot(plotID,plotX,plotY,r.getString("fcPlayerName"),r.getString("fcPirateName"),r.getInt("fnCoinsPaid"),r.getInt("fnEarned")));
                arrPlotIDs[plotX][plotY] = plotID;
            }
            m_botAction.SQLClose(r);
        } catch (SQLException e) {fatal("Error reading plots [ERR: " + e.getMessage() + "]");}
    }

    public void loadHouses() {
        try {
            ResultSet r = m_botAction.SQLQuery(m_database,"SELECT fnHouseID, fcPlayerName, fcPirateName, fnCoinsPaid from tblPirateHouses houses left join tblPiratePlayers players on houses.fnPlayerID=players.fnPlayerID");
            if (r == null) {fatal("Error reading houses: NULL resultset returned.");}

            int houseID;
            while (r.next()) {
                houseID = r.getInt("fnHouseID");
                m_houses.put(houseID,new House(houseID,r.getString("fcPlayerName"),r.getString("fcPirateName"),r.getInt("fnCoinsPaid")));
            }
            m_botAction.SQLClose(r);
        } catch (SQLException e) {fatal("Error reading houses [ERR: " + e.getMessage() + "]");}
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, PLAYER_DEATH);
        events.request(this, PRIZE);
        events.request(this, PLAYER_ENTERED);
        events.request(this, FLAG_REWARD);
        events.request(this, FLAG_CLAIMED);
    }

    public void registerCommands() {
        int intPrivs;
        intPrivs = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand("!on",            intPrivs, this, "cmdOn", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!shutdown",      intPrivs, this, "cmdShutdown", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!award",         intPrivs, this, "cmdAward", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!top",           intPrivs, this, "cmdTop", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!reset",         intPrivs, this, "cmdReset", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!killmsgs",      intPrivs, this, "cmdKillmsgs", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!guide",         intPrivs, this, "cmdGuide");
        m_commandInterpreter.registerCommand("!name",          intPrivs, this, "cmdName");
        m_commandInterpreter.registerCommand("!status",        intPrivs, this, "cmdStatus");
        m_commandInterpreter.registerCommand("!info",          intPrivs, this, "cmdInfo");
        m_commandInterpreter.registerCommand("!buy",           intPrivs, this, "cmdBuy");
        m_commandInterpreter.registerCommand("!sell",          intPrivs, this, "cmdSell");
        m_commandInterpreter.registerCommand("!houseinfo",     intPrivs, this, "cmdHouseInfo");
        m_commandInterpreter.registerCommand("!buyhouse",      intPrivs, this, "cmdBuyHouse");
        m_commandInterpreter.registerCommand("!listhouses",    intPrivs, this, "cmdListHouses");
        m_commandInterpreter.registerCommand("!listplots",     intPrivs, this, "cmdListPlots");
        m_commandInterpreter.registerCommand("!myplots",       intPrivs, this, "cmdMyPlots");
        m_commandInterpreter.registerCommand("!myhouses",      intPrivs, this, "cmdMyHouses");
        m_commandInterpreter.registerCommand("!records",       intPrivs, this, "cmdRecords");
        m_commandInterpreter.registerCommand("!help",          intPrivs, this, "cmdHelp");
    }

    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);
    }

    public void handleEvent(PlayerEntered event) {
        if (isRunning) {
            m_botAction.showObjectForPlayer(event.getPlayerID(),1);
        }
    }

    public void handleEvent(FlagReward event) {
        int freq = event.getFrequency();
        if (freq == 0) {
            m_freqscores.put("freq0FlagTime",m_freqscores.get("freq0FlagTime")+1);
        } else if (freq == 1) {
            m_freqscores.put("freq1FlagTime",m_freqscores.get("freq1FlagTime")+1);
        }
    }

    public void handleEvent(FlagClaimed event) {
        Player claimer = m_botAction.getPlayer(event.getPlayerID());
        m_scores.get(claimer).flagTouches += 1;
    }
    
    public void handleEvent(PlayerDeath event) {
        if (!isRunning) {return;}
        Player killer = m_botAction.getPlayer(event.getKillerID());
        int killerX = killer.getXTileLocation();
        int killerY = killer.getYTileLocation();

        Player killed = m_botAction.getPlayer(event.getKilleeID());
        String killedName = killed.getPlayerName();
        Pirate kp = m_players.get(killedName);
        if (kp == null) {
            loadOrCreatePlayer(killedName,true);
            kp = m_players.get(killedName);
        }

        // prize the killer
        String playerName = killer.getPlayerName();
        Pirate cp = m_players.get(playerName);
        if (cp == null) {
            loadOrCreatePlayer(playerName,true);
            cp = m_players.get(playerName);
        }
        
        m_scores.get(killedName).deaths++;
        m_scores.get(playerName).kills++;
        int killerfreq = killer.getFrequency();
        if (killerfreq % 2 == 0) {
            m_scores.get(playerName).freq0Kills++;
        } else {
            m_scores.get(playerName).freq1Kills++;
        }

        // prize the land owner (if the owner isn't the killer)
        int plotID = pointToPlotID(killerX,killerY);
        
        if (plotID != -1) { // no prizing for no-fly zones (shouldn't be seeing kills here anyway?)
            Plot curPlot = m_plots.get(plotID);
            String plotOwner = curPlot.getOwner();
            if (plotOwner != null && plotOwner != playerName) {
                cp.addBountyCoins(BOUNTY_COINS-TAX_COINS);
                Pirate op = m_players.get(plotOwner);
                if (op == null) {
                    loadOrCreatePlayer(plotOwner,true);
                    op = m_players.get(plotOwner);
                }
                op.addTaxCoins(plotID,TAX_COINS);
            } else {
                cp.addBountyCoins(BOUNTY_COINS);
            }
            curPlot.addEarnings(TAX_COINS);
        } else {
            logError("Player killed on plot that is a NO-FLY zone (outside boundaries).  Player: " + killedName + " killed by " + playerName + " at " + killerX + "," + killerY);
        }
        if (boolAnnounce) {
            if (killer.getShipType() == 3) {
                m_botAction.sendArenaMessage("The dread pirate " + kp.getPirateName() + " was hunted by " + cp.getPirateName());
            } else {
                m_botAction.sendArenaMessage(kp.getPirateName() + " was killed by " + cp.getPirateName());
            }
        }
    }

    public void handleEvent(Prize event) {
        if (!isRunning) {return;}
        Player p = m_botAction.getPlayer(event.getPlayerID());
        String playerName = p.getPlayerName();
        Pirate cp = m_players.get(playerName);
        if (cp == null) {
            loadOrCreatePlayer(playerName,true);
            cp = m_players.get(playerName);
        }
        cp.addFoundCoins(10);
    }
    
    public void cmdGuide(String name, String msg){
        if (!boolInTortuga) {m_botAction.sendPrivateMessage(name,"You (and the bot) must be in ?go tortuga to use this command."); return;}

        Player mp = m_botAction.getPlayer(name);
        if (mp == null) {return;}

        int pID = mp.getPlayerID();
        // hide all pages
        m_botAction.setupObject(pID,2,false);
        m_botAction.setupObject(pID,3,false);
        m_botAction.setupObject(pID,4,false);
        if (!msg.equals("off")) {
            if (msg.equals("houses")) {
                m_botAction.setupObject(pID,3,true);
            } else if (msg.equals("plots")) {
                m_botAction.setupObject(pID,4,true);
            } else {
                m_botAction.setupObject(pID,2,true);
            }
        }
        m_botAction.sendSetupObjectsForPlayer(pID);
    }

    public void cmdHelp(String name, String msg) {
        if (m_botAction.getOperatorList().isER(name)) {
            m_botAction.remotePrivateMessageSpam(name,getModHelpMessage());
        } else {
            m_botAction.remotePrivateMessageSpam(name,getPlayerHelpMessage());
        }
    }

    public  String[] getModHelpMessage() {
        if (boolInTortuga) {
            String[] message = {
                "|-----------------------------------------------------------------------|",
                "| ER+ Commands                                                          |",
                "|  !on          - Activates Tortuga (begins awarding for coins/kills).  |",
                "|  !shutdown    - De-Activated Tortuga.                                 |",
                "|  !top         - Displays the Top Players tracked by bot               |",
                "|  !reset       - Resets Top Players tracked by bot                     |",
                "|  !award <player>:<amount>:<reason> Announces and awards <amount>      |",
                "|      coins to <player>.  Reason is required and must be < 30 chars.   |",
                "|  !killmsgs    - Toggles announcing kills as *arena messages.          |",
                "|-----------------------------------------------------------------------|",
                "| Player Commands:                                                      |",
                "|  !guide                     - A guide to Tortuga for new players      |",
                "|  !status                    - Shows your player status                |",
                "|  !records                   - View your Last 10 Bank Records          |",
                "|  !name                      - View/change pirate name                 |",
                "|  !info <plot>               - Look up plot records for <plot>         |",
                "|  !buy <plot> <price>        - Purchase <plot> for <price>             |",
                "|  !sell <plot>               - Sell <plot> for the price you paid      |",
                "|  !houseinfo <house#>        - Shows the owner and price for the house |",
                "|  !buyhouse <house#> <price> - Purchase the house for <price>          |",
                "|  !myplots, !myhouses        - List plots/houses you own               |",
                "|  !listhouses, !listplots    - Lists Top 10 houses/plots by price      |",
                "|-----------------------------------------------------------------------|"
            };
            return message;
        } else {
            String[] message = {
                    "|-----------------------------------------------------------------------|",
                    "| ER+ Commands                                                          |",
                    "|  !on          - Activates Tortuga (begins awarding for coins/kills).  |",
                    "|  !shutdown    - De-Activated Tortuga.                                 |",
                    "|  !top         - Displays the Top Players tracked by bot               |",
                    "|  !reset       - Resets Top Players tracked by bot                     |",
                    "|  !award <player>:<amount>:<reason> Announces and awards <amount>      |",
                    "|      coins to <player>.  Reason is required and must be < 30 chars.   |",
                    "|  !killmsgs    - Toggles announcing kills as *arena messages.          |",
                    "|-----------------------------------------------------------------------|",
                    "| Player Commands:                                                      |",
                    "|  !status                    - Shows your player status                |",
                    "|  !records                   - View your Last 10 Bank Records          |",
                    "|  !name                      - View/change pirate name                 |",
                    "| You can only purchase houses/plots in ?go tortuga, but commands to    |",
                    "| do so are available in any arena where tortuga is activated.          |",
                    "|  !info <plot>               - Look up plot records for <plot>         |",
                    "|  !buy <plot> <price>        - Purchase <plot> for <price>             |",
                    "|  !sell <plot>               - Sell <plot> for the price you paid      |",
                    "|  !houseinfo <house#>        - Shows the owner and price for the house |",
                    "|  !buyhouse <house#> <price> - Purchase the house for <price>          |",
                    "|  !myplots, !myhouses        - List plots/houses you own               |",
                    "|  !listhouses, !listplots    - Lists Top 10 houses/plots by price      |",
                    "|-----------------------------------------------------------------------|"
            };
            return message;
        }
    }

    public String[] getPlayerHelpMessage() {
        if (boolInTortuga) {
            String[] help = {
                "|-----------------------------------------------------------------------|",
                "| Commands directed privately:                                          |",
                "|  !guide                     - A guide to Tortuga for new players      |",
                "|  !status                    - Shows your player status                |",
                "|  !records                   - View your Last 10 Bank Records          |",
                "|  !name                      - View/change pirate name                 |",
                "|  !info <plot>               - Look up plot records for <plot>         |",
                "|  !buy <plot> <price>        - Purchase <plot> for <price>             |",
                "|  !sell <plot>               - Sell <plot> for the price you paid      |",
                "|  !houseinfo <house#>        - Shows the owner and price for the house |",
                "|  !buyhouse <house#> <price> - Purchase the house for <price>          |",
                "|  !myplots, !myhouses        - List plots/houses you own               |",
                "|  !listhouses, !listplots    - Lists Top 10 houses/plots by price      |",
                "|-----------------------------------------------------------------------|"
            };
            return help;
        } else {
            String[] help = {
                    "|-----------------------------------------------------------------------|",
                    "| Commands directed privately:                                          |",
                    "|  !status                    - Shows your player status                |",
                    "|  !records                   - View your Last 10 Bank Records          |",
                    "|  !name                      - View/change pirate name                 |",
                    "| You can only purchase houses/plots in ?go tortuga, but commands to    |",
                    "| do so are available in any arena where tortuga is activated.          |",
                    "|  !info <plot>               - Look up plot records for <plot>         |",
                    "|  !buy <plot> <price>        - Purchase <plot> for <price>             |",
                    "|  !sell <plot>               - Sell <plot> for the price you paid      |",
                    "|  !houseinfo <house#>        - Shows the owner and price for the house |",
                    "|  !buyhouse <house#> <price> - Purchase the house for <price>          |",
                    "|  !myplots, !myhouses        - List plots/houses you own               |",
                    "|  !listhouses, !listplots    - Lists Top 10 houses/plots by price      |",
                    "|-----------------------------------------------------------------------|"
                };
            return help;
        }
    }

    public boolean isUnloadable() {
        return true;
    }

    /**
     * Special chunks of land include multiple Plots (eg, the Island includes H4-H5, I4-I5 and J4-J5), but we want
     * to treat these like just one Plot of land, with one X/Y, so we convert all others within that chunk of land
     * to the primary one (eg: the Islands primary plot is H4 (plotX = 7, plotY = 3) so we'll convert all others
     * (H5, I4-I5 and J4-J5) to H4 and return that.  If the Plot is one that's not possible to fly through,
     * (the water), then a PlotPoint with coords -1/-1 will be returned.  -2/-2 is returned for the dueling arena.
     */
    public PlotPoint getPrimaryPlot(int x, int y) {
        int newx = x;
        int newy = y;

        if (x == 7 && y == 4) {newx = 7; newy = 3;}                          // ISLAND
        else if (x == 8 && (y == 3 || y == 4)) {newx = 7; newy = 3;}         // ISLAND
        else if (x == 9 && (y == 3 || y == 4)) {newx = 7; newy = 3;}         // ISLAND
        // else if (x >= 13 && x <= 18 && y >= 9 && y <= 19) {newx = -2; newy = -2;} // DUELING BOX
        else if ((x >= 5 && x <= 18) && y == 0) {newx = 5; newy = 0;}        // NWALL (north wall)
        else if (x == 0 && (y >= 4 && y <= 17)) {newx = 0; newy = 4;}        // WWALL (west wall)
        else if ((x >= 1 && x <= 11) && y == 17) {newx = 1; newy = 17;}      // SWALL (south wall)
        else if (x == 19 && (y >= 1 && y <= 8)) {newx = 19; newy = 0;}       // EWALL (east wall)
        else if (x == 12 && (y >= 8 && y <= 17)) {newx = 19; newy = 0;}      // EWALL (east wall)
        else if (x >= 0 && x <= 4 && y >= 0 && y <= 3) {newx = 0; newy = 0;} // SKULL
        else if ((x >= 0 && x <= 11) && (y == 18 || y == 19)) {newx = -1; newy = -1;} // NOFLY ZONE (SEA)
        else if (x == 19 && (y >= 9 && y <= 19)) {newx = -1; newy = -1;}     // NOFLY ZONE (SEA)

        return new PlotPoint(newx,newy);
    }
    
    /**
     * Returns the Plot ID at the X/Y tile 
     * location sent (x/y should be 0-1023)
     * Returns -1 if it is a nofly zone
     */
    public int pointToPlotID(int x, int y) {
        // the goal here is to determine the COORDS (A1-T20 as accurately as is reported by the radar)
        // some columns/rows are a little wider than others, so to compensate:
        x--; y--;
        if (x >= 253) {x--;}
        if (x >= 765) {x--;}
        if (x >= 767) {x--;}
        if (y >= 253) {y--;}
        if (y >= 765) {y--;}
        if (y >= 767) {y--;}

        // Make sure we aren't out of bounds
        if (x < 0) {x = 0;}
        if (y < 0) {y = 0;}
        if (x > 1019) {x = 1019;}
        if (y > 1019) {y = 1019;}

        int plotX, plotY;
        plotX = (int)Math.floor(x/51);
        plotY = (int)Math.floor(y/51);

        PlotPoint realPlot = getPrimaryPlot(plotX,plotY);
        plotX = realPlot.getX();
        plotY = realPlot.getY();

        if (plotX != -1) {
            return arrPlotIDs[plotX][plotY];
        } else {
            return -1;
        }
    }
    
    /**
     * converts string (eg: A1) to the ID of the plot (as found in the plots table).
     * returns 0 if the string is an invalid coord.  returns -1 if it is part of the sea (area ships can't fly into) 
     */
    public int plotToPlotID(String curPlot) {
        curPlot = curPlot.toUpperCase();

        // if curPlot sent is a special (ISLAND, SKULL, etc), switch it to its primary plot addr.
        String liPlot;
        Iterator<String> li = m_specials.keySet().iterator();
        while (li.hasNext()) {
            liPlot = li.next();
            if (m_specials.get(liPlot).equals(curPlot)) {
                curPlot = liPlot;
                break;
            }
        }

        int plotX = (int)(curPlot.charAt(0))-64;
        int plotY = 0;
        try {
            plotY = Integer.parseInt(curPlot.substring(1));
        } catch (NumberFormatException e) {return 0;}

        if (plotX >= 1 && plotX <= 20 && plotY >= 1 && plotY <= 20) {
            plotX--;
            plotY--;

            PlotPoint realPlot = getPrimaryPlot(plotX,plotY);
            plotX = realPlot.getX();
            plotY = realPlot.getY();
            if (plotX == -1) {return -1;} else {return arrPlotIDs[plotX][plotY];}
        } else {return 0;}
    }

    public int loadOrCreatePlayer(String playerName, Boolean createPlayer) {
        // returns 0 if player not loaded (doesn't exist in the db)
        // returns 1 if player loaded
        // returns 2 if player had to be created
        int playerLoaded = 0;
        List<Integer> arrPlots = new LinkedList<Integer>(); // plot IDs the player owns
        HashMap<Integer,PiratePlotEarnings> m_earnings = new HashMap<Integer,PiratePlotEarnings>(); // player plotID -> earnings, may contain plots not currently owned by player
        List<Integer> arrHouses = new LinkedList<Integer>();
        Vector<BankRecord> m_bankrecords = new Vector<BankRecord>();

        try {
            ResultSet r = m_botAction.SQLQuery(m_database,"SELECT fnPlayerID,fnCoins,fcPirateName from tblPiratePlayers where fcPlayerName = '" + playerName.replace("'","\'") + "'");
            if (r == null) {fatal("Error reading player info: NULL resultset returned.");}

            if (r.next()) {
                // player exists, load em up
                int playerID = r.getInt("fnPlayerID");

                // the next two queries should be converted into one, but since my lazy ass is still running a mySQL
                // 3.23 development db that doesn't support full outer joins, we'll do it the inefficient way for now.
                String strQuery = "SELECT fnPlotID,fnEarned from tblPiratePlots where fnPlayerID = " + playerID;
                
                ResultSet rp = m_botAction.SQLQuery(m_database,strQuery);
                if (rp == null) {
                    m_botAction.SQLClose(r);
                    fatal("Error reading plot info for player: NULL resultset returned. [" + strQuery + "]");
                }
                while (rp.next()) {arrPlots.add(rp.getInt("fnPlotID"));}
                m_botAction.SQLClose(rp);

                strQuery = "SELECT fnPlotID,fnEarned from tblPirateEarnings where fnPlayerID = " + playerID;
                ResultSet rp2 = m_botAction.SQLQuery(m_database,strQuery);
                if (rp2 == null) {
                    m_botAction.SQLClose(r);
                    fatal("Error reading plot info for player: NULL resultset returned. [" + strQuery + "]");
                }
                while (rp2.next()) {
                    m_earnings.put(rp2.getInt("fnPlotID"),new PiratePlotEarnings(playerID,rp2.getInt("fnPlotID"),rp2.getInt("fnEarned")));
                }
                m_botAction.SQLClose(rp2);

                ResultSet rp3 = m_botAction.SQLQuery(m_database,"SELECT fnHouseID from tblPirateHouses where fnPlayerID = " + playerID);
                if (rp3 == null) {
                    m_botAction.SQLClose(r);
                    fatal("Error reading house info for player: NULL resultset returned.");
                }
                while (rp3.next()) {arrHouses.add(rp3.getInt("fnHouseID"));}
                m_botAction.SQLClose(rp3);

                ResultSet rp4 = m_botAction.SQLQuery(m_database,"SELECT fnEntryID,fdDate,fnBuyerID,fnSellerID,fnPlotID,fnHouseID,fnOriPrice,fnNewPrice,fcBonusBy,fnBonusAmount,fcBonusReason from tblPirateBankRecords where fnBuyerID = " + playerID + " or fnSellerID = " + playerID);
                if (rp4 == null) {
                    m_botAction.SQLClose(r);
                    fatal("Error reading bank record info for player: NULL resultset returned.");
                }
                while (rp4.next()) {
                    m_bankrecords.add(new BankRecord(playerID,rp4.getInt("fnEntryID"),rp4.getString("fdDate"),rp4.getInt("fnBuyerID"),rp4.getInt("fnSellerID"),rp4.getInt("fnPlotID"),rp4.getInt("fnHouseID"),rp4.getInt("fnOriPrice"),rp4.getInt("fnNewPrice"),rp4.getString("fcBonusBy"),rp4.getInt("fnBonusAmount"),rp4.getString("fcBonusReason"),false));
                }
                // create a record to store the players progress for THIS game.
                try {
                    m_botAction.SQLQueryAndClose(m_database,"INSERT into tblPirateStats(fnGameID,fnPlayerID,fnStartPlots) values(" + m_gameid + "," + playerID + "," + arrPlots.size() + ")");
                } catch (SQLException e) {fatal("Error creating player: " + playerName + " - " + e.getMessage());}

                m_players.put(playerName,new Pirate(playerID,playerName,r.getString("fcPirateName"),r.getInt("fnCoins"),arrPlots,arrHouses,m_earnings,m_bankrecords));
                m_scores.put(playerName,new ScoreSet(playerName));
                playerLoaded = 1;
            } else {
                if (createPlayer) {
                    // create player
                    String pirateName = generatePirateName(playerName);
                    int playerID = createPlayer(playerName,pirateName);

                    // create a record to store the players progress for THIS game.
                    try {
                        m_botAction.SQLQueryAndClose(m_database,"INSERT into tblPirateStats(fnGameID,fnPlayerID) values(" + m_gameid + "," + playerID + ")");
                    } catch (SQLException e) {fatal("Error creating player: " + playerName + " - " + e.getMessage());}

                    m_players.put(playerName,new Pirate(playerID,playerName,pirateName,0,arrPlots,arrHouses,m_earnings,m_bankrecords));
                    m_scores.put(playerName,new ScoreSet(playerName));
                
                    // message the newly created player if they are in the arena, which.. they should always be
                    Player newPlayer;
                    newPlayer = m_botAction.getPlayer(playerName);
                    if (newPlayer == null) {
                        logError("Just created a player that doesn't exist in the arena: " + playerName);
                    } else {
                        m_botAction.sendPrivateMessage(playerName,"A landlubber eh?  Well if you'll be gettin' yer feet wet, you'll be needin a proper name.");
                        m_botAction.sendPrivateMessage(playerName,pirateName + " will do.  If ya don't like it, you'll be needin to pay the Cap'n to git yerself some new papers.");
                        m_botAction.sendArenaMessage("Welcome our newest matey, " + playerName + ", who will now be called \"" + pirateName + "\"");
                    }
                    playerLoaded = 2;
                }
            }
            m_botAction.SQLClose(r);
        } catch (SQLException e) {fatal("Error reading player info [ERR: " + e.getMessage() + "]");}
        return playerLoaded;
    }
    
    public String generatePirateName(String playerName) {
        String pirateName = "";
        String pirateInitial = "-";
        int x;
        int asc;
        Boolean nameIsLower = false; // what the hell, if the player likes lowercase letters, we'll lowercase their entire pirate name

        // Find the first A-Z character in the players name
        for (x = 0; x < playerName.length(); x++) {
            asc = (int)(playerName.charAt(x));
            if ((asc > 96 && asc < 123) || (asc > 64 && asc < 91)) {
                pirateInitial = Character.toString(playerName.charAt(x)).toUpperCase();
                if (asc > 96 && asc < 123) {nameIsLower = true;}
                break;
            }
        }

        // This is a bit complicated of a system just to generate a silly pirate name, but I wanted to keep things fun
        // by having a variety of names and reducing the chances of names appearing over and over and over again.
        // There are 3 types of pirate names that we can generate:
        //      <title> <playerName> <lastname>, eg: Pesky <playerName> Pete.
        //          title and lastname will be pulled from tblPirateNamesDB where fnNameType = 1 (titles) and fnNameType = 2 (last names)
        //      <2-word title> <playerName>, eg: Apple Pickin' <playerName>.  2-word titles are fnNameType = 3 
        //      <playerName> <suffix>, eg: <playerName> the Heartless.   Suffixes are fnNameType = 4
        // Randomly selected names are based on the players first initial so that it flows nicely, except for
        // the suffixes (fnNameType 4), which can be assigned to any player.  To select a random name (and name style)
        // from the db, we do a select() of all possible names that match the characters initial, or are fnNameType 4.
        // We'll order that resultset by rand() and use the fnNameType in the first row returned, unless it is an
        // fnNameType 4 (which we'll skip 80% of the time.  These names and this type of name should be fairly uncommon).
        // If the row we're going to use is an fnNameType of 1 or 2, we'll continue cycling through the resultset
        // until we have both an fnNameType 1 AND 2 to use for the name.  This method makes several assumptions: that
        // tblPirateNamesDB will always have at least ONE fnNameType 1 and fnNameType 2 for every character in the
        // alphabet (A-Z) and will always have at least ONE fnNameType 4 to be used for players whos names are made up
        // of special characters and have no A-Z at all.

        // strip the <ZH> and <ER> tags off of players' pirate names
        playerName = playerName.replace(" <ZH>","");
        playerName = playerName.replace(" <ER>","");

        // strip out other special characters
        playerName = playerName.replaceAll("[\\-._^]","");

        try {
            ResultSet rp = m_botAction.SQLQuery(m_database,"SELECT fcName,fnNameType FROM `tblPirateNamesDB` WHERE fcIndex1 = '" + pirateInitial + "' or fcIndex2 = '" + pirateInitial + "' or fnNameType = 4 order by rand()");
            if (rp == null) {fatal("Error reading tblPirateNamesDB: NULL resultset returned.");}
            int odds;
            int nameType = 0;
            String nameStart = "", nameEnd = "";

            while (rp.next()) {
                if (nameType == 0) {
                    // we have yet to determine the type of name, do so now
                    if (rp.getInt("fnNameType") == 4) {
                        // if the player has a valid initial (A-Z), we'll use this name only 20% of the time
                        odds = (int)(10 * Math.random()) + 1; // random number 1-10, inclusive
                        if (odds < 9 && pirateName != "-") {
                            // skip to the next name/name type
                            continue;
                        } else {
                            nameEnd = rp.getString("fcName");
                            nameType = 4;
                        }
                    } else {
                        nameType = rp.getInt("fnNameType");
                        if (nameType == 3 || nameType == 1) {
                            nameStart = rp.getString("fcName");
                        } else {
                            nameEnd = rp.getString("fcName");
                        }
                    }
                }

                // if nameType is 3 or 4, we're done
                if (nameType == 3 || nameType == 4) {break;}

                // otherwise, nameType is 1 or 2, and we need both a title and a last name before we're done
                if (nameType == 1 && rp.getInt("fnNameType") == 2) {
                    nameEnd = rp.getString("fcName");
                    break;
                } else if (nameType == 2 && rp.getInt("fnNameType") == 1) {
                    nameStart = rp.getString("fcName");
                    break;
                }
            }
            m_botAction.SQLClose(rp);
            pirateName = nameStart + " " + playerName + " " + nameEnd;
            pirateName = pirateName.trim();
            if (nameIsLower) {pirateName = pirateName.toLowerCase();}
        } catch (SQLException e) {fatal("Error selecting from tblPirateNamesDB: " + e.getMessage());}
        return pirateName;
    }

    public int createPlayer(String playerName, String pirateName) {
        int newID = 0;
        try {
            ResultSet r = m_botAction.SQLQuery(m_database,"INSERT into tblPiratePlayers(fcPlayerName,fcPirateName) values('" + playerName.replace("'","\'") + "','" + Tools.addSlashes(pirateName) + "')");
            if (r == null) {fatal("Error inserting player: NULL resultset returned.");}
            if (r.next()) {
                newID = r.getInt(1);
                m_botAction.SQLClose(r);
            }
        } catch (SQLException e) {fatal("Error creating player: " + playerName);}
        return newID;
    }
    
    public int createGame() {
        int newID = 0;
        try {
            ResultSet r = m_botAction.SQLQuery(m_database,"INSERT into tblPirateGames(fcHost,fdDate) values('" + m_host.replace("'","\'") + "',now())");
            if (r == null) {fatal("Error inserting game: NULL resultset returned.");}
            if (r.next()) {
                newID = r.getInt(1);
                m_botAction.SQLClose(r);
            }
        } catch (SQLException e) {fatal("Error creating game: " + e.getMessage());}
        return newID;
    }

    public String getPlural(Integer intCount,String strWord) {
        if (intCount > 1 || intCount == 0) {strWord += "s";}
        return intCount + " " + strWord;
    }
    
    private class House {
        private int HouseID;
        private int CoinsPaid;
        private String PlayerName;
        private String PirateName;
        private boolean hasChanged;

        private House(int HouseID, String PlayerName, String PirateName, int CoinsPaid) {
            this.HouseID = HouseID;
            this.PlayerName = PlayerName;
            this.PirateName = PirateName;
            this.CoinsPaid = CoinsPaid;
        }
        
        private int getHouseID() {return HouseID;}
        private String getOwner() {return PlayerName;}
        private String getOwnerString() {
            if (PlayerName == null) {return "none";} else {return PlayerName;}
        }
        private String getOwnerPirateName() {return PirateName;}
        private int getCoinsPaid() {return CoinsPaid;}
        private int getPrice() {
            if (CoinsPaid == 0) {return HOUSEPRICE_DEFAULT;} else {return (int)(CoinsPaid+(CoinsPaid*HOUSEPRICE_INCREMENT));}
        }
        private boolean saveToDB() {
            if (hasChanged) {
                try {
                    m_botAction.SQLQueryAndClose(m_database,"UPDATE tblPirateHouses set fnPlayerID=" + m_players.get(PlayerName).getPlayerID() + ",fnCoinsPaid=" + CoinsPaid + " where fnHouseID=" + HouseID);
                } catch (SQLException e) {fatal("Error updating house: " + e.getMessage());}
                hasChanged = false;
                return true;
            } else {return false;}
        }
        private void setCoinsPaid(int CoinsPaid) {this.CoinsPaid = CoinsPaid; hasChanged = true;}
        private void setOwner(String PlayerName, String PirateName) {
            this.PlayerName = PlayerName;
            this.PirateName = PirateName;
            hasChanged = true;
        }
    }
    
    private class BankRecord {
        private int PlayerID;
        private int EntryID;
        private String EntryDate;
        private int BuyerID;
        private int SellerID;
        private int PlotID;
        private int HouseID;
        private int OriPrice;
        private int NewPrice;
        private String BonusBy;
        private int BonusAmount;
        private String BonusReason;
        private Boolean isNew;
        
        private BankRecord(int PlayerID, int EntryID, String EntryDate, int BuyerID, int SellerID, int PlotID, int HouseID, int OriPrice, int NewPrice, String BonusBy, int BonusAmount, String BonusReason, Boolean isNew) {
            this.PlayerID = PlayerID;
            this.EntryID = EntryID;
            this.EntryDate = EntryDate;
            this.BuyerID = BuyerID;
            this.SellerID = SellerID;
            this.PlotID = PlotID;
            this.HouseID = HouseID;
            this.OriPrice = OriPrice;
            this.NewPrice = NewPrice;
            this.BonusBy = BonusBy;
            this.BonusAmount = BonusAmount;
            this.BonusReason = BonusReason;
            this.isNew = isNew;
        }
        private void saveToDB() {
            if (isNew) {
                try {
                    m_botAction.SQLQueryAndClose(m_database,"INSERT INTO tblPirateBankRecords(fdDate,fnBuyerID,fnSellerID,fnPlotID,fnHouseID,fnOriPrice,fnNewPrice,fcBonusBy,fnBonusAmount,fcBonusReason) values(now()," + BuyerID + "," + SellerID + "," + PlotID + "," + HouseID + "," + OriPrice + "," + NewPrice + ",'" + BonusBy + "'," + BonusAmount + ",'" + BonusReason + "')");
                } catch (SQLException e) {fatal("Error updating bank record: " + e.getMessage());}
                isNew = false;
            }
        }
        private String getDate() {
            if (EntryDate == "") {return "TODAY";} else {return EntryDate;}
        }
        private String getTrans() {
            if (BonusAmount > 0) {
                return BonusReason;
            } else if (PlotID == 0) {
                return "House #" + HouseID;
            } else {
                return "Plot " + m_plots.get(PlotID).getCaption();
            }
        }
        private String getType() {
            if (BonusAmount > 0) {
                return "BONUS";
            } else {
                if (PlayerID == SellerID) {return "SELL";} else {return "BUY";}
            }
        }
        private String getAmount() {
            if (BonusAmount > 0) {
                // bonus
                return "+" + BonusAmount;
            } else {
                if (PlayerID == SellerID) {
                    return "+" + OriPrice;
                } else {
                    return "-" + NewPrice;
                }
            }
        }
    }

    private class PiratePlotEarnings {
        private int PlayerID;
        private int PlotID;
        private int Earnings;
        private int newEarnings;
        private boolean hasChanged;
        
        private PiratePlotEarnings(int PlayerID, int PlotID, int Earnings) {
            this.PlayerID = PlayerID;
            this.PlotID = PlotID;
            this.Earnings = Earnings;
            this.newEarnings = 0;
            hasChanged = false;
        }
        private void addEarnings(int addAmount) {
            this.Earnings += addAmount;
            this.newEarnings += addAmount;
            this.hasChanged = true;
        }
        private void saveToDB() {
            if (hasChanged) {
                try {
                    m_botAction.SQLQueryAndClose(m_database,"UPDATE tblPirateEarnings set fnEarned=" + Earnings + " where fnPlotID=" + PlotID + " and fnPlayerID=" + PlayerID);
                } catch (SQLException e) {fatal("Error updating plot: " + e.getMessage());}
                hasChanged = false;
            }
        }
    }

    private class PlotPoint {
        private int PlotX;
        private int PlotY;
        private PlotPoint(int PlotX, int PlotY) {
            this.PlotX = PlotX;
            this.PlotY = PlotY;
        }
        private int getX() {return PlotX;}
        private int getY() {return PlotY;}
    }
    
    
    private class Plot {
        private int PlotID;
        private int PlotX;
        private int PlotY;
        private String PlayerName;
        private String PirateName;
        private int CoinsPaid;
        private boolean hasChanged;
        private String Addr;
        private int Earnings;
        private int newEarnings;

        private Plot(int PlotID, int PlotX, int PlotY, String PlayerName, String PirateName, int CoinsPaid, int Earnings) {
            this.PlotID = PlotID;
            this.PlotX = PlotX;
            this.PlotY = PlotY;
            this.PlayerName = PlayerName;
            this.PirateName = PirateName;
            this.CoinsPaid = CoinsPaid;
            hasChanged = false;
            this.Addr = (char)(PlotX+65) + "" + (PlotY+1);
            this.Earnings = Earnings;
            this.newEarnings = 0;
        }
        
        private String getOwner() {return PlayerName;}
        private String getOwnerString() {
            if (PlayerName == null) {return "none";} else {return PlayerName;}
        }
        private String getOwnerPirateName() {return PirateName;}
        private int getPlotX() {return PlotX;}
        private int getPlotY() {return PlotY;}
        private int getCoinsPaid() {return CoinsPaid;}
        private int getPrice() {
            if (CoinsPaid == 0) {return PRICE_DEFAULT;} else {return (int)(CoinsPaid+(CoinsPaid*PRICE_INCREMENT));}
        }
        private boolean saveToDB() {
            if (hasChanged) {
                // when a plot is sold, the owner is set to null (in java).  For the db, we want to set the field to 0.
                int playerID = 0;
                if (PlayerName != null) {playerID = m_players.get(PlayerName).getPlayerID();}
                try {
                    m_botAction.SQLQueryAndClose(m_database,"UPDATE tblPiratePlots set fnPlayerID=" + playerID + ",fnCoinsPaid=" + CoinsPaid + ",fnEarned=" + Earnings + " where fnPlotID=" + PlotID);
                } catch (SQLException e) {fatal("Error updating plot: " + e.getMessage());}
                hasChanged = false;
                return true;
            } else {return false;}
        }
        private void setCoinsPaid(int CoinsPaid) {this.CoinsPaid = CoinsPaid; hasChanged = true;}
        private void setOwner(String PlayerName, String PirateName) {
            this.PlayerName = PlayerName;
            this.PirateName = PirateName;
            hasChanged = true;
        }
        private String getCaption() {
            if (m_specials.get(Addr) != null) {return m_specials.get(Addr);} else {return Addr;}
        }
        private String getAddr() {return Addr;}
        private void addEarnings(int addAmount) {
            this.Earnings += addAmount;
            this.newEarnings += addAmount;
            this.hasChanged = true;
        }
        private int getEarnings() {return Earnings;}
        private int getNewEarnings() {return newEarnings;}
    }

    private class ScoreSet {
        private String playerName;
        private int freq0Kills;
        private int freq1Kills;
        private int kills;
        private int flagTouches;
        private int flagCaptures;
        private int foundCoins;
        private int bountyCoins;
        private int taxCoins;
        private int deaths;

        private ScoreSet(String PlayerName) {
            this.playerName = PlayerName;
            freq0Kills = 0;
            freq1Kills = 0;
            kills = 0;
            flagTouches = 0;
            flagCaptures = 0;
            foundCoins = 0;
            bountyCoins = 0;
            taxCoins = 0;
            deaths = 0;
        }
    }

    private class Pirate {
        private int PlayerID;
        private String PlayerName;
        private String PirateName;
        private int origCoins;
        private int foundCoins;
        private int bountyCoins;
        private int taxCoins;
        private int otherCoins;
        private List<Integer> arrPlots;
        private List<Integer> arrHouses;
        private Vector<BankRecord> m_bankrecords;
        private HashMap<Integer,PiratePlotEarnings> m_earnings;
        boolean pirateNameChanged;
        boolean earningsChanged;

        private Pirate(int PlayerID, String PlayerName, String PirateName, int Coins, List<Integer> arrPlots, List<Integer> arrHouses, HashMap<Integer,PiratePlotEarnings> m_earnings, Vector<BankRecord> m_bankrecords) {
            this.PlayerID = PlayerID;
            this.PlayerName = PlayerName;
            this.PirateName = PirateName;
            this.origCoins = Coins;
            this.arrPlots = arrPlots;
            this.arrHouses = arrHouses;
            this.m_bankrecords = m_bankrecords;
            this.m_earnings = m_earnings;
            this.foundCoins = 0;
            this.bountyCoins = 0;
            this.taxCoins = 0;
            this.otherCoins = 0;
            pirateNameChanged = false;
            earningsChanged = false;
        }

        private void addRecord(BankRecord m_record) {m_bankrecords.add(m_record);}
        private int getPlayerID() {return PlayerID;}
        private String getPlayerName() {return PlayerName;}
        private String getPirateName() {return PirateName;}
        private void setPirateName(String PirateName) {this.PirateName = PirateName; pirateNameChanged = true;}
        private int getCoins() {return (origCoins+foundCoins+bountyCoins+taxCoins+otherCoins);}
        private int getFoundCoins() {return foundCoins;}
        private int getBountyCoins() {return bountyCoins;}
        private int getTaxCoins() {return taxCoins;}
        private List<Integer> getPlots() {return arrPlots;}
        private List<Integer> getHouses() {return arrHouses;}
        private Vector<BankRecord> getRecords() {return m_bankrecords;}
        private void addPlot(int plotID) {
            arrPlots.add(plotID);
            if (m_earnings.get(plotID) == null) {
                try {
                    m_botAction.SQLQueryAndClose(m_database,"INSERT into tblPirateEarnings(fnPlotID,fnPlayerID) values(" + plotID + "," + PlayerID + ");");
                } catch (SQLException e) {fatal("Error inserting plot earnings for player: " + PlayerID + ", plot: " + plotID + ".  Error: " + e.getMessage());}
                m_earnings.put(plotID,new PiratePlotEarnings(PlayerID,plotID,0));
            }
        }
        private void removePlot(int plotID) {arrPlots.remove(arrPlots.indexOf(plotID));}
        private int getPlotCount() {return arrPlots.size();}
        private String getPlotList() {
            String strPlots = "";
            int plotID;
            Iterator<Integer> li = arrPlots.listIterator();

            while (li.hasNext()) {
                plotID = li.next();
                strPlots += " " + m_plots.get(plotID).getCaption();
            }
            return strPlots.trim();
        }
        private void addHouse(int houseID) {arrHouses.add(houseID);}
        private void removeHouse(int houseID) {arrHouses.remove(arrHouses.indexOf(houseID));}
        private int getHouseCount() {return arrHouses.size();}
        private String getHouseList() {
            String strHouses = "";
            Iterator<Integer> li = arrHouses.listIterator();
            while (li.hasNext()) {strHouses += " " + li.next();}
            return strHouses.trim();
        }
        private void addFoundCoins(int newCoins) {
            foundCoins += newCoins;
            m_scores.get(PlayerName).foundCoins += newCoins;
        }
        private void addBountyCoins(int newCoins) {
            bountyCoins += newCoins;
            m_scores.get(PlayerName).bountyCoins += newCoins;
        }
        private void addTaxCoins(int plotID, int newCoins) {
            taxCoins += newCoins;
            m_earnings.get(plotID).addEarnings(newCoins);
            m_scores.get(PlayerName).taxCoins += newCoins;
        }
        private void addCoins(int coins) {
            otherCoins += coins;
        }
        private void removeCoins(int coins) {otherCoins = otherCoins - coins;}
        private void saveToDB() {
            if (pirateNameChanged) {
                try {
                    m_botAction.SQLQueryAndClose(m_database,"UPDATE tblPiratePlayers set fcPirateName='" + PirateName.replace("'","\'") + "' where fnPlayerID=" + PlayerID);
                } catch (SQLException e) {fatal("Error updating player: " + e.getMessage());}
            }
            
            // if any of our plots earnings have changed, cycle through them and call saveToDB on each.  For any
            // that have changed, an update statement will be done.
            if (earningsChanged) {
                for (PiratePlotEarnings p : m_earnings.values()) {p.saveToDB();}
                earningsChanged = false;
            }

            // saveToDB called for each bank record.  If the bank record is new, it will be inserted into the db. 
            Iterator<BankRecord> it = m_bankrecords.iterator();
            while (it.hasNext()) {
                BankRecord r = it.next();
                r.saveToDB();
            }

            try {
                m_botAction.SQLQueryAndClose(m_database,"UPDATE tblPirateStats set fnFoundCoins=" + foundCoins + ",fnBountyCoins=" + bountyCoins + ",fnTaxCoins=" + taxCoins + ",fnOtherCoins=" + otherCoins + ",fnEndPlots=" + arrPlots.size() + " where fnPlayerID=" + PlayerID + " and fnGameID=" + m_gameid);
            } catch (SQLException e) {fatal("Error updating player stats: " + e.getMessage());}
        }
    }
}
