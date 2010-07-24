package twcore.bots.purepubbot.pointlocation;

import java.util.List;

import twcore.core.util.Point;
import twcore.core.util.PointLocation;

//Chain of responsibility to calculate where the player is
public abstract class ProcessorPointLocation {

    protected ProcessorPointLocation sucessor;
    protected PointLocation location;
    
    public ProcessorPointLocation(List<Point> coords){
        location = new PointLocation(coords, true);
    }
  
    public void setSucessor(ProcessorPointLocation sucessor){
        this.sucessor = sucessor;
    }
    
    public abstract String getLocation(Point point);
    
}
