package twcore.bots.spaceballbot;

import twcore.core.*;

class Cannon {

	SBPlayer owner = null;
	int attachTime = 0;
	int x;
	int y;

	BotAction m_botAction;

	public Cannon(int tX, int tY, BotAction b) {
		x = tX;
		y = tY;
		m_botAction = b;
	}

	public boolean isInArea(int tX, int tY) { return getDistance(tX, tY) <= 32; }

	public double getDistance(int tX, int tY) {
		double dist = Math.sqrt( Math.pow(( tX - x ), 2) + Math.pow(( tY - y ), 2) );
		return dist;
	}

	public void attach(SBPlayer p) {
		attachTime = (int)(System.currentTimeMillis());
		owner = p;
		owner.setCannon(this);
		m_botAction.setShip(owner.getName(), 2);
		m_botAction.warpTo(owner.getName(), x / 16, y / 16);
		m_botAction.sendPrivateMessage(owner.getName(), "You have been attached to a cannon. To deattach simply warp out (or die.. )");
	}

	public void deAttach() {
		if (timeAttached() > 3000) {
			owner.setCannon(null);
			m_botAction.setShip(owner.getName(), 1);
			owner = null; 
		}
	}

	public int timeAttached() { return (int)(System.currentTimeMillis()) - attachTime; }

	public String getOwner() { return owner.getName(); }

	public boolean inUse() { return owner != null; }
}
