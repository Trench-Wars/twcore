package twcore.bots.purepubbot.pointlocation;

import java.util.List;
import java.util.Vector;

import twcore.core.util.Point;
import twcore.core.util.PointLocation;

public class BasePointLocation {

    private PointLocation location;
    private List<Point> verticeFlagRoom;
    private List<Point> verticeMidBase;
    
    public BasePointLocation(){

        List<Point> vertices = new Vector<Point>();
        
        verticeFlagRoom = new Vector<Point>();
        verticeMidBase = new Vector<Point>();
        
        //flagroom coords
        verticeFlagRoom.add(new Point(479, 285));
        verticeFlagRoom.add(new Point(479, 249));
        verticeFlagRoom.add(new Point(545, 249));
        verticeFlagRoom.add(new Point(545, 285));
        verticeFlagRoom.add(new Point(531, 285));
        verticeFlagRoom.add(new Point(531, 282));
        verticeFlagRoom.add(new Point(530, 282));
        verticeFlagRoom.add(new Point(530, 281));
        verticeFlagRoom.add(new Point(527, 281));
        verticeFlagRoom.add(new Point(527, 282));
        verticeFlagRoom.add(new Point(526, 285));
        verticeFlagRoom.add(new Point(523, 285));
        verticeFlagRoom.add(new Point(523, 292));
        verticeFlagRoom.add(new Point(521, 292));
        verticeFlagRoom.add(new Point(517, 281));
        verticeFlagRoom.add(new Point(517, 278));
        verticeFlagRoom.add(new Point(519, 278));
        verticeFlagRoom.add(new Point(519, 276));
        verticeFlagRoom.add(new Point(517, 276));
        verticeFlagRoom.add(new Point(517, 274));
        verticeFlagRoom.add(new Point(515, 274));
        verticeFlagRoom.add(new Point(515, 276));
        verticeFlagRoom.add(new Point(509, 276));
        verticeFlagRoom.add(new Point(509, 274));
        verticeFlagRoom.add(new Point(507, 274));
        verticeFlagRoom.add(new Point(507, 276));
        verticeFlagRoom.add(new Point(505, 276));
        verticeFlagRoom.add(new Point(505, 278));
        verticeFlagRoom.add(new Point(507, 278));
        verticeFlagRoom.add(new Point(507, 281));
        verticeFlagRoom.add(new Point(503, 281));
        verticeFlagRoom.add(new Point(503, 292));
        verticeFlagRoom.add(new Point(501, 292));
        verticeFlagRoom.add(new Point(501, 285));
        verticeFlagRoom.add(new Point(499, 285));
        verticeFlagRoom.add(new Point(499, 284));
        verticeFlagRoom.add(new Point(498, 284));
        verticeFlagRoom.add(new Point(498, 282));
        verticeFlagRoom.add(new Point(497, 282));
        verticeFlagRoom.add(new Point(497, 281));
        verticeFlagRoom.add(new Point(494,281));
        verticeFlagRoom.add(new Point(494, 282));
        verticeFlagRoom.add(new Point(492, 282));
        verticeFlagRoom.add(new Point(493, 284));
        verticeFlagRoom.add(new Point(492, 284));
        verticeFlagRoom.add(new Point(492, 285));
        //

        //mid coords
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

        for(Point p: verticeFlagRoom)
            vertices.add(p);
        
        for(Point p: verticeMidBase)
            vertices.add(p);
        
        location = new PointLocation(vertices, true);
        
    }
    
    public boolean isInMid(Point point){
        if(isInside(point))
            return verticeMidBase.contains(point);
        
        return false;
    }
    
    public boolean isInFlagRoom(Point point){
        if(isInside(point))
            return verticeFlagRoom.contains(point);
        
        return false;
    }
    public boolean isInside(Point point){
        return location.isInside(point);
    }
    
    public boolean isOutSide(Point point){
        return location.isOutside(point);
    }

}
