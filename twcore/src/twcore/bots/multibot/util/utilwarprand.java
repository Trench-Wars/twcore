package twcore.bots.multibot.util;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.util.MapRegions;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * Warps players that enter one defined region to a random point contained within
 * another defined map region.
 *
 * @author D1st0rt
 * @version 06.06.21
 */
public class utilwarprand extends MultiUtil
{
	/** The regions on the map. */
	private MapRegions regions;
	/** The positions the bot sits at. */
	private Point[] positions;
	/** The mapping between initial and destination regions. */
	private int[] targetRegions;
	/** The task to keep the bot moving. */
	private Reposition jumpTask;
	/** Whether the module is engaged or not. */
	private boolean active;
	/** The warp cooldown list. */
	private HashSet<String> queue;

	/** The help message to be sent to bot operators */
	private final String helpMessage[] =
	{
		"+--------------------Random Warps Module-------------------+",
		"|  Release 1.0 [06/21/06] - http://d1st0rt.sscentral.com   |",
		"+----------------------------------------------------------+",
		"! !activate  - Toggles the module doing anything when a    |",
		"|              player flies over a designated warp region  |",
		"|                                                          |",
		"| !loadwarps <cfg> - Sets up the warping system for a given|",
		"|                    warp configuration, ie !loadwarps javs|",
		"+----------------------------------------------------------+"
	};

	/**
     * Creates a new instance of twbotwarprand
     */
	public void init()
	{
		regions = new MapRegions();
		positions = new Point[0];
		queue = new HashSet<String>();
	}
    
    /**
     * Requests events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.PLAYER_POSITION );
    }

	/**
     * Get the help message for this module.
     * @return a String array containing the help message
     */
	public String[] getHelpMessages()
	{
		return helpMessage;
	}

	/**
     * Cancel the repositioning task when module is unloaded.
     */
	public void cancel()
	{
		if(jumpTask != null)
            m_botAction.cancelTask(jumpTask);
	}

	/**
     * Sets up the positions the bot should sit at from a provided config file
     * @param cfg the configuration file containing the spec positions
     */
	private void readSpecPositions(BotSettings cfg)
	{
		ArrayList<Point> posList = new ArrayList<Point>();

		int count = 0;
		String pos = cfg.getString("Position0");
		while(pos != null)
		{
			String[] part = pos.split(",");
			if(part.length == 2)
			{
				Point p = new Point(Integer.parseInt(part[0]),
								    Integer.parseInt(part[1]));
				posList.add(p);
			}
			count++;
			pos = cfg.getString("Position"+ count);
		}

		positions = posList.toArray(positions);
	}

	/**
     * Sets up the warps from a provided config file
     * @param cfg the configuration file containing the warp parameters
     */
	private void readWarpSetups(BotSettings cfg)
	{
		targetRegions = new int[regions.getRegionCount()];

		for(int i = 0; i < targetRegions.length; i++)
		{
			int target = cfg.getInt("Warp"+ i);

			targetRegions[i] = target;
		}
	}

	/**
     * Generates a random point within the specified region
     * @param region the region the point should be in
     * @return A point object, or null if the first 10000 points generated
     *         were not within the desired region.
     */
	private Point getRandomPoint(int region)
	{
		boolean valid = false;
		Point p = null;

		int count = 0;
		while(!valid)
		{
			int x = (int)(Math.random() * 1024);
			int y = (int)(Math.random() * 1024);
			p = new Point(x, y);
			valid = regions.checkRegion(x, y, region);
			count++;
			if(count > 10000)
				valid = true;
		}

		return p;
	}

	/**
     * Event: Message
     * Handle incoming commands.
     */
	public void handleEvent(Message event)
	{
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(m_opList.isER(name))
			{
				String message = event.getMessage().toLowerCase();
				if(message.startsWith("!activate"))
				{
					c_Activate(name, message);
				}
				else if(message.startsWith("!loadwarps "))
				{
					c_LoadWarps(name, message.substring(11));
				}
			}
		}
	}

	/**
     * Event: PlayerPosition
     * Check player positions and warp when necessary.
     */
	public void handleEvent(PlayerPosition event)
	{
		if(active)
		{
			int current = regions.getRegion(m_botAction.getPlayer(event.getPlayerID()));
			if(current != -1)
			{
				int dest = targetRegions[current];
				String name = m_botAction.getPlayerName(event.getPlayerID());
				if(dest != -1 && !queue.contains(name))
				{
					queue.add(name);
					Point p = getRandomPoint(dest);
					if(p != null)
					{
						m_botAction.warpTo(event.getPlayerID(), (int)p.getX(), (int)p.getY());
						m_botAction.scheduleTask(new WarpCooldown(name), 5000);
					}
				}
			}
		}
	}

	/**
     * Command: !loadwarps <configuration>
     * Sets up the warping system for the given parameters.
     * @param name the name of the person who sent the command
     * @param message the name of the warp configuration
     */
	private void c_LoadWarps(String name, String message)
	{
		try{
			//wipe any existing regions
			regions.clearRegions();
			regions.loadRegionImage(message +".png");
			BotSettings cfg = regions.loadRegionCfg(message +".cfg");

			//read in watching positions
			readSpecPositions(cfg);

			//read in warp configurations
			readWarpSetups(cfg);

			m_botAction.sendPrivateMessage(name, "Successfully loaded "+ message);

		}catch(FileNotFoundException fnf)
		{
			m_botAction.sendPrivateMessage(name, "Error: "+ message +".png and "+
			message +".cfg must be in the data/maps folder.");
		}
		catch(javax.imageio.IIOException iie)
		{
			m_botAction.sendPrivateMessage(name, "Error: couldn't read image");
		}
		catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Could not load warps for "+ message);
			Tools.printStackTrace(e);
		}
	}

	/**
     * Command: !activate
     * Turns warping on/off
     */
	private void c_Activate(String name, String message)
	{
		if(active = !active)
		{
			m_botAction.sendPrivateMessage(name, "Warping active.");
			jumpTask = new Reposition();
			m_botAction.scheduleTaskAtFixedRate(jumpTask, 0, 2000);
		}
		else
		{
			m_botAction.sendPrivateMessage(name, "Warping disabled.");
            m_botAction.cancelTask(jumpTask);
			jumpTask = null;
		}
	}

	/**
     * Task to move the bot to all of its watching positions
     *
     * @author D1st0rt
     * @version 06.06.21
     */
	private class Reposition extends TimerTask
	{
		/** The current index in the position list */
		private int index;

		/**
	     * Creates a new instance of Reposition.
	     */
		public Reposition()
		{
			index = 0;
		}

		/**
	     * Runs this task, moves to the next position.
	     */
		public void run()
		{
			if(positions.length > 0)
			{
				index++;
				if(index >= positions.length)
					index = 0;
				Point p = positions[index];
				m_botAction.moveToTile((int)p.getX(), (int)p.getY());
			}
		}
	}

	/**
     * Task to ignore any position packets after the first one received from
     * a single player that require a warp.
     *
     * @author D1st0rt
     * @version 06.06.21
     */
	private class WarpCooldown extends TimerTask
	{
		/** The name of the player to remove from the list */
		private String name;

		/**
	     * Creates a new instance of WarpCooldown.
	     * @param name the name of the player to remove from the list.
	     */
		public WarpCooldown(String name)
		{
			this.name = name;
		}

		/**
	     * Runs this task, removes the player from the cooldown list.
	     */
		public void run()
		{
			queue.remove(name);
		}
	}
}
