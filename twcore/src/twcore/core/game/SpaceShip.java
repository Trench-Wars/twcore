package twcore.core.game;

import java.util.LinkedList;
import java.util.ListIterator;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.events.PlayerPosition;
import twcore.core.events.WeaponFired;
import twcore.core.util.Tools;

/**
 * Moves bot from location to another and detects hits.
 * In future it will maybe target/aim too..
 * !! UNDER EXTREME CONSTRUCTION !! (works for balance fallout)
 */

public class SpaceShip extends Thread
{

	public static int MOVING_TIME = 100;
	public static int SPEED = 2000;
	public static int THRUST = 160;
	public static int ROTATION = 3;

	double BULLET_SPEED = 5000.0;
	double BOMB_SPEED = 8000.0;

	Object m_bot;
	String m_methodName;

	LinkedList<Projectile> fired_projectiles = new LinkedList<Projectile>();

	BotAction m_botAction;
	BotSettings m_botSettings;

	Ship m_ship;

	int m_mAge = (int)System.currentTimeMillis();
	int m_mStart = 0;

	short x = 8192;
	short y = 8192;
	int vX = 0;
	int vY = 0;
	int d = 0;

	int lastVX = 0;
	int lastVY = 0;

	int iDir = 0;
	int cDir = 0;

	int allowance = 0;

	int targetX = 0;
	int targetY = 0;
	int m_targetStartTime;

	int m_mode;
	int m_shipNumber;

	public SpaceShip(BotAction botAction, BotSettings botSettings, Object b, String method, int s, int m)
	{
		m_botAction = botAction;
		m_botSettings = botSettings;

		m_bot = b;
		m_methodName = method;

		m_mode = m;

		m_ship = m_botAction.getShip();
		m_shipNumber = s;

		setAllowance();
	}

	public void init() {
		m_ship.setShip(m_shipNumber);
		m_mAge = (int)(System.currentTimeMillis());
		start();
	}

	public void updatePosition() {
		if (hasTarget()) {

			checkIfCPReached();
			checkHits();

			iDir = (int)getDirection(x, y, targetX, targetY);
			cDir = (int)getDirection(vX, vY);

			if (iDir != cDir) {
				thrust();
			} else {
				if (d == iDir && (int)getSpeed() < SPEED) {
					thrust();
				}
			}
			rotate();

			x = m_ship.getX();
			y = m_ship.getY();
			if ((x < 0 && vX < 0) || (x > 16384 && vX > 0)) {
				vX *= -1;
			}
			if ((y < 0 && vY < 0) || (y > 16384 && vY > 0)) {
				vY *= -1;
			}
		}

		int tD;
		if (m_mode == 0) {
			tD = 0;
		} else {
			tD = d;
		}

//		m_botAction.sendArenaMessage("faweew "+tD+" "+x+" "+y+" "+vX+" "+vY);

		m_ship.setVelocitiesAndDir(vX, vY, tD);
		m_mAge = (int)(System.currentTimeMillis());
	}

	public void checkHits() {
		ListIterator<Projectile> it = fired_projectiles.listIterator();
		while (it.hasNext()) {
			Projectile b = it.next();

			if (b.isHitting(x, y)) {

		        try
			    {
				    Class<?> parameterTypes[] = { b.getClass() };
					Object      lagReportA[] = { b };
					m_bot.getClass().getMethod(m_methodName, parameterTypes).invoke(m_bot, lagReportA);
		        }
			    catch (Exception e)
				{
		        }

				it.remove();
			} else if (b.getAge() > 5000) {
				it.remove();
			}
		}
	}

	public void checkIfCPReached() {
		if ((x < targetX + 16 && x > targetX - 16) && (y < targetY + 16 && y > targetY - 16)) {

			Integer duration = new Integer((int)(System.currentTimeMillis()) - m_targetStartTime);

	        try
		    {
			    Class<?>    parameterTypes[] = { duration.getClass() };
				Object      lagReportA[] = { duration };
				m_bot.getClass().getMethod(m_methodName, parameterTypes).invoke(m_bot, lagReportA);
	        }
		    catch (Exception e)
			{
				            Tools.printLog("Could not invoke method '" + m_methodName + "()' in class " + m_bot);
            Tools.printStackTrace( e );
	        }
		}
	}

	public void setAllowance() {

/*		int dist = (int)getDistance(targetX, targetY);

		if (dist < 100) {
			allowance = 20;
		} else if (dist < 500) {
			allowance = 10;
		} else if (dist < 1000) {
			allowance = 6;
		} else {
			allowance = 3;
		} */
		allowance = 5;
	}


	public double getSpeed() {
		return Math.sqrt(Math.pow(vX, 2) + Math.pow(vY, 2));
	}

	public double getDistance(int pX, int pY) {
		return Math.sqrt(Math.pow((x - pX), 2) + Math.pow((y - pY), 2));
	}

	public double getDirection(int x1, int y1, int x2, int y2) {
		return getDirection(x2 - x1, y2 - y1);
	}

	public double getDirection(int tx, int ty) {

		double angle;
		double dx = tx;
		double dy = ty;

		if (dx == 0) {
			if (dy > 0) {
				angle = 180.0;
			} else {
				angle = 0.0;
			}
		} else {
			double tAngle = Math.atan(dy / dx) * 180 / Math.PI;

			if (tAngle < 0) {
				tAngle *= -1;
			}

			if (dx > 0) {
				if (dy == 0) {
					angle = 90.0;
				} else if (dy < 0) {
					angle = 90.0 - tAngle;
				} else {
					angle = 90.0 + tAngle;
				}
			} else {
				if (dy == 0) {
					angle = 270.0;
				} else if (dy > 0) {
					angle = 270.0 - tAngle;
				} else {
					angle = 270.0 + tAngle;
				}
			}
		}
		return angle / 9.0;
	}

	public void thrust() {

		double bearing = Math.PI * 2 * (double)d / 40.0;
		vX += (short)(THRUST * Math.sin(bearing));
		vY -= (short)(THRUST * Math.cos(bearing));

		int speed = (int)getSpeed();

		if (speed > SPEED) {
			int nS = speed - SPEED;
			bearing = Math.PI * 2 * getDirection(vX, vY) / 40.0;
			vX -= (short)(nS * Math.sin(bearing));
			vY += (short)(nS * Math.cos(bearing));
		}
	}

	public void rotate() {

		if (iDir == d && m_mode == 1) {
			m_ship.fire(1);
		}

		if (iDir != cDir) {
			int c;

			if (cDir > iDir) {
				c = 40 - cDir + iDir;
			} else {
				c = iDir - cDir;
			}

			if (c < 20) {
				if (d != fixRotation(iDir + allowance)) {

					if (d > fixRotation(iDir + allowance)) {
						c = 40 - d + fixRotation(iDir + allowance);
					} else {
						c = fixRotation(iDir + allowance) - d;
					}

					if (c < ROTATION) {
						d -= c - ROTATION;
					} else {
						d += ROTATION;
					}
					if (d > 40) {
						d -= 40;
					}
				}
			} else {
				if (d != fixRotation(iDir - allowance)) {

					if (d > fixRotation(iDir - allowance)) {
						c = 40 - d + fixRotation(iDir - allowance);
					} else {
						c = fixRotation(iDir - allowance) - d;
					}

					if (40 - c < ROTATION) {
						d += 40 - c - ROTATION;
					} else {
						d -= ROTATION;
					}
					if (d < 0) {
						d += 40;
					}
				}
			}
		} else if (d != cDir) {
			int c;

			if (d > iDir) {
				c = 40 - d + iDir;
			} else {
				c = iDir - d;
			}

			if (c < 20) {
				if (c < ROTATION) {
					d -= c - ROTATION;
				} else {
					d += ROTATION;
				}
				if (d > 40) {
					d -= 40;
				}
			} else {
				if (40 - c < ROTATION) {
					d += 40 - c - ROTATION;
				} else {
					d -= ROTATION;
				}
				if (d < 0) {
					d += 40;
				}
			}
		}
	}

	public int fixRotation(int n) {
		int nN = n;
		if (nN > 40) {
			nN -= 40;
		} else if (nN < 0) {
			nN += 40;
		}
		return nN;
	}

	public boolean hasTarget() {
		return (targetX != 0 && targetY != 0);
	}

	public boolean changeTarget(int x, int y) {
		if ((x <= 16384 && x >= 0) && (y <= 16384 && y >= 0)) {
			targetX = x;
			targetY = y;
			m_targetStartTime = (int)System.currentTimeMillis();
			return true;
		}
		return false;
	}

	public void run()
	{
		try
		{
			while (!interrupted())
			{
				updatePosition();
				Thread.sleep(MOVING_TIME);
			}
		}
		catch (InterruptedException e)
		{
			return;
		}
	}

	public void handleEvent(PlayerPosition event) {
		if (m_mode == 1) {
			double predX = (double)event.getXLocation() + ((double)event.getXVelocity() / 10.0);
			double predY = (double)event.getYLocation() + ((double)event.getYVelocity() / 10.0);
			changeTarget((int)predX, (int)predY);
		}
	}

	public void handleEvent(WeaponFired event) {
		double pSpeed;
		if (event.isType(1)) {
			pSpeed = BULLET_SPEED;
		} else if (event.isType(3)) {
			pSpeed = BOMB_SPEED;
		} else { return; }

		double bearing = Math.PI * 2 * (double)event.getRotation() / 40.0;

		//double bVX = event.getXVelocity() + (short)(pSpeed * Math.sin(bearing));
		//double bVY = event.getYVelocity() - (short)(pSpeed * Math.cos(bearing));

		fired_projectiles.add(new Projectile(m_botAction.getPlayerName(event.getPlayerID()), event.getXLocation() + (short)(10.0 * Math.sin(bearing)), event.getYLocation() - (short)(10.0 * Math.cos(bearing)), event.getXVelocity() + (short)(pSpeed * Math.sin(bearing)), event.getYVelocity() - (short)(pSpeed * Math.cos(bearing)), event.getWeaponType(), event.getWeaponLevel()));
	}

	public int getX() { return x; }

	public int getY() { return y; }

	public void setLocation(int tX, int tY) {
		x = (short)tX;
		y = (short)tY;
	}

	public int getVX() { return vX; }

	public void setVX(int tVX) { vX = tVX; }

	public int getVY() { return vY; }

	public void setVY(int tVY) { vY = tVY; }

	public int getDir() { return d; }

	public void setDir(int tD) { d = tD; }

	public void setMaxSpeed(int tMSpeed) { SPEED = tMSpeed; }

	public void setMaxRotation(int tMRotation) { ROTATION = tMRotation; }

	public void setMaxThrust(int tMThrust) { THRUST = tMThrust; }

	public void setShip(int s) { m_ship.setShip(s); }

	public void reset() {
		x = 8192;
		y = 8192;
		vX = 0;
		vY = 0;

		targetX = 0;
		targetY = 0;
		m_ship.move(x, y);
	}
}