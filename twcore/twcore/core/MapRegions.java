package twcore.core;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import twcore.core.*;

/** <b>MapRegions 1.0</b><br>
 * Very easy way to check if a player is in a certain part of the map.
 * Can be any shape or size, and you don't have to be a programmer to use
 * it because you can just draw on a map where you want things to be.
 * How it works is it reads in an image where each pixel is a tile on the map.
 * When you specify an (x, y) coordinate, it retrieves the color of that pixel
 * from the image. This color is then checked against defined region colors.
 * All calculations are done in RGB, I recommend using a .png image with the
 * web-safe palette. The image HAS to be either GIF, PNG, or JPEG.
 *
 * @author D1st0rt (Original credit for concept of this system to Dr Brain)
 */
public class MapRegions
{
    private BotAction m_botAction;
    private BufferedImage m_map;
    private int[] m_regions;

    //Creates a new instance of MapRegions
    public MapRegions(BotAction botAction)
    {
        m_regions = new int[0];
        m_botAction = botAction;
    }

    //Creates a new instance of MapRegions with an initial capacity
    public MapRegions(BotAction botAction, int regionCount)
    {
        m_botAction = botAction;
        m_regions = new int[regionCount];
        //store all initial values as -1 so they don't collide with actual colors
        for(int x = 0; x < m_regions.length; x++)
            m_regions[x] = -1;
    }

    /** Sets the number of regions in this map. Any existing regions will
     * be preserved in the new array
     * @param newSize the new number of regions
     */
    public void resizeRegions(int newSize)
    {
        //create a new array of proper size
        int[] temp = new int[newSize];
        //store all initial values as -1 so they don't collide with actual colors
        for(int x = 0; x < temp.length; x++)
            temp[x] = -1;
        //copy data from old array
        System.arraycopy(m_regions, 0, temp, 0, Math.min(m_regions.length, temp.length));
        //change the pointer so that it points to the new one
        m_regions = temp;
    }

    /** Loads the map image into the BufferedImage used to extract colors from
     * @param filename the filename of the image to load.
     */
    private void loadMap(String filename) throws IOException
    {
        //Map Image
        m_map = ImageIO.read(new File(filename));
        if(m_map == null)
            throw new IOException("Map is null");

    }

    /** Loads the region file for this arena from the /data/maps folder,
     * with filename defaulted to the name of the arena.
     * @throws java.io.IOException if the file wasn't loaded, allows bots to
     * let the operator know about it or take other custom action.
     */
    public void loadRegionFile() throws IOException
    {
        loadRegionFile(m_botAction.getArenaName());
    }

    /** Loads the region file for this arena from the /data/maps folder,
     * with a specified filename.
     * @param filename the name of the file to load
     * @throws java.io.IOException if the file wasn't loaded, allows bots to
     * let the operator know about it or take other custom action.
     */
    public void loadRegionFile(String filename) throws IOException
    {
        String location = m_botAction.getCoreData().getGeneralSettings().getString("Core Location");
        loadMap(location +"/data/maps/"+ filename);
    }

    /** Helper function for loading region settings that follow a basic
     * formatting convention:<br>
     * <code><b>Regions</b>=(number of regions contained in file)<br>
     * <b>Region0</b>=(red 0-255),(green 0-255),blue(0-255)<br>
     * <b>Region(n)</b>=...<br></code>
     * @param filename the name of the cfg file to load
     * @return The cfg file in case there is other information to get from it
     */
    public BotSettings loadRegionCfg(String filename) throws IOException
    {
        String[] color;
        int r, g, b;
        File file = m_botAction.getDataFile("maps/"+ filename);
        BotSettings cfg = new BotSettings(file);

        int regions = cfg.getInt("Regions");
        resizeRegions(regions);

        for(int x = 0; x < regions; x++)
        {
            color = cfg.getString("Region"+x).split(",");
            r = Integer.parseInt(color[0]); //bits 16-23    r<<16
            g = Integer.parseInt(color[1]); //bits 8-15      g<<8
            b = Integer.parseInt(color[2]); //bits 0-7       b
            setRegion(x,r,g,b);
            Tools.printLog("Loaded region "+ x +": "+ m_regions[x]);
        }

        return cfg;
    }


    /** Checks to see if a point is within a specified region
     * Evaluates the color of their position against the color of the region
     * @param x The x coordinate (in tiles)
     * @param y The y coordinate (in tiles)
     * @param region the index of the region to check against
     * @return true if point is in region, false if not
     */
    private boolean check(int x, int y, int region)
    {
        int mapColor = m_map.getRGB(x, y);
        int regColor = m_regions[region];
        return (mapColor == regColor);
    }

    /** Gets the region a point is in
     * @param x The x coordinate (in tiles)
     * @param y The y coordinate (in tiles)
     * @return the index of the region the point is in, or -1
      */
    private int getRegion(int x, int y)
    {
        int region = -1;
        for(int z = 0; z < m_regions.length; z++)
            if(check(x,y,z))
            {
                region = z;
                break;
            }

        return region;
    }

    /** Sets a region's color. This corresponds with a color on the
     * region file.
     * @param region the index of the region
     * @param color the color of the region in the region file
     */
    public void setRegion(int region, Color color)
    {
        setRegion(region,color.getRGB());
    }

    /** Sets a region's color. This corresponds with a color on the
     * region file.
     * @param region the index of the region
     * @param rgb the color of the region in the region file's rgb value
     */
    public void setRegion(int region, int rgb)
    {
        if(region < m_regions.length && region > -1)
            m_regions[region] = rgb;
        else
            Tools.printLog("Regions: bad region index");
    }

    /** Sets a region's color. This corresponds with a color on the
     * region file.
     * @param region the index of the region
     * @param r the RGB red value (0-255) of the region's color
     * @param g the RGB green value (0-255) of the region's color
     * @param b the RGB blue value (0-255) of the region's color
     */
    public void setRegion(int region, int r, int g, int b)
    {
        if(region < m_regions.length && region > -1)
        {
            Color c = new Color(r,g,b);
            m_regions[region] = c.getRGB();
        }
        else
            Tools.printLog("Regions: bad region index");
    }


    /** Gets the number of regions established, whether they have
     * a color mapped to them or not
     * @return the number of regions being stored
     */
    public int getRegionCount()
    {
        return m_regions.length;
    }

    /**
     * Checks if a player is in a certain region of the map
     * @param name the name of the player
     * @param region the region to check for the player
     * @return true if player is in region, false if not
     */
    public boolean checkRegion(String name, int region)
    {
        Player p = m_botAction.getPlayer(name);
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        return check(x, y, region);
    }

    /**
     * Checks if a player is in a certain region of the map
     * @param playerID the id of the player
     * @param region the region to check for the player
     * @return true if player is in region, false if not
     */
    public boolean checkRegion(int playerID, int region)
    {
        Player p = m_botAction.getPlayer(playerID);
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        return check(x, y, region);
    }

    /**
     * Gets the region index of where a player is
     * @param name the name of the player
     * @return the index of the region, or -1 if player is not in any
     */
    public int getRegion(String name)
    {
        Player p = m_botAction.getPlayer(name);
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        return getRegion(x, y);
    }

    /**
     * Gets the region index of where a player is
     * @param playerID the id of the player
     * @return the index of the region, or -1 if player is not in any
     */
    public int getRegion(int playerID)
    {
        Player p = m_botAction.getPlayer(playerID);
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        return getRegion(x, y);
    }
}