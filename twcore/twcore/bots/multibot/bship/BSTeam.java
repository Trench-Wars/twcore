package twcore.bots.multibot.bship;

import java.util.*;

public class BSTeam
{
	/** The players on this team */
	private Vector<BSPlayer> _players;
	/** This team's frequency */
	private int _freq;
	/** A cached ship count for this team */
	private byte[] _shipCount;
	/** Whether the cache is up to date or not */
	private boolean _changed;

	public BSTeam(int freq)
	{
		_freq = freq;
		_players = new Vector<BSPlayer>();
		_changed = true;
		_shipCount = new byte[8];
	}

	public void reset()
	{
		_players.clear();
		_changed = false;
		_shipCount = null;
	}

	public BSPlayer[] getPlayers(byte ship)
	{
		Vector<BSPlayer> players = new Vector<BSPlayer>();
		for(BSPlayer p : _players)
		{
			if(ship == bship.ALL || (ship == bship.PLAYING && p.ship != bship.SPEC) || (p.ship == ship) )
				players.add(p);
		}
		return players.toArray(new BSPlayer[players.size()]);
	}

	public void removePlayer(String name)
	{
		BSPlayer p = getPlayer(name);
		if(p != null)
			_players.remove(p);
	}

	public BSPlayer[] getPlayers()
	{
		return _players.toArray(new BSPlayer[_players.size()]);
	}

	public void setShip(String name, byte ship)
	{
		BSPlayer p = getPlayer(name);
		if(p == null)
			p = new BSPlayer(name);

		p.ship = ship;
		if(p.ship != bship.SPEC)
			p.ships[ship-1] = ("" +ship).charAt(0);

		_changed = true;
		System.out.println("Ship set: "+ name +" - "+ ship);
	}

	public BSPlayer getPlayer(String name)
	{
		BSPlayer[] players = getPlayers();
		for(int x = 0; x < players.length; x++)
		{
			if(players[x].toString().equals(name))
				return players[x];
		}
		return null;
	}

	public boolean playerDeath(String name)
	{
		BSPlayer p = getPlayer(name);
		switch(p.ship)
		{
			case bship.PLANE:
				//fall through
			case bship.GUN:
				//fall through
			case bship.CANNON:
				p.rating--;
			break;

			default:
				p.rating -= 3;
				p.lives--;
				if(p.lives < 1)
					return false;
		}
		return true;
	}

	public void playerKill(String name, byte ship)
	{
		BSPlayer p = getPlayer(name);
		switch(ship)
		{
			case bship.PLANE:
				p.pkills++;
				p.rating++;
			break;

			case bship.GUN:
				//fall through
			case bship.CANNON:
				p.tkills++;
				p.rating += 2;
			break;

			default:
				p.cskills++;
				p.rating += 5;
		}
	}

	public void attach(String attachee, String attacher)
	{
		BSPlayer ship = getPlayer(attachee);
		BSPlayer turret = getPlayer(attacher);

		ship.tacount++;
		ship.rating++;

		turret.takeoffs++;
	}

	public int getShipCount(byte ship)
	{
		return getPlayers(ship).length;
	}

	public byte[] getShipCount()
	{
		if(_changed)
			for(byte x = 1; x < 9; x++)
				_shipCount[x-1] = (byte)getShipCount(x);

		_changed = false;
		return _shipCount;
	}

	public boolean isOut()
	{
		byte[] ships = getShipCount();
		int count = ships[3] + ships[4] + ships[5] + ships[6] + ships[7];
		return (count == 0);
	}
}

