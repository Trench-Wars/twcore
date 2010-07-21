package twcore.bots.purepubbot.pointlocation;

import java.util.List;
import java.util.Vector;

import twcore.core.util.Point;
import twcore.core.util.PointLocation;

public class MidPointLocation extends ProcessorPointLocation{

    public MidPointLocation(List<Point> coords) {
        super(coords);
        // TODO Auto-generated constructor stub
    }
  
    public boolean isInside(Point point){
        return location.isInside(point);
    }
    
    public boolean isOutside(Point point){
        return location.isOutside(point);
    }

    @Override
    public String getLocation(Point point) {
        // TODO Auto-generated method stub
        if(isInside(point))
            return "mid";
        else if(sucessor != null)
            sucessor.getLocation(point);
        
        return "spawn";
    }
}
