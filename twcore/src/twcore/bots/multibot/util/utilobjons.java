package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

public class utilobjons extends MultiUtil
{
	public void init()
	{
	}
    
    /**
     * Requests events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
    }    

	public void handleEvent(Message event)
	{
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			int playerID = event.getPlayerID();
			String playerName = m_botAction.getPlayerName(playerID);
			if(m_opList.isER(playerName))
				handleCommand(playerName, event.getMessage().toLowerCase());
		}
	}

	public void handleCommand(String name, String message)
	{
		if(message.startsWith("!obj"))
			Objon(message);
		else if(message.startsWith("!stop"))
			m_botAction.cancelTasks();
	}

	public void Objon(String message)
	{
		String pieces[] = message.split(" ");
		if( pieces.length < 2 )
		    return;
		
		final String params[] = pieces[1].split(":");

		if(pieces[0].equals("!objon"))
		{
			try {
				switch(params.length)
				{
					case 1:
						m_botAction.showObject(Integer.parseInt(params[0]));
						break;
					case 2:
						m_botAction.showObject(Integer.parseInt(params[0]));
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								m_botAction.hideObject(Integer.parseInt(params[0]));
							}
						};

						m_botAction.scheduleTask(objoff, Integer.parseInt(params[1]) * 1000);
						break;
					case 3:
						TimerTask objoff2 = new TimerTask()
						{
							public void run()
							{
								m_botAction.hideObject(Integer.parseInt(params[0]));
							}
						};
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								m_botAction.hideObject(Integer.parseInt(params[0]));
							}
						};

						m_botAction.scheduleTaskAtFixedRate(objoff2, Integer.parseInt(params[1]) * 1000, (Integer.parseInt(params[1]) + Integer.parseInt(params[2])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon, 0, (Integer.parseInt(params[1]) + Integer.parseInt(params[2])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
		else if(pieces[0].equals("!objonplayer"))
		{
			try {
				switch(params.length)
				{
					case 2:
						m_botAction.sendUnfilteredPrivateMessage(params[1], "*objon " + params[0]);
						break;
					case 3:
						m_botAction.sendUnfilteredPrivateMessage(params[1], "*objon " + params[0]);
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								m_botAction.sendUnfilteredPrivateMessage(params[1], "*objoff " + params[0]);
							}
						};

						m_botAction.scheduleTask(objoff, Integer.parseInt(params[2]) * 1000);
						break;
					case 4:
						TimerTask objoff2 = new TimerTask()
						{
							public void run()
							{
								m_botAction.sendUnfilteredPrivateMessage(params[1], "*objoff " + params[0]);
							}
						};
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								m_botAction.sendUnfilteredPrivateMessage(params[1], "*objon " + params[0]);
							}
						};

						m_botAction.scheduleTaskAtFixedRate(objoff2, Integer.parseInt(params[2]) * 1000, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon, 0, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
		else if(pieces[0].equals("!objonfreq"))
		{
			try {
				switch(params.length)
				{
					case 2:
						Iterator<Player> iterator1 = m_botAction.getPlayerIterator();
						Player player1;
						while(iterator1.hasNext())
						{
							player1 = (Player) iterator1.next();
							if(player1.getFrequency() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player1.getPlayerName(), "*objon " + params[0]);
						}
						break;
					case 3:
						Iterator<Player> iterator2 = m_botAction.getPlayerIterator();
						Player player2;
						while(iterator2.hasNext())
						{
							player2 = (Player) iterator2.next();
							if(player2.getFrequency() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player2.getPlayerName(), "*objon " + params[0]);
						}
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getFrequency() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objoff " + params[0]);
								}
							}
						};

						m_botAction.scheduleTask(objoff, Integer.parseInt(params[2]) * 1000);
						break;
					case 4:
						TimerTask objoff2 = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getFrequency() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objoff " + params[0]);
								}
							}
						};
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getFrequency() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objon " + params[0]);
								}
							}
						};

						m_botAction.scheduleTaskAtFixedRate(objoff2, Integer.parseInt(params[2]) * 1000, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon, 0, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
		else if(pieces[0].equals("!objonship"))
		{
			try {
				switch(params.length)
				{
					case 2:
						Iterator<Player> iterator1 = m_botAction.getPlayerIterator();
						Player player1;
						while(iterator1.hasNext())
						{
							player1 = (Player) iterator1.next();
							if(player1.getShipType() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player1.getPlayerName(), "*objon " + params[0]);
						}
						break;
					case 3:
						Iterator<Player> iterator2 = m_botAction.getPlayerIterator();
						Player player2;
						while(iterator2.hasNext())
						{
							player2 = (Player) iterator2.next();
							if(player2.getShipType() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player2.getPlayerName(), "*objon " + params[0]);
						}
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getShipType() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objoff " + params[0]);
								}
							}
						};

						m_botAction.scheduleTask(objoff, Integer.parseInt(params[2]) * 1000);
						break;
					case 4:
						TimerTask objoff2 = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getShipType() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objoff " + params[0]);
								}
							}
						};
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getShipType() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objon " + params[0]);
								}
							}
						};

						m_botAction.scheduleTaskAtFixedRate(objoff2, Integer.parseInt(params[2]) * 1000, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon, 0, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
		else if(pieces[0].equals("!objoff"))
		{
			try {
				switch(params.length)
				{
					case 1:
						m_botAction.hideObject(Integer.parseInt(params[0]));
						break;
					case 2:
						m_botAction.hideObject(Integer.parseInt(params[0]));
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								m_botAction.showObject(Integer.parseInt(params[0]));
							}
						};

						m_botAction.scheduleTask(objon, Integer.parseInt(params[1]) * 1000);
						break;
					case 3:
						TimerTask objon2 = new TimerTask()
						{
							public void run()
							{
								m_botAction.showObject(Integer.parseInt(params[0]));
							}
						};
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								m_botAction.hideObject(Integer.parseInt(params[0]));
							}
						};
						m_botAction.scheduleTaskAtFixedRate(objoff, 0, (Integer.parseInt(params[1]) + Integer.parseInt(params[2])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon2, Integer.parseInt(params[1]) * 1000, (Integer.parseInt(params[1]) + Integer.parseInt(params[2])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
		else if(pieces[0].equals("!objoffplayer"))
		{
			try {
				switch(params.length)
				{
					case 2:
						m_botAction.sendUnfilteredPrivateMessage(params[1], "*objoff " + params[0]);
						break;
					case 3:
						m_botAction.sendUnfilteredPrivateMessage(params[1], "*objoff " + params[0]);
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								m_botAction.sendUnfilteredPrivateMessage(params[1], "*objon " + params[0]);
							}
						};

						m_botAction.scheduleTask(objon, Integer.parseInt(params[2]) * 1000);
						break;
					case 4:
						TimerTask objon2 = new TimerTask()
						{
							public void run()
							{
								m_botAction.sendUnfilteredPrivateMessage(params[1], "*objon " + params[0]);
							}
						};
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								m_botAction.sendUnfilteredPrivateMessage(params[1], "*objoff " + params[0]);
							}
						};

						m_botAction.scheduleTaskAtFixedRate(objoff, 0, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon2, Integer.parseInt(params[2]) * 1000, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
		else if(pieces[0].equals("!objofffreq"))
		{
			try {
				switch(params.length)
				{
					case 2:
						Iterator<Player> iterator1 = m_botAction.getPlayerIterator();
						Player player1;
						while(iterator1.hasNext())
						{
							player1 = (Player) iterator1.next();
							if(player1.getFrequency() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player1.getPlayerName(), "*objoff " + params[0]);
						}
						break;
					case 3:
						Iterator<Player> iterator2 = m_botAction.getPlayerIterator();
						Player player2;
						while(iterator2.hasNext())
						{
							player2 = (Player) iterator2.next();
							if(player2.getFrequency() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player2.getPlayerName(), "*objoff " + params[0]);
						}
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getFrequency() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objon " + params[0]);
								}
							}
						};

						m_botAction.scheduleTask(objon, Integer.parseInt(params[2]) * 1000);
						break;
					case 4:
						TimerTask objon2 = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getFrequency() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objon " + params[0]);
								}
							}
						};
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getFrequency() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objoff " + params[0]);
								}
							}
						};

						m_botAction.scheduleTaskAtFixedRate(objoff, 0, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon2, Integer.parseInt(params[2]) * 1000, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
		else if(pieces[0].equals("!objoffship"))
		{
			try {
				switch(params.length)
				{
					case 2:
						Iterator<Player> iterator1 = m_botAction.getPlayerIterator();
						Player player1;
						while(iterator1.hasNext())
						{
							player1 = (Player) iterator1.next();
							if(player1.getShipType() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player1.getPlayerName(), "*objoff " + params[0]);
						}
						break;
					case 3:
						Iterator<Player> iterator2 = m_botAction.getPlayerIterator();
						Player player2;
						while(iterator2.hasNext())
						{
							player2 = (Player) iterator2.next();
							if(player2.getShipType() == Integer.parseInt(params[1]))
								m_botAction.sendUnfilteredPrivateMessage(player2.getPlayerName(), "*objoff " + params[0]);
						}
						TimerTask objon = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getShipType() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objon " + params[0]);
								}
							}
						};

						m_botAction.scheduleTask(objon, Integer.parseInt(params[2]) * 1000);
						break;
					case 4:
						TimerTask objon2 = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getShipType() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objon " + params[0]);
								}
							}
						};
						TimerTask objoff = new TimerTask()
						{
							public void run()
							{
								Iterator<Player> iterator = m_botAction.getPlayerIterator();
								Player player;
								while(iterator.hasNext())
								{
									player = (Player) iterator.next();
									if(player.getShipType() == Integer.parseInt(params[1]))
										m_botAction.sendUnfilteredPrivateMessage(player.getPlayerName(), "*objoff " + params[0]);
								}
							}
						};

						m_botAction.scheduleTaskAtFixedRate(objoff, 0, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						m_botAction.scheduleTaskAtFixedRate(objon2, Integer.parseInt(params[2]) * 1000, (Integer.parseInt(params[2]) + Integer.parseInt(params[3])) * 1000);
						break;
				}
			} catch(Exception e) {}
		}
	}

	public String[] getHelpMessages()
	{
		String help[] = {
			"!Objon <#>                              -Turns on object <#> for everyone.",
			"!Objon <#>:<time>                       -Turns on object <#> for <time> seconds.",
			"!Objon <#>:<time>:<time2>               -Turns on object <#> for <time> seconds, off for <time2> seconds and repeats.",
			"!ObjonPlayer <#>:<name>                 -Turns on object <#> for <name>.",
			"!ObjonPlayer <#>:<name>:<time>          -Turns on object <#> for <name> for <time> seconds.",
			"!ObjonPlayer <#>:<name>:<time>:<time2>  -Turns on object <#> for <name> for <time> seconds, off for <time2> seconds and repeats.",
			"!ObjonFreq <#>:<freq>                   -Turns on object <#> for freq <freq>.",
			"!ObjonFreq <#>:<freq>:<time>            -Turns on object <#> for freq <freq> for <time> seconds.",
			"!ObjonFreq <#>:<freq>:<time>:<time2>    -Turns on object <#> for freq <freq> for <time> seconds, off for <time2> seconds and repeats.",
			"!ObjonShip <#>:<ship>                   -Turns on object <#> for ship <ship>.",
			"!ObjonShip <#>:<ship>:<time>            -Turns on object <#> for ship <ship> for <time> seconds.",
			"!ObjonShip <#>:<ship>:<time>:<time2>    -Turns on object <#> for ship <ship> for <time> seconds, off for <time2> seconds and repeats.",
			"!Objoff <#>                             -Turns off object <#> for everyone.",
			"!Objoff <#>:<time>                      -Turns off object <#> for <time> seconds.",
			"!Objoff <#>:<time>:<time2>              -Turns off object <#> for <time> seconds, off for <time2> seconds and repeats.",
			"!ObjoffPlayer <#>:<name>                -Turns off object <#> for <name>.",
			"!ObjoffPlayer <#>:<name>:<time>         -Turns off object <#> for <name> for <time> seconds.",
			"!ObjoffPlayer <#>:<name>:<time>:<time2> -Turns off object <#> for <name> for <time> seconds, off for <time2> seconds and repeats.",
			"!ObjoffFreq <#>:<freq>                  -Turns off object <#> for freq <freq>.",
			"!ObjoffFreq <#>:<freq>:<time>           -Turns off object <#> for freq <freq> for <time> seconds.",
			"!ObjoffFreq <#>:<freq>:<time>:<time2>   -Turns off object <#> for freq <freq> for <time> seconds, off for <time2> seconds and repeats.",
			"!ObjoffShip <#>:<ship>                  -Turns off object <#> for ship <ship>.",
			"!ObjoffShip <#>:<ship>:<time>           -Turns off object <#> for ship <ship> for <time> seconds.",
			"!ObjoffShip <#>:<ship>:<time>:<time2>   -Turns off object <#> for ship <ship> for <time> seconds, on for <time2> seconds and repeats."
		};

		return help;
	}

	public void cancel()
	{
	}
}