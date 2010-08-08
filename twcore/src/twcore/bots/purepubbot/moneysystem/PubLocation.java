package twcore.bots.purepubbot.moneysystem;

import twcore.core.util.Point;
import twcore.core.util.PointLocation;

/**
 * Simple wrapper to get the name of the location
 */
public class PubLocation {

    private PointLocation location;
    private String name;
    
    public PubLocation(PointLocation location, String name){
    	this.location = location;
    	this.name = name;
    }
   
    public boolean isInside(Point point) {
    	return location.isInside(point);
    }

    public String getName() {
    	return name;
    }
}
