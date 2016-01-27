package twcore.core.stats;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import twcore.core.BotAction;

//With intentions of being able to use this for a variety of things.

public class PlayerProfile
{
    HashMap<String, Enemy> peeps = new HashMap<String, Enemy>();
    int kills = 0, deaths = 0, teamKills = 0;
    int ship = 1, freq = 0, time = 0, lastDeath = 0;
    int[] data = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    String playerName;
    String sub = "";
    boolean state = false;

    public PlayerProfile() {
    }

    public PlayerProfile( String name ) {
        playerName = name;
    }

    public PlayerProfile( String name, int s ) {
        playerName = name;
        ship = s;
    }

    public PlayerProfile( String name, int s, int f ) {
        playerName = name;
        ship = s;
        freq = f;
    }

    public void addKill()   {
        kills++;
    }
    public int  getKills()  {
        return kills;
    }

    public void addDeath()  {
        deaths++;
        lastDeath = (int)(System.currentTimeMillis() / 1000);
    }
    public void removeDeath()   {
        deaths--;   //Adds SpecTask functionality (death-wise) for non-spec purposes.
    }
    public int timeFromLastDeath() {
        return (int)(System.currentTimeMillis() / 1000) - lastDeath;
    }
    public int  getDeaths() {
        return deaths;
    }
    public void setDeaths( int d ) {
        deaths = d;
    }

    public void setShip( int s ) {
        ship = s;
    }
    public int  getShip()   {
        return ship;
    }

    public void setFreq( int f ) {
        freq = f;
    }
    public int  getFreq()   {
        return freq;
    }

    public void setTime( int t ) {
        time = t;
    }
    public int  getTime()   {
        return time;
    }

    public void setSub( String s ) {
        sub = s;
    }
    public String getSub() {
        return sub;
    }

    public void setData( int location, int value ) {
        data[location] = value;
    }
    public void incData( int location ) {
        data[location]++;
    }
    public void decData( int location ) {
        data[location]--;
    }
    public int  getData( int location ) {
        return data[location];
    }

    public String getName() {
        return playerName;
    }
    public boolean state() {
        return state;
    }
    public void toggleState() {
        state = !state;
    }

    public void reset() {
        Set<String> s = peeps.keySet();
        Iterator<String> i = s.iterator();

        while( i.hasNext() ) {
            String name = (String)i.next();
            peeps.get( name ).zeroDamage();
        }
    }

    public void attack( String attacker, int damage ) {
        if( peeps.containsKey( attacker ) )
            peeps.get( attacker ).addDamage( damage );
        else
            peeps.put( attacker, new Enemy( damage ) );
    }

    public void hit( String defender, int damage ) {
        if( peeps.containsKey( defender ) )
            peeps.get( defender ).addDamageDealt( damage );
        else
            peeps.put( defender, new Enemy( 0, damage ) );
    }

    public String getAssist( int m, String killer, BotAction m_botAction ) {
        String result = "";
        int min = m;
        Set<String> s = peeps.keySet();
        Iterator<String> i = s.iterator();

        while( i.hasNext() ) {
            String name = (String)i.next();
            Enemy e = peeps.get( name );

            //m_botAction.sendArenaMessage( name + " did " + e.getCurrentEnergy() + " of damage." );
            if( e.getCurrentEnergy() > min && !name.equals( killer ) ) {
                min = e.getCurrentEnergy();
                result = name;
            }
        }

        return result;
    }

    public int getDamageDealt() {
        int dd = 0;
        Set<String> s = peeps.keySet();
        Iterator<String> i = s.iterator();

        while( i.hasNext() ) {
            String name = (String)i.next();
            Enemy e = peeps.get( name );
            dd += e.getDamageDealt();
        }

        return dd;
    }

    public int getDamageTaken() {
        int dd = 0;
        Set<String> s = peeps.keySet();
        Iterator<String> i = s.iterator();

        while( i.hasNext() ) {
            String name = (String)i.next();
            Enemy e = peeps.get( name );
            dd += e.getDamageTaken();
        }

        return dd;
    }
}

class Enemy {

    int totalEnergy = 0;
    int currentEnergy = 0;
    int damageTaken = 0;

    public Enemy( int d ) {
        totalEnergy = d;
        currentEnergy = d;
    }

    public Enemy( int d, int d2 ) {
        damageTaken = d2;
    }

    public void addDamage( int d ) {
        totalEnergy += d;
        currentEnergy += d;
    }

    public int getCurrentEnergy() {
        return currentEnergy;
    }

    public void addDamageDealt( int d ) {
        damageTaken += d;
    }

    public int getDamageDealt() {
        return damageTaken;
    }
    public int getDamageTaken() {
        return totalEnergy;
    }

    public void zeroDamage() {
        currentEnergy = 0;
    }
}