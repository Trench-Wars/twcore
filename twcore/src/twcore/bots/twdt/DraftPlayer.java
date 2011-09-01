package twcore.bots.twdt;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.WeaponFired;
import twcore.core.lag.LagInfo;

/**
 *
 * @author WingZero
 */
public class DraftPlayer {

    public static final String db = "website";
    
    BotAction ba;
    OperatorList opList;
    BotSettings rules;
    
    enum Status { NONE, IN, LAGGED, OUT }
    
    private LagInfo lagInfo;
    private int maxCurrPing;
    private double maxPacketLoss;
    //private double maxSlowPackets;
    private double maxStandardDeviation;
    private int maxNumSpikes;
    
    Status status;
    DraftStats statTracker;
    String name;
    int ship, lagouts, stars, specAt;
    long lastAttach;
    boolean botSpec;                // true if bot specced player for lag
    
    public DraftPlayer(String name, int ship) {
        this.name = name;
        this.ship = ship;
        lastAttach = 0;
        lagouts = 0;
        statTracker = new DraftStats(ship);
        status = Status.IN;
        lagInfo = new LagInfo(ba, name, rules.getInt("spikesize"));
        lagInfo.updateLag();
        maxCurrPing = rules.getInt("maxcurrping");
        maxPacketLoss = rules.getDouble("maxploss");
        //maxSlowPackets = rules.getDouble("maxslowpackets");
        maxStandardDeviation = rules.getDouble("maxstandarddeviation");
        maxNumSpikes = rules.getInt("maxnumspikes");
    }
    
    public void handleEvent(Message event) {
        lagInfo.handleEvent(event);
    }

    public void handleEvent(WeaponFired event) {
        int type = event.getWeaponType();
        if (type == WeaponFired.WEAPON_BULLET)
            statTracker.getStat(StatType.SHOTS).increment();
        if (type == WeaponFired.WEAPON_BOMB)
            statTracker.getStat(StatType.BOMBS).increment();
        if (type == WeaponFired.WEAPON_BURST)
            statTracker.getStat(StatType.BURSTS).increment();
        if (type == WeaponFired.WEAPON_REPEL)
            statTracker.getStat(StatType.REPELS).increment();
    }
    
    public void handleDeath(DraftPlayer player) {
        
    }
    
    public void handleKill(DraftPlayer player) {
        
    }
    
    public void handleAttach() {
        lastAttach = System.currentTimeMillis();
    }
    
    public void handleFlagClaim() {
        statTracker.getStat(StatType.FLAG_CLAIMS).increment();
    }
    
    public Status getStatus() {
        return status;
    }
    
    public int getDeaths() {
        return statTracker.getStat(StatType.DEATHS).getValue();
    }
    
    public void setLagSpec(boolean specced) {
        botSpec = specced;
        if (botSpec)
            status = Status.LAGGED;
    }
    
    public void lagSpec(String reason) {
        ba.sendSmartPrivateMessage(name, "You have been placed into spectator mode due to lag.");
        ba.sendSmartPrivateMessage(name, reason);
        ba.spec(name);
        ba.spec(name);
    }

    public void checkLag() {
        try {
            int currentPing = lagInfo.getCurrentPing();
            double s2c = lagInfo.getS2C();
            double c2s = lagInfo.getC2S();
            //double s2cSlowPercent = lagInfo.getS2CSlowPercent();
            //double c2sSlowPercent = lagInfo.getC2SSlowPercent();
            double spikeSD = lagInfo.getSpikeSD();
            int numSpikes = lagInfo.getNumSpikes();
            /*
                m_botAction.sendArenaMessage("Lag for " + m_fcPlayerName + ":");
                m_botAction.sendArenaMessage("Current Ping: " + currentPing + "ms.");
                m_botAction.sendArenaMessage("C2S Packetloss: " + c2s + "%.  S2C Packetloss: " + s2c + "%.");
                m_botAction.sendArenaMessage("C2S Slow Packets: " + c2sSlowPercent + "%.  S2C Slow Packets: " + s2cSlowPercent + "%.");
                m_botAction.sendArenaMessage("Spike: +- " + spikeSD + "ms.  Number of spikes: " + numSpikes + ".");
            */
            if (status == Status.IN) {
                if (currentPing > maxCurrPing)
                    lagSpec("Current ping is: " + currentPing + "ms.  Maximum allowed ping is: " + maxCurrPing + "ms.");
                else if (s2c > maxPacketLoss)
                    lagSpec("Current S2C Packetloss is: " + s2c + "%.  Maximum allowed S2C Packetloss is: " + maxPacketLoss + "%.");
                else if (c2s > maxPacketLoss)
                    lagSpec("Current C2S Packetloss is: " + c2s + "%.  Maximum allowed C2S Packetloss is: " + maxPacketLoss + "%.");
                //else if(s2cSlowPercent > maxSlowPackets)
                //  specForLag("Current S2C Slow Packetloss is: " + s2cSlowPercent + "%.  Maximum allowed S2C Slow Packetloss is: " + maxSlowPackets + "%.");
                //else if(c2sSlowPercent > maxSlowPackets)
                //  specForLag("Current C2S Slow Packetloss is: " + c2sSlowPercent + "%.  Maximum allowed C2S Slow Packetloss is: " + maxSlowPackets + "%.");
                else if (spikeSD > maxStandardDeviation)
                    lagSpec("Current spiking: +- " + spikeSD + "ms.  " + "Maximum spiking allowed: +- " + maxStandardDeviation + "ms.");
                else if (numSpikes > maxNumSpikes)
                    lagSpec("Number of recent spikes: " + spikeSD + ".  Maximum spikes allowed: " + maxNumSpikes + ".");
            }
        } catch (Exception e) {}
    }
}
