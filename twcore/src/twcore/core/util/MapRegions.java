package twcore.core.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import twcore.core.BotSettings;
import twcore.core.CoreData;
import twcore.core.Session;
import twcore.core.game.Player;

/**
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
 * @version 06.06.20
 */
public class MapRegions
{
    private CoreData m_coreData;
    private BufferedImage m_map;
    private ArrayList<Integer> m_regions;

    /**
     * Creates a new instance of MapRegions.
     */
    public MapRegions()
    {
        m_regions = new ArrayList<Integer>();
        m_coreData = ((Session)Thread.currentThread()).getCoreData();

    }

    private File loadFile(String filename)
    {
    	String location = m_coreData.getGeneralSettings().getString("Core Location");
        location += File.separatorChar + "data" + File.separatorChar + "maps";
        location += File.separatorChar + filename;
        return new File(location);
    }

    /** Gets the number of regions established, whether they have
     * a color mapped to them or not
     * @return the number of regions being stored
     */
    public int getRegionCount()
    {
        return m_regions.size();
    }


    public void clearRegions()
    {
    	m_regions.clear();
    }

    /** Loads the region file for this arena from the /data/maps folder,
     * with a specified filename.
     * @param filename the name of the file to load
     * @throws java.io.IOException if the file wasn't loaded, allows bots to
     * let the operator know about it or take other custom action.
     */
    public void loadRegionImage(String filename) throws IOException
    {
        m_map = ImageIO.read(loadFile(filename));
        if(m_map == null)
            throw new IOException("Could not load map");
    }

    /** Helper function for loading region settings that follow a basic
     * formatting convention:<br>
     * <code><b>Region0</b>=(red 0-255),(green 0-255),blue(0-255)<br>
     * <b>Region(n)</b>=...<br></code>
     * @param filename the name of the cfg file to load
     * @return The cfg file in case there is other information to get from it
     */
    public BotSettings loadRegionCfg(String filename) throws IOException
    {
        String[] color;
        int r, g, b;
        File file = loadFile(filename);
        BotSettings cfg = new BotSettings(file);

        int count = 0;
		String region = cfg.getString("Region0");
		while(region != null)
		{
			color = region.split(",");
		 	r = Integer.parseInt(color[0]);
            g = Integer.parseInt(color[1]);
            b = Integer.parseInt(color[2]);
            Color c = new Color(r,g,b);
            m_regions.add(c.getRGB());
            count++;
            region = cfg.getString("Region"+ count);
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
    public boolean checkRegion(int x, int y, int region)
    {
        int mapColor = m_map.getRGB(x, y);
        int regColor = m_regions.get(region);
        return (mapColor == regColor);
    }

    /** Gets the region a point is in
     * @param x The x coordinate (in tiles)
     * @param y The y coordinate (in tiles)
     * @return the index of the region the point is in, or -1
      */
    public int getRegion(int x, int y)
    {
        int region = -1;
        for(int z = 0; z < m_regions.size(); z++)
            if(checkRegion(x,y,z))
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
    private void setRegion(int region, Color color)
    {
        setRegion(region,color.getRGB());
    }

    /** Sets a region's color. This corresponds with a color on the
     * region file.
     * @param region the index of the region
     * @param rgb the color of the region in the region file's rgb value
     */
    private void setRegion(int region, int rgb)
    {
        if(region < m_regions.size() && region > -1)
            m_regions.set(region, rgb);
    }

    /** Sets a region's color. This corresponds with a color on the
     * region file.
     * @param region the index of the region
     * @param r the RGB red value (0-255) of the region's color
     * @param g the RGB green value (0-255) of the region's color
     * @param b the RGB blue value (0-255) of the region's color
     */
    private void setRegion(int region, int r, int g, int b)
    {
        if(region < m_regions.size() && region > -1)
        {
            Color c = new Color(r,g,b);
            m_regions.set(region, c.getRGB());
        }
    }

    /**
     * Checks if a player is in a certain region of the map
     * @param p The Player object of the player
     * @param region the region to check for the player
     * @return true if player is in region, false if not
     */
    public boolean checkRegion(Player p, int region)
    {
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        return checkRegion(x, y, region);
    }

    /**
     * Gets the region index of where a player is
     * @param p The Player object of the player
     * @return the index of the region, or -1 if player is not in any
     */
    public int getRegion(Player p)
    {
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        return getRegion(x, y);
    }
}