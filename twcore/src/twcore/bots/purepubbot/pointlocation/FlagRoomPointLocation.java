package twcore.bots.purepubbot.pointlocation;

import java.util.List;
import twcore.core.util.Point;

public class FlagRoomPointLocation extends ProcessorPointLocation{
    
    public FlagRoomPointLocation(List<Point> coords) {
        super(coords);
        // TODO Auto-generated constructor stub
       
    }

    public boolean isInside(Point point){
        return location.isInside(point);
    }
    
    public boolean isOutSide(Point point){
        return location.isOutside(point);
    }

    @Override
    public String getLocation(Point point) {
        // TODO Auto-generated method stub
        if(isInside(point))
            return "flagroom";
        else if(sucessor != null)
            sucessor.getLocation(point);
            
        return "spawn";
    }

}
