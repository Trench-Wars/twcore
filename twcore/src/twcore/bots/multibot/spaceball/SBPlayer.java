package twcore.bots.multibot.spaceball;

/**
    SpacelBall Player class

*/

class SBPlayer {

    String name;
    int frequency = 0;
    int bulletsFired = 0;
    int bombsFired = 0;
    int bulletsHit = 0;
    int bombsHit = 0;
    int kills = 0;
    int deaths = 0;

    int lastEShutDown = 0;

    boolean lagged = false;

    Cannon c = null;


    /**
        Constructor

        @param   player is the name of the player
        @param   team is the team # of the player
    */

    public SBPlayer(String player, int team) {
        name = player;
        frequency = team;
    }

    public String getName() {
        return name;
    }

    public int getTeam() {
        return frequency;
    }

    public void setTeam(int t) {
        frequency = t;
    }

    public int timeFromEShutDown() {
        return (int)(System.currentTimeMillis() / 1000) - lastEShutDown;
    }

    public void incrementBulletsFired() {
        bulletsFired++;
    }

    public void incrementBombsFired() {
        bombsFired++;
    }

    public void incrementBulletsHit() {
        bulletsHit++;
    }

    public void incrementBombsHit() {
        bombsHit++;
    }

    public void setLastEShutDown() {
        lastEShutDown = (int)(System.currentTimeMillis() / 1000);
    }

    public int getBulletsFired() {
        return bulletsFired;
    }

    public int getBulletsHit() {
        return bulletsHit;
    }

    public int getBombsFired() {
        return bombsFired;
    }

    public int getBombsHit() {
        return bombsHit;
    }

    public int getTotalFired() {
        return bulletsFired + bombsFired;
    }

    public int getTotalHits() {
        return bulletsHit + bombsHit;
    }

    public void setCannon(Cannon tC) {
        c = tC;
    }

    public Cannon getCannon() {
        return c;
    }

    public boolean isCannon() {
        return c != null;
    }

    public void setLagged(boolean b) {
        lagged = b;
    }

    public boolean isLagged() {
        return lagged;
    }

    public int getAccuracy() {
        if (getTotalHits() == 0 || getTotalFired() == 0) {
            return 0;
        } else {
            return (getTotalHits() * 100) / getTotalFired();
        }
    }

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void incrementDeaths() {
        deaths++;
    }
}
