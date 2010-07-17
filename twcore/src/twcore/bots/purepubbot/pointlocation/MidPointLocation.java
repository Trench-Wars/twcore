package twcore.bots.purepubbot.pointlocation;

import java.util.List;
import java.util.Vector;

import twcore.core.util.Point;
import twcore.core.util.PointLocation;

public class MidPointLocation {

    private PointLocation location;
    
    public MidPointLocation(){
        List<Point> verticeMidBase = new Vector<Point>();
        
        verticeMidBase.add(new Point(472, 286));
        verticeMidBase.add(new Point(471, 272));
        verticeMidBase.add(new Point(448, 272));
        verticeMidBase.add(new Point(461, 334));
        verticeMidBase.add(new Point(563, 334));
        verticeMidBase.add(new Point(575, 295));
        verticeMidBase.add(new Point(575, 272));
        verticeMidBase.add(new Point(552, 271));
        verticeMidBase.add(new Point(552, 286));
        verticeMidBase.add(new Point(529, 286));
        verticeMidBase.add(new Point(529, 283));
        verticeMidBase.add(new Point(528, 283));
        verticeMidBase.add(new Point(528, 286));
        verticeMidBase.add(new Point(527, 286));
        verticeMidBase.add(new Point(527, 287));
        verticeMidBase.add(new Point(525, 287));
        verticeMidBase.add(new Point(525, 294));
        verticeMidBase.add(new Point(519, 294));
        verticeMidBase.add(new Point(519, 283));
        verticeMidBase.add(new Point(515, 283));
        verticeMidBase.add(new Point(515, 278));
        verticeMidBase.add(new Point(509, 278));
        verticeMidBase.add(new Point(509, 283));
        verticeMidBase.add(new Point(505, 283));
        verticeMidBase.add(new Point(505, 294));
        verticeMidBase.add(new Point(499, 294));
        verticeMidBase.add(new Point(499, 286));
        verticeMidBase.add(new Point(496, 28));
        verticeMidBase.add(new Point(496, 283));
        verticeMidBase.add(new Point(495, 283));
        verticeMidBase.add(new Point(495, 286));
        
        location = new PointLocation( verticeMidBase, true );
    }
    
    public boolean isInside(Point point){
        return location.isInside(point);
    }
    
    public boolean isOutside(Point point){
        return location.isOutside(point);
    }
}
