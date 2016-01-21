package twcore.core.game;

import twcore.core.events.WeaponFired;
import twcore.core.util.Point;

/**
    SpaceBall Projectile class

*/

public class Projectile {

    String owner;
    int birth;

    int x;
    int y;
    int vX;
    int vY;
    int type;
    int level;


    /**
        Constructor

        @param   tO is the owner of the projectile
        @param   tX is the x location where the projectile was fired
        @param   tY is the y location where the projectile was fired
        @param  tVX is the x velocity of the projectile
        @param  tVY is the y velocity of the projectile
        @param    t is the type of the projectile (1 = bullet, 2 = bomb)
        @param    l is the weapon level
    */

    public Projectile(String tO, int tX, int tY, int tVX, int tVY, int t, int l) {
        owner = tO;
        birth = (int)(System.currentTimeMillis());

        x = tX;
        y = tY;
        vX = tVX;
        vY = tVY;
        type = t;
        level = l;
    }

    /**
        Alternative constructor. Used when you are unsure about the needed calculations.
        @param owner Owner of the projectile. (ba.getPlayerName(playerID))
        @param event The original WeaponFired event.
        @param pSpeed The base speed of the projectile, obtained through ba.getPlayer(playerID).getWeaponSpeed().
    */
    public Projectile(String owner, WeaponFired event, double pSpeed) {
        double bearing = 0.5 * Math.PI - Math.PI * 2 * (double)event.getRotation() / 40.0;

        this.owner = owner;
        birth = (int)(System.currentTimeMillis());

        x = event.getXLocation() + (short)(10.0 * Math.cos(bearing));
        y = event.getYLocation() - (short)(10.0 * Math.sin(bearing));
        vX = event.getXVelocity() + (short)(pSpeed * Math.cos(bearing));
        vY = event.getYVelocity() - (short)(pSpeed * Math.sin(bearing));
        type = event.getWeaponType();
        level = event.getWeaponLevel();
    }

    /**
        This function calculates the time until a projectile impacts on the given target, if at all.
        <p>
        To accurately do this, this function will need to know the exact coordinates of the target, including
        the hit box radius. To make everything more realistic, an alive time for the projectile is needed as well.
        <p>
        Please note that this function is only accurate for a non-moving target. Also, it is best to provide the
        coordinates in pixels and not tiles.
        <p>
        Also note, that this function is only accurate for missiles and/or bullets that come straight out of the front
        or back of a ship. Multifire type weapons and burst etc, as well as bouncing is not (yet) implemented.
        @param target The coordinates, in pixels, of the target.
        @param radius The radius of the hit box of the target. This will be treated as a square hit box.
        @param aliveTime The maximum alive time of the projectile in milliseconds.
        @return If the projectile misses: -1; if the projectile instantly hits: 0; otherwise the time to impact in milliseconds.
    */
    public int getImpactTime(Point target, int radius, int aliveTime) {
        int xTime = 0;
        int yTime = 0;

        // Shot is within the hit box of the bot.
        if(x <= target.x + radius && x >= target.x - radius && y <= target.y + radius && y >= target.y - radius)
            return 0;

        Point pStart = new Point(x, y);
        Point pEnd = new Point( (int) ( x + vX * aliveTime / 10000.0 ), (int) ( y + vY * aliveTime / 10000.0 ) );

        if(x < target.x - radius) {
            // Calculate left side collision
            xTime = getLineIntersection(
                        pStart,
                        pEnd,
                        new Point(target.x - radius, target.y - radius),
                        new Point(target.x - radius, target.y + radius));
        } else if (x > target.x + radius) {
            // Calculate right side collision
            xTime = getLineIntersection(
                        pStart,
                        pEnd,
                        new Point(target.x + radius, target.y - radius),
                        new Point(target.x + radius, target.y + radius));
        }

        if(y < target.y - radius) {
            // Calculate top side collision
            yTime = getLineIntersection(
                        pStart,
                        pEnd,
                        new Point(target.x - radius, target.y - radius),
                        new Point(target.x + radius, target.y - radius));
        } else if(y > target.y + radius) {
            // Calculate bottom side collision
            yTime = getLineIntersection(
                        pStart,
                        pEnd,
                        new Point(target.x - radius, target.y + radius),
                        new Point(target.x + radius, target.y + radius));
        }

        // If multiple edges report an impact, return the lowest time value.
        if(xTime > 0 && yTime > 0) {
            if(xTime <= yTime)
                return xTime;
            else
                return yTime;
        } else if(xTime > 0 && yTime <= 0) {
            // This happens when only one of the edges is hit.
            return xTime;
        } else if(xTime <= 0 && yTime > 0) {
            // This happens when only one of the edges is hit.
            return yTime;
        } else {
            // The projectile missed completely.
            return -1;
        }
    }

    /**
        Algorithm that determines if a given line segment intersects with another line segment.
        <p>
        In conjunction with the current class, this basically means that the trajectory of the bullet is
        determined as a line with starting point pStart and end point pEnd. Generally the length of this line
        should be determined through the maximum alive time of the projectile.
        <p>
        The second line should be one of the sides of the collision box of the target. This will only work if the
        target is stationary! This algorithm doesn't account for a moving target. The edge of the hit box is defined
        by it's starting point tStart and end point tEnd.
        <p>
        Theoretically, this algorithm can be applied to tile-based coordinates, although the time to impact might be off.
        If you are unsure, then please convert the coordinates to pixels beforehand.
        (Generally this means tile-coordinates * 16 or tile-coordinates << 4.)
        @param pStart The projectile's originating point.
        @param pEnd The projectile's end-of-life point.
        @param tStart The starting point of an edge of the non-moving hit box.
        @param tEnd The end point of an edge of the non-moving hit box.
        @return When no intersection takes place, -1. Otherwise, the time to impact in milliseconds based on the projectile's speed.
    */
    private int getLineIntersection(Point pStart, Point pEnd, Point tStart, Point tEnd) {
        float dX1, dY1, dX2, dY2;       // Delta x's and y's.
        float s, t;                     // Vector variables.
        int xImpact, yImpact;           // Point of impact.
        dX1 = pEnd.x - pStart.x;
        dY1 = pEnd.y - pStart.y;
        dX2 = tEnd.x - tStart.x;
        dY2 = tEnd.y - tStart.y;

        // Vector math.
        s = (-dY1 * (pStart.x - tStart.x) + dX1 * (pStart.y - tStart.y)) / (-dX2 * dY1 + dX1 * dY2);
        t = ( dX2 * (pStart.y - tStart.y) - dY2 * (pStart.x - tStart.x)) / (-dX2 * dY1 + dX1 * dY2);

        // Collision detected
        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            // Calculate impact points.
            xImpact = (int) (pStart.x + (t * dX1));
            yImpact = (int) (pStart.y + (t * dY1));

            // Calculate the time to impact.
            // It is possible that one of either speeds is 0. We need to counter this to prevent a division by 0!
            // Also, due to order of operation, the speed must be cast to a double to prevent the inner calculation to be 0.
            int timeToImpact = 0;

            if(vX != 0)
                timeToImpact += (int) ( 10000.0 * (Math.abs((x - xImpact) / (double) vX )));

            if(vY != 0)
                timeToImpact += (int) ( 10000.0 * (Math.abs((y - yImpact) / (double) vY )));

            // If we have speed on both the x- and y-axis, average the time.
            if(vX != 0 && vY != 0)
                timeToImpact /= 2;

            return timeToImpact;
        }

        return -1; // No collision
    }

    public String getOwner() {
        return owner;
    }


    /**
        This method is used to see if the projectile is colliding with the bot.

        @param    bX is bot's x location
        @param    bY is bot's y location
        @return   true is returned if the projectile is inside bots "circle"
    */

    public boolean isHitting(int bX, int bY) {

        if (getDistance(bX, bY) <= 32) {
            return true;
        } else {
            return false;
        }
    }


    /**
        This method is used to calculate the distance between the projectile and bot.

        @param    bX is bot's x location
        @param    bY is bot's y location
        @return   the distance between projectile & bot is returned
    */

    public double getDistance(int bX, int bY) {
        double dist = Math.sqrt( Math.pow(( bX - getXLocation() ), 2) + Math.pow(( bY - getYLocation() ), 2) );
        return dist;
    }

    public double getXLocation() {
        return x + vX * getAge() / 10000.0;
    }

    public double getYLocation() {
        return y + vY * getAge() / 10000.0;
    }

    public int getXVelocity() {
        return vX;
    }

    public int getYVelocity() {
        return vY;
    }

    public double getAge() {
        return (int)(System.currentTimeMillis()) - birth;
    }

    public int getMass() {
        return (type + level) * 3;
    }

    public int getType() {
        return type;
    }
}

