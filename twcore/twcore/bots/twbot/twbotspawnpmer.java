package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;

public class twbotspawnpmer extends TWBotExtension
{
    public static final int SAFE_TIME_DEFAULT = 200;

    private String controller;
    private HashMap times;
    private int safeTime;
    private boolean enabled;

    public twbotspawnpmer()
    {
        String controller;
        times = new HashMap();
        safeTime = SAFE_TIME_DEFAULT;
        enabled = false;
    }

    public String[] getHelpMessages()
    {
        String[] message =
        {
            "!SpawnPM On                             -- Activates the spawnpmer module",
            "!SpawnPM Off                            -- Deactivates the spawnpmer module",
            "!SpawnPM Status                         -- Reports spawn time as well as controller name",
            "!SpawnPMTime <Time>                     -- Sets the amount of time in seconds"
        };

        return message;
    }

    public void doAntiSpawnOnCmd( String sender )
    {
        if ( enabled )
            throw new IllegalArgumentException( "SpawnPMer module is already enabled." );
        enabled = true;
        controller = sender;
    }

    public void doAntiSpawnOffCmd( String sender )
    {
        if (!enabled)
            throw new IllegalArgumentException( "SpawnPMer module is already disabled." );
        enabled = false;
        controller = null;
        times.clear();
    }

    public void doNotifyStatusCmd( String sender )
    {
        m_botAction.sendSmartPrivateMessage( sender, "SpawnPMer module currently: " + (enabled ? "ON":"OFF") );
        m_botAction.sendSmartPrivateMessage( sender,"Delay set at: " + (safeTime / 1000) + " seconds" );
        if ( enabled )
            m_botAction.sendSmartPrivateMessage( sender, "Controller is " + controller );
    }

    public void doSafeTimeCmd( String sender, String argString )
    {
        try
        {
            float time = Float.parseFloat(argString);
            if(time < 0.1)
                throw new IllegalArgumentException( "Invalid safe time." );
            safeTime = (new Float(time * 1000)).intValue();
            m_botAction.sendSmartPrivateMessage( sender, "Spawn time adjusted to " + time + " seconds." );
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException( "Please use the following format: !SafeTime <Time>" );
        }
    }

    public void handleCommand( String sender, String message )
    {
        String command = message.toLowerCase();

        try
        {
            if ( command.toLowerCase().equals( "!spawnpm on" ) )
                doAntiSpawnOnCmd( sender );
            if ( command.toLowerCase().equals( "!spawnpm off" ) )
                doAntiSpawnOffCmd( sender );
            if ( command.toLowerCase().equals( "!spawnpm status" ) )
                doNotifyStatusCmd( sender );
            if ( command.toLowerCase().startsWith( "!spawnpmtime " ) )
                doSafeTimeCmd( sender, message.substring( 13 ) );
        }
            catch( Exception e )
        {
            m_botAction.sendSmartPrivateMessage( sender, e.getMessage() );
        }
    }

    public void handleEvent( Message event )
    {
        int messageType = event.getMessageType();
        int senderID = event.getPlayerID();
        String sender = m_botAction.getPlayerName( senderID );
        String message = event.getMessage();

        if ( messageType == Message.PRIVATE_MESSAGE && m_opList.isER( sender ) )
            handleCommand( sender, message );
    }

    public void handleEvent( PlayerDeath event )
    {
        if ( enabled )
        {
            Player player = m_botAction.getPlayer( event.getKilleeID() );
            Player killer = m_botAction.getPlayer( event.getKillerID() );
            if ( player.getFrequency() != killer.getFrequency() )
            {
                long time = System.currentTimeMillis();
                long difference = time - ( (Long)times.get( player ) ).longValue();
                if ( times.containsKey( player ) && difference <= safeTime )
                {
                    difference /= 1000;
                    m_botAction.sendSmartPrivateMessage( controller, player.getPlayerName() + " spawned by " + killer.getPlayerName() + ": " + difference + " seconds." );
                }

                times.put( player, new Long(time) );

            }
        }
    }

    public void cancel()
    {
        enabled = false;
    }

}