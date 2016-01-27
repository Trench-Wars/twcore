package twcore.bots.multibot.hunt;

import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.StringBag;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerEntered;
import twcore.core.events.FrequencyShipChange;
import twcore.core.game.Player;

/**
    Complete hunt make-over.

    @author milosh
*/
public class hunt extends MultiModule {

    public boolean isRunning = false;

    public int deaths = 99;
    public int preyKillPoints = 3;
    public int hunterKillPoints = 1;
    public int nonHunterPoints = -2;

    public Vector<HuntFreq> freqs;
    public Vector<HuntPlayer> mvpPlayers;

    public String[] opmsg = {
        "!starthunt  - starts a game of hunt",
        "!stop       - stops the game",
        "!huntspec # - sets non-hunted death limit. (Default:99)",
        "!prey #     - sets the point reward for a successful hunt. (Default:3)",
        "!hunter #   - sets the point reward for killing your hunter. (Default:1)",
        "!other #    - sets the point reward for killing other players. (Default: -2)"
    };
    public String[] helpmsg = {
        "!prey      - shows your current prey.",
        "!help      - displays this"
    };

    /**
        Initializes.
    */
    public void init() {
        freqs = new Vector<HuntFreq>();
        mvpPlayers = new Vector<HuntPlayer>();
        m_botAction.setReliableKills(1);
    }

    // //////////
    // EVENTS////
    // //////////
    /**
        Requests events for this bot to handle.
    */
    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.PLAYER_DEATH);
        events.request(this, EventRequester.PLAYER_LEFT);
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(this, EventRequester.PLAYER_ENTERED);
    }

    /**
        Handles messaging.
    */
    public void handleEvent(Message event) {
        int messageType = event.getMessageType();
        String message = event.getMessage();
        String name = m_botAction.getPlayerName(event.getPlayerID());

        if (messageType == Message.PRIVATE_MESSAGE && opList.isER(name)) {
            handleModCommands(name, message);
        }

        if (messageType == Message.PRIVATE_MESSAGE) {
            handlePubCommands(name, message);
        }
    }

    /**
        Exectued in the event of a player death.
    */
    public void handleEvent(PlayerDeath event) {
        if (!isRunning)
            return;

        String killer = m_botAction.getPlayerName(event.getKillerID());
        String killed = m_botAction.getPlayerName(event.getKilleeID());
        HuntFreq hfKiller = getHuntFreq(event.getKillerID());
        HuntFreq hfKilled = getHuntFreq(event.getKilleeID());

        if (killer == null || killed == null || hfKiller == null || hfKilled == null)
            return;

        hfKilled.getHuntPlayer(event.getKilleeID()).addDeath();

        if (hfKiller.getPrey().equals(killed)) {
            m_botAction.specWithoutLock(event.getKilleeID());
            int points = hfKilled.getHuntPlayer(killed).getPoints();
            m_botAction.sendArenaMessage(killed + " has been hunted by " + killer + " and is out! (" + points + " point(s))");
            mvpPlayers.add(hfKilled.getHuntPlayer(killed));
            hfKilled.remove(event.getKilleeID());
            hfKiller.getHuntPlayer(killer).addPoints(preyKillPoints);
            hfKiller.setPrey();
            return;
        }

        if (hfKilled.getPrey().equals(killer)) {
            m_botAction.sendSmartPrivateMessage(killer, "You killed your hunter! (" + hunterKillPoints + " point(s))");
            hfKiller.getHuntPlayer(killer).addPoints(hunterKillPoints);
        } else {
            m_botAction.sendSmartPrivateMessage(killer, "You killed an innocent bystander! (" + nonHunterPoints + " point(s))");
            hfKiller.getHuntPlayer(killer).addPoints(nonHunterPoints);
        }

        if (hfKilled.getHuntPlayer(event.getKilleeID()).getLosses() == deaths) {
            mvpPlayers.add(hfKilled.getHuntPlayer(killed));
            m_botAction.sendArenaMessage(killed + " is out! (" + hfKilled.getHuntPlayer(killed).getPoints() + " point(s))");
            m_botAction.specWithoutLock(killed);
            hfKilled.remove(event.getKilleeID());

            if (killed.equals(hfKilled.getHunterFreq().getPrey())) {
                hfKilled.getHunterFreq().setPrey();
            }
        }

    }

    /**
        If the host forgot to lock the arena. Make sure no one can sneak into the
        game.
    */
    public void handleEvent(PlayerEntered event) {
        m_botAction.specWithoutLock(event.getPlayerID());
    }

    /**
        If someone leaves the arena or lags out while playing, they are removed.
    */
    public void handleEvent(PlayerLeft event) {
        if (!isRunning)
            return;

        HuntFreq hf = getHuntFreq(event.getPlayerID());

        if (hf == null)
            return;

        HuntPlayer hp = hf.getHuntPlayer(event.getPlayerID());
        mvpPlayers.add(hp);
        m_botAction.sendArenaMessage(hp.getPlayerName() + " is out! (" + hp.getPoints() + " point(s))");
        hf.remove(hp);

        if (hp.getPlayerName().equals(hf.getHunterFreq().getPrey())) {
            hf.getHunterFreq().setPrey();
        }
    }

    /**
        This method ensures reliability even if the arena isn't locked. People
        not in the game are not allowed to enter. People who are in the game but
        decide to spectate, change frequencies or change ships are removed.
    */
    public void handleEvent(FrequencyShipChange event) {
        if (!isRunning)
            return;

        m_botAction.specWithoutLock(event.getPlayerID());
        HuntFreq hf = getHuntFreq(event.getPlayerID());

        if (hf == null)
            return;

        HuntPlayer hp = hf.getHuntPlayer(event.getPlayerID());
        mvpPlayers.add(hp);
        m_botAction.sendArenaMessage(hp.getPlayerName() + " is out! (" + hp.getPoints() + " point(s))");
        hf.remove(hp);

        if (hp.getPlayerName().equals(hf.getHunterFreq().getPrey())) {
            hf.getHunterFreq().setPrey();
        }
    }

    // //////////
    // COMMANDS//
    // //////////
    /**
        Commands that only Moderators can use.
    */
    public void handleModCommands(String sender, String cmd) {
        if (cmd.equalsIgnoreCase("!starthunt"))
            doStartCmd(sender);
        else if (cmd.equalsIgnoreCase("!stop"))
            doStopCmd(sender);
        else if (cmd.startsWith("!huntspec "))
            huntSpec(sender, cmd.substring(10));
        else if (cmd.startsWith("!prey "))
            preyReward(sender, cmd.substring(6));
        else if (cmd.startsWith("!hunter "))
            hunterReward(sender, cmd.substring(8));
        else if (cmd.startsWith("!other "))
            nonReward(sender, cmd.substring(7));
    }

    /**
        Commands that anyone can use.
    */
    public void handlePubCommands(String sender, String cmd) {
        if (cmd.equalsIgnoreCase("!prey"))
            doTellPreyCmd(sender);
        else if (cmd.equalsIgnoreCase("!help"))
            doHelpCmd(sender);
    }

    /**
        Give basic help info to anyone who asks for it. Use modhelp for more
        information.

        @param name
    */
    public void doHelpCmd(String name) {
        m_botAction.smartPrivateMessageSpam(name, helpmsg);
    }

    // //////////
    // GAME//////
    // //////////
    /**
        Starts the game. See comments within this method for a more detailed
        description of how it operates.
    */
    public void doStartCmd(String name) {
        if (isRunning) {
            m_botAction.sendSmartPrivateMessage(name, "Hunt is already running.");
            return;
        }

        isRunning = true;
        m_botAction.sendArenaMessage("Hunt mode activated by " + name + ". Type :" + m_botAction.getBotName() + ":!prey if you forget who you are hunting.");
        m_botAction.sendArenaMessage("Removing players with " + deaths + " non-hunted deaths.");

        m_botAction.scoreResetAll();
        m_botAction.shipResetAll();
        freqs.clear();
        mvpPlayers.clear();// Start with a clean slate.

        int x = 0;

        for (int i = 0; i < 10000; i++) {
            int size = m_botAction.getFrequencySize(i);

            if (size > 0) {
                freqs.add(x, new HuntFreq(i, x));// Find existing frequencies
                // and add them to the
                // frequency Vector.
                x++;
            }
        }

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

        if (!i.hasNext()) {
            m_botAction.sendSmartPrivateMessage(name, "Not enough players.");
            doStopCmd(name);// If everyone is in spec... The game can't be
            // played.
            return;
        }

        while (i.hasNext()) {
            Player p = i.next();

            for (int z = 0; z < freqs.size(); z++) {
                if (freqs.elementAt(z).getFreq() == p.getFrequency()) {
                    freqs.elementAt(z).add(new HuntPlayer(p));// Add
                    // frequencies
                    // that have
                    // playing
                    // players to
                    // the freqs
                    // Vector
                    break;
                }
            }
        }

        Iterator<HuntFreq> it = freqs.iterator();

        while (it.hasNext()) {
            HuntFreq f = it.next();

            if (f.size() < 1) {
                it.remove();// If there are frequencies without any players(only
                // spectators) remove them. (Spectators in other
                // frequencies are unimportant as they don't exist
                // in the HuntFreq)
            }
        }

        if (freqs.size() == 1) {
            freqs.clear();
            Iterator<Player> ppi = m_botAction.getPlayingPlayerIterator();
            int z = 0;

            while (ppi.hasNext()) {
                freqs.add(z, new HuntFreq(z, z));
                freqs.elementAt(z).add(new HuntPlayer(ppi.next()));
                z++;
            }

            if (freqs.size() < 2) {
                m_botAction.sendSmartPrivateMessage(name, "Not enough players.");
                doStopCmd(name);
                return;
            }
        }

        updateIndices();
        Iterator<HuntFreq> it2 = freqs.iterator();

        while (it2.hasNext()) {
            HuntFreq h = it2.next();
            h.setPrey();// Tell everyone who they are hunting.
            h.setInitialSize();// Record the initial size of this frequency
        }

        // We now have all HuntFreqs with their HuntPlayers created and preys
        // assigned.

    }

    /**
        Stops the game whilst in progress. Cleans up public Vector Objects.

        @param name
    */
    public void doStopCmd(String name) {
        if (!isRunning) {
            m_botAction.sendSmartPrivateMessage(name, "Hunt is not currently running.");
            return;
        } else {
            freqs.clear();
            mvpPlayers.clear();
            m_botAction.sendArenaMessage("Game stopped. -" + name);
            isRunning = false;
        }

    }

    /**
        Displays winner(s) and MVP(s). Cleans up public Vector Objects.

        @param h
    */
    public void end(HuntFreq h) {
        mvpPlayers.addAll(h.players);
        m_botAction.sendArenaMessage("Game over!", 5);

        if (h.size() == 1)
            m_botAction.sendArenaMessage("Winner: " + h.toString());
        else if (h.size() > 1)
            m_botAction.sendArenaMessage("Frequency " + h.getFreq() + " wins! Survivors: " + h.toString());

        m_botAction.sendArenaMessage(getMVP());
        m_botAction.toggleLocked();
        freqs.clear();
        mvpPlayers.clear();
        isRunning = false;
    }

    /**
        A registered command. Changes the number of points a player receives for
        killing their prey. This can only be done when the game is not running.
    */
    public void preyReward(String name, String msg) {
        boolean badmsg = false;
        int x = 0;

        try {
            x = Integer.parseInt(msg);
        } catch (Exception e) {
            badmsg = true;
        }

        if (!isRunning && !badmsg) {
            preyKillPoints = x;
            m_botAction.sendSmartPrivateMessage(name, "Players will receive " + x + " point(s) for a successful hunt.");
        } else if (isRunning) {
            m_botAction.sendSmartPrivateMessage(name, "Please modify point rewards before you start the game.");
        } else if (badmsg) {
            m_botAction.sendSmartPrivateMessage(name, "Command Format: !prey #");
        }
    }

    /**
        A registered command. Changes the number of points a player receives for
        killing their hunter. This can only be done when the game is not running.
    */
    public void hunterReward(String name, String msg) {
        boolean badmsg = false;
        int x = 0;

        try {
            x = Integer.parseInt(msg);
        } catch (Exception e) {
            badmsg = true;
        }

        if (!isRunning && !badmsg) {
            hunterKillPoints = x;
            m_botAction.sendSmartPrivateMessage(name, "Players will receive " + x + " point(s) for killing their hunter.");
        } else if (isRunning) {
            m_botAction.sendSmartPrivateMessage(name, "Please modify point rewards before you start the game.");
        } else if (badmsg) {
            m_botAction.sendSmartPrivateMessage(name, "Command Format: !hunter #");
        }
    }

    /**
        A registered command. Changes the number of points a player receives for
        killing innocent bystanders. This can only be done when the game is not
        running.
    */
    public void nonReward(String name, String msg) {
        boolean badmsg = false;
        int x = 0;

        try {
            x = Integer.parseInt(msg);
        } catch (Exception e) {
            badmsg = true;
        }

        if (!isRunning && !badmsg) {
            nonHunterPoints = x;
            m_botAction.sendSmartPrivateMessage(name, "Players will receive " + x + " point(s) for killing other players.");
        } else if (isRunning) {
            m_botAction.sendSmartPrivateMessage(name, "Please modify point rewards before you start the game.");
        } else if (badmsg) {
            m_botAction.sendSmartPrivateMessage(name, "Command Format: !other #");
        }
    }

    /**
        A registered command. Changes the number of non-hunted deaths players
        have before being removed from the game. This can only be done when the
        game is not running.
    */
    public void huntSpec(String name, String msg) {
        boolean badmsg = false;
        int x = 0;

        try {
            x = Integer.parseInt(msg);

            if (x < 0)
                throw new Exception();
        } catch (Exception e) {
            badmsg = true;
        }

        if (!isRunning && !badmsg) {
            deaths = x;
            m_botAction.sendSmartPrivateMessage(name, "Non-hunted death limit set to: " + x);
        } else if (isRunning) {
            m_botAction.sendSmartPrivateMessage(name, "Please make changes before you start the game.");
        } else if (badmsg) {
            m_botAction.sendSmartPrivateMessage(name, "Command Format: !huntspec # (# must be positive)");
        }
    }

    /**
        Tells the specified player who he/she is currently hunting.

        @param name -
                  The name of the player to find the prey of.
    */
    public void doTellPreyCmd(String name) {
        HuntFreq hf;

        for (int i = 0; i < freqs.size(); i++) {
            hf = freqs.elementAt(i);
            HuntPlayer p = hf.getHuntPlayer(name);

            if (p != null) {
                m_botAction.sendSmartPrivateMessage(name, "You are currently hunting " + hf.getPrey() + ".");
                return;
            }
        }

        m_botAction.sendSmartPrivateMessage(name, "You are not playing!");
    }

    /**
        Finds the frequency a player is on and then returns the HuntFreq Object
        for that frequency.

        @param playerID -
                  The ID of the player you wish to find the frequency of.
        @return HuntFreq Object for the player parameter.
    */
    public HuntFreq getHuntFreq(int playerID) {
        for (int i = 0; i < freqs.size(); i++) {
            if (freqs.elementAt(i).getHuntPlayer(playerID) != null)
                return freqs.elementAt(i);
        }

        return null;
    }

    /**
        Tells each HuntFreq object what its index in the main freqs Vector is.
    */
    public void updateIndices() {
        for (int i = 0; i < freqs.size(); i++) {
            freqs.elementAt(i).setIndex(i);
        }
    }

    /**
        Returns a string of the HuntPlayer with the most points in the format
        "MVP: [PlayerName] (# point(s))!" If more than one players share the high
        score it displays them all.

        @return
    */
    public String getMVP() {
        int mvpPoints = 0;
        String mvp = null;
        ArrayList<String> multiMvps = new ArrayList<String>();
        Iterator<HuntPlayer> i = mvpPlayers.iterator();

        while (i.hasNext()) {
            HuntPlayer p = i.next();

            if (p.getPoints() > mvpPoints) {
                mvpPoints = p.getPoints();
                mvp = p.getPlayerName();
            }
        }

        Iterator<HuntPlayer> it = mvpPlayers.iterator();

        while (it.hasNext()) {
            HuntPlayer p = it.next();

            if (p.getPoints() == mvpPoints)
                multiMvps.add(p.getPlayerName() + " (" + p.getPoints() + ")");
        }

        if (mvp == null) {
            return "MVP: None. No one had positive points.";
        }

        if (multiMvps.size() > 1) {
            return "MVPs: " + multiMvps.toString();
        }

        return "MVP: " + mvp + " (" + mvpPoints + " point(s))!";
    }

    /**
        Required methods.
    */
    public String[] getModHelpMessage() {
        return opmsg;
    }

    public boolean isUnloadable() {
        if (isRunning)
            return false;

        return true;
    }

    public void cancel() {}

    // ////////////////////////////////////////////
    // ///////PRIVATE CLASSES//////////////////////
    // ////////////////////////////////////////////
    /**
        An object that represents a team of hunt player(s) whether plural or singular.
    */
    private class HuntFreq {

        private Vector<HuntPlayer> players;
        public int freq, index, initialSize;
        public String prey;

        /**
            HuntFreq constructor.
            @param freq - The frequency number.
            @param index - The index of this object in the freqs Vector\<HuntFreq\>
        */
        public HuntFreq(int freq, int index) {
            this.freq = freq;
            this.index = index;
            players = new Vector<HuntPlayer>();
        }

        /**
            Returns a HuntPlayer Object by the player's name.

            @param playerName
            @return
        */
        public HuntPlayer getHuntPlayer(String playerName) {
            for (int i = 0; i < players.size(); i++) {
                if (players.elementAt(i).getPlayerName().equals(playerName))
                    return players.elementAt(i);
            }

            return null;
        }

        /**
            Returns a HuntPlayer Object by the player's ID.

            @param playerID
            @return
        */
        public HuntPlayer getHuntPlayer(int playerID) {
            for (int i = 0; i < players.size(); i++) {
                if (players.elementAt(i).getPlayerID() == playerID)
                    return players.elementAt(i);
            }

            return null;
        }

        /**
            Adds a HuntPlayer Object to this Object

            @param p
        */
        public void add(HuntPlayer p) {
            players.add(p);
        }

        /**
            Removes a HuntPlayer Object from this HuntFreq by playerID

            @param id -
                      the playerID
        */
        public void remove(int id) {
            for (int i = 0; i < players.size(); i++) {
                if (players.elementAt(i).getPlayerID() == id)
                    players.remove(i);
            }
        }

        /**
            Removes a HuntPlayer Object from this HuntFreq

            @param hp
        */
        public void remove(HuntPlayer hp) {
            players.remove(hp);
        }

        /**
            Returns the frequency number of this HuntFreq.

            @return
        */
        public int getFreq() {
            return freq;
        }

        /**
            Changes local knowledge of this frequency's index to the parameter.

            @param index
        */
        public void setIndex(int index) {
            this.index = index;
        }

        /**
            Records the initial size of this HuntFreq for later reference.
        */
        public void setInitialSize() {
            initialSize = players.size();
        }

        /**
            Returns the size of this frequency.

            @return
        */
        public int size() {
            return players.size();
        }

        /**
            Returns the name of the person this frequency is hunting.

            @return
        */
        public String getPrey() {
            return prey;
        }

        /**
            Returns the HuntFreq Object of the frequency this one is hunting.

            @return
        */
        public HuntFreq getPreyFreq() {
            if (index == 0)
                return freqs.lastElement();

            return freqs.elementAt(index - 1);
        }

        /**
            Returns the HuntFreq Object of the frequency hunting this one.

            @return
        */
        public HuntFreq getHunterFreq() {
            if (index == freqs.lastIndexOf(freqs.lastElement()))
                return freqs.elementAt(0);

            return freqs.elementAt(index + 1);
        }

        /**
            Changes the prey for this frequency to a random person on the
            frequency they are hunting. If no players are found then the prey
            frequency is removed and it re-runs. If this freq is the last
            frequency remaining this ends the game.
        */
        public void setPrey() {
            String addPlayerName;
            StringBag randomPlayerBag = new StringBag();
            Iterator<HuntPlayer> i = getPreyFreq().players.iterator();

            if (i.hasNext()) {
                while (i.hasNext()) {
                    addPlayerName = i.next().getPlayerName();
                    randomPlayerBag.add(addPlayerName);
                }

                prey = randomPlayerBag.grabAndRemove();
                Iterator<HuntPlayer> it = players.iterator();

                while (it.hasNext()) {
                    HuntPlayer p = it.next();
                    p.setPrey(prey);
                }

                tellPreyName();
            } else {
                if (initialSize > 1)
                    m_botAction.sendArenaMessage("Frequency " + getPreyFreq().getFreq() + " is out!");

                freqs.remove(getPreyFreq());

                if (freqs.size() == 1)
                    end(this);// Cool!
                else {
                    updateIndices();
                    this.setPrey();
                }
            }
        }

        /**
            Tell's everyone on this frequency who the prey is.
        */
        public void tellPreyName() {
            Iterator<HuntPlayer> i = players.iterator();

            while (i.hasNext()) {
                HuntPlayer p = i.next();
                p.tellPreyName();
            }
        }

        /**
            Returns a string instance of this HuntFreq.
        */
        public String toString() {
            ArrayList<String> s = new ArrayList<String>();
            int size = players.size();
            Iterator<HuntPlayer> i = players.iterator();

            if (size == 1)
                return players.firstElement().getPlayerName() + " (" + players.firstElement().getPoints() + " point(s))";

            while (i.hasNext()) {
                HuntPlayer p = i.next();
                s.add(p.getPlayerName() + " (" + p.getPoints() + ")");
            }

            return s.toString();
        }

    }

    /**
        This object represents an instance of each player on a HuntFreq
    */
    private class HuntPlayer {

        public Player p;
        public String prey;
        public int points = 0;
        public int deaths = 0;

        /**
            The HuntPlayer constructor.
            @param p - The player
        */
        public HuntPlayer(Player p) {
            this.p = p;
        }

        /**
            Returns the player's name.

            @return
        */
        public String getPlayerName() {
            return p.getPlayerName();
        }

        /**
            Returns the player's ID.

            @return
        */
        public int getPlayerID() {
            return p.getPlayerID();
        }

        /**
            Returns the player's frequency.

            @return
        */
        @SuppressWarnings("unused")
        public int getFreq() {
            return p.getFrequency();
        }

        /**
            Changes this players prey to the parameter.

            @param prey
        */
        public void setPrey(String prey) {
            this.prey = prey;
        }

        /**
            Tells this player who his prey is.
        */
        public void tellPreyName() {
            if (prey == null)
                return;

            m_botAction.sendSmartPrivateMessage(getPlayerName(), "Prey: " + prey + ".");
        }

        /**
            Returns this players current points.

            @return
        */
        public int getPoints() {
            return points;
        }

        /**
            Adds points to the player. Just use a negative value to subtract
            points.

            @param x -
                      The amount you want to add.
        */
        public void addPoints(int x) {
            points += x;
        }

        /**
            Returns the number of times this player has died.

            @return
        */
        public int getLosses() {
            return deaths;
        }

        /**
            Adds a documented death of this player.
        */
        public void addDeath() {
            deaths += 1;
        }

        /**
            Returns the name of the player.
        */
        public String toString() {
            return getPlayerName();
        }
    }
}