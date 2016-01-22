package twcore.bots.multibot.spaceball;

/**
    SpaceBall Projectile class

*/

class Projectile {

    SBPlayer owner;
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

    public Projectile(SBPlayer tO, int tX, int tY, int tVX, int tVY, int t, int l) {
        owner = tO;
        birth = (int)(System.currentTimeMillis());

        x = tX;
        y = tY;
        vX = tVX;
        vY = tVY;
        type = t;
        level = l;
    }

    public SBPlayer getOwner() {
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
        @return   the distance between projectile and bot is returned
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

