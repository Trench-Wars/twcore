package twcore.bots.duel1bot;

import java.util.Random;
import java.util.Vector;

public class DuelBox {

    int d_box = -1;
    int d_type = -1;
    int d_Ax1 = 0;
    int d_Ay1 = 0;
    int d_Bx1 = 0;
    int d_By1 = 0;
    int d_safeAx = 0;
    int d_safeAy = 0;
    int d_safeBx = 0;
    int d_safeBy = 0;
    int d_areaXmin = 0;
    int d_areaXmax = 0;
    int d_areaYmin = 0;
    int d_areaYmax = 0;
    boolean inUse = false;
    WarpPoint last;

    Random generator;
    Vector<WarpPoint> randomWarpPoints = new Vector<WarpPoint>();

    public DuelBox(String settings[], String randomPt[], String area[], int b) {
        d_box = b;
        d_type = Integer.parseInt(settings[0]);
        d_safeAx = Integer.parseInt(settings[1]);
        d_safeAy = Integer.parseInt(settings[2]);
        d_safeBx = Integer.parseInt(settings[3]);
        d_safeBy = Integer.parseInt(settings[4]);
        d_Ax1 = Integer.parseInt(settings[5]);
        d_Ay1 = Integer.parseInt(settings[6]);
        d_Bx1 = Integer.parseInt(settings[7]);
        d_By1 = Integer.parseInt(settings[8]);
        d_areaXmin = Integer.parseInt(area[0]);
        d_areaYmin = Integer.parseInt(area[1]);
        d_areaXmax = Integer.parseInt(area[2]);
        d_areaYmax = Integer.parseInt(area[3]);
        for (int i = 0; i < randomPt.length; i += 2)
            randomWarpPoints.add(new WarpPoint(randomPt[i], randomPt[i + 1]));
        generator = new Random();
    }

    public boolean gameType(int gameType) {
        if (d_type == 1 && gameType == 3)
            return true;
        if (d_type == 1 && gameType == 4)
            return true;
        if (d_type == 1 && gameType == 5)
            return true;
        if (d_type == 1 && gameType == 7)
            return true;
        if (d_type == gameType)
            return true;
        else
            return false;
    }

    public WarpPoint getRandomWarpPoint() {
        WarpPoint p = randomWarpPoints.elementAt(generator.nextInt(randomWarpPoints.size()));
        if (p == last)
            return getRandomWarpPoint();
        last = p;
        return p;
    }

    public int getAX() {
        return d_Ax1;
    }

    public int getAY() {
        return d_Ay1;
    }

    public int getBX() {
        return d_Bx1;
    }

    public int getBY() {
        return d_By1;
    }

    public int getSafeAX() {
        return d_safeAx;
    }

    public int getSafeAY() {
        return d_safeAy;
    }

    public int getSafeBX() {
        return d_safeBx;
    }

    public int getSafeBY() {
        return d_safeBy;
    }

    public int getAreaMinX() {
        return d_areaXmin;
    }

    public int getAreaMinY() {
        return d_areaYmin;
    }

    public int getAreaMaxX() {
        return d_areaXmax;
    }

    public int getAreaMaxY() {
        return d_areaYmax;
    }

    public int getBoxNumber() {
        return d_box;
    }

    public void toggleUse() {
        inUse = !inUse;
    }

    public boolean inUse() {
        return inUse;
    }
}

class WarpPoint {

    int d_xLocation;
    int d_yLocation;

    public WarpPoint(String x, String y) {
        d_xLocation = Integer.parseInt(x);
        d_yLocation = Integer.parseInt(y);
    }

    public int getXCoord() {
        return d_xLocation;
    }

    public int getYCoord() {
        return d_yLocation;
    }
}
