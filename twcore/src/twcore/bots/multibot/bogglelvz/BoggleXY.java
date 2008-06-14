package twcore.bots.multibot.bogglelvz;

/**
 * This class holds (X, Y) coordinates for Boggle.
 */
public class BoggleXY {
    public int x, y;
    
    /**
     * The BoggleXY constructor.
     * @param x - The X Coordinate
     * @param y - The Y Coordinate
     */
    public BoggleXY(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Returns the X Coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Returns the Y Coordinate
     */
    public int getY() {
        return y;
    }
}