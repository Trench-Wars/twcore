package twcore.bots.spaceballbot;

import twcore.core.*;

class Projectile {

	SBPlayer owner;
	int birth;

	int x;
	int y;
	int vX;
	int vY;
	int type;

	public Projectile(SBPlayer tO, int tX, int tY, int tVX, int tVY, int t) {
		owner = tO;
		birth = (int)(System.currentTimeMillis());

		x = tX;
		y = tY;
		vX = tVX;
		vY = tVY;
		type = t;
	}

	public SBPlayer getOwner() { return owner; }

	public boolean isHitting(int bX, int bY) {

		if (getDistance(bX, bY) <= 32) {
			return true;
		} else {
			return false;
		}
	}

	public double getDistance(int bX, int bY) {
		double dist = Math.sqrt( Math.pow(( bX - getXLocation() ), 2) + Math.pow(( bY - getYLocation() ), 2) );
		return dist;
	}

	public double getXLocation() { return x + vX * getAge() / (double) 10000.0; }

	public double getYLocation() { return y + vY * getAge() / (double) 10000.0; }

	public int getXVelocity() { return vX; }

	public int getYVelocity() { return vY; }

	public double getAge() { return (int)(System.currentTimeMillis()) - birth; }

	public int getMass() { return 10; }

	public int getType() { return type; }
}

