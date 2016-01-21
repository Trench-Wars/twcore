package twcore.bots.multibot.spaceball;

import twcore.core.BotAction;

/**
    SpaceBall Cannon class

*/

class Cannon {

    SBPlayer owner = null;
    int attachTime = 0;
    int x;
    int y;

    BotAction m_botAction;

    /**
        Constructor

        @param   tX is the x location of the cannon's center
        @param   tY is the y location of the cannon's center
        @param    b is the bot action of the bot
    */

    public Cannon(int tX, int tY, BotAction b) {
        x = tX;
        y = tY;
        m_botAction = b;
    }


    /**
        This method is used to determine if player's location is inside the cannon.

        @param   tX is player's x location
        @param   tY is player's y location
        @return  true is returned if distance between cannon & player is less than 32
    */

    public boolean isInArea(int tX, int tY) {
        return getDistance(tX, tY) <= 32;
    }


    /**
        This method calculates the distance between the cannon and player

        @param    tX is player's x location
        @param    tY is player's y location
        @return   the distance between cannon and player is returned
    */

    public double getDistance(int tX, int tY) {
        double dist = Math.sqrt( Math.pow(( tX - x ), 2) + Math.pow(( tY - y ), 2) );
        return dist;
    }

    /**
        This method attachs a player to the cannon.

        @param    p is the player to attach
    */

    public void attach(SBPlayer p) {
        attachTime = (int)(System.currentTimeMillis());
        owner = p;
        owner.setCannon(this);
        m_botAction.setShip(owner.getName(), 2);
        m_botAction.warpTo(owner.getName(), x / 16, y / 16);
        m_botAction.sendPrivateMessage(owner.getName(), "You have been attached to a cannon. To deattach simply warp out (or die.. )");
    }


    /**
        This method deattachs the player from cannon.

    */

    public void deAttach() {
        if (timeAttached() > 3000) {
            owner.setCannon(null);
            m_botAction.setShip(owner.getName(), 1);
            owner = null;
        }
    }

    public int timeAttached() {
        return (int)(System.currentTimeMillis()) - attachTime;
    }

    public String getOwner() {
        return owner.getName();
    }

    public boolean inUse() {
        return owner != null;
    }
}
