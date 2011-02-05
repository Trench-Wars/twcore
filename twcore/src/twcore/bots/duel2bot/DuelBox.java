package twcore.bots.duel2bot;

import java.util.Random;
import java.util.Vector;

public class DuelBox {

    int d_box = -1;
    int d_type = -1;
    int d_Ax1 = 0;
    int d_Ax2 = 0;
    int d_Ay1 = 0;
    int d_Ay2 = 0;
    int d_Bx1 = 0;
    int d_Bx2 = 0;
    int d_By1 = 0;
    int d_By2 = 0;
    int d_safeAx1 = 0;
    int d_safeAx2 = 0;
    int d_safeAy1 = 0;
    int d_safeAy2 = 0;
    int d_safeBx1 = 0;
    int d_safeBx2 = 0;
    int d_safeBy1 = 0;
    int d_safeBy2 = 0;
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
        d_safeAx1 = Integer.parseInt(settings[1]);
        d_safeAy1 = Integer.parseInt(settings[2]);
        d_safeAx2 = Integer.parseInt(settings[3]);
        d_safeAy2 = Integer.parseInt(settings[4]);
        d_safeBx1 = Integer.parseInt(settings[5]);
        d_safeBy1 = Integer.parseInt(settings[6]);
        d_safeBx2 = Integer.parseInt(settings[7]);
        d_safeBy2 = Integer.parseInt(settings[8]);
        d_Ax1 = Integer.parseInt(settings[9]);
        d_Ay1 = Integer.parseInt(settings[10]);
        d_Ax2 = Integer.parseInt(settings[11]);
        d_Ay2 = Integer.parseInt(settings[12]);
        d_Bx1 = Integer.parseInt(settings[13]);
        d_By1 = Integer.parseInt(settings[14]);
        d_Bx2 = Integer.parseInt(settings[15]);
        d_By2 = Integer.parseInt(settings[16]);
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

    public int getAX1() {
        return d_Ax1;
    }

    public int getAY1() {
        return d_Ay1;
    }

    public int getAX2() {
        return d_Ax2;
    }

    public int getAY2() {
        return d_Ay2;
    }

    public int getBX1() {
        return d_Bx1;
    }

    public int getBY1() {
        return d_By1;
    }

    public int getBX2() {
        return d_Bx2;
    }

    public int getBY2() {
        return d_By2;
    }

    public int getSafeAX1() {
        return d_safeAx1;
    }

    public int getSafeAX2() {
        return d_safeAx2;
    }

    public int getSafeAY1() {
        return d_safeAy1;
    }

    public int getSafeAY2() {
        return d_safeAy2;
    }

    public int getSafeBX1() {
        return d_safeBx1;
    }

    public int getSafeBX2() {
        return d_safeBx2;
    }

    public int getSafeBY1() {
        return d_safeBy1;
    }

    public int getSafeBY2() {
        return d_safeBy2;
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