package twcore.bots.purepubbot.pointlocation;

import java.util.List;
import java.util.Vector;

import twcore.core.util.Point;
import twcore.core.util.PointLocation;

public class FlagRoomPointLocation {

    private PointLocation location;
    
    public FlagRoomPointLocation(){

        List<Point> verticeFlagRoom = new Vector<Point>();
       
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
 
        location = new PointLocation(verticeFlagRoom, true);
        
    }

    public boolean isInside(Point point){
        return location.isInside(point);
    }
    
    public boolean isOutSide(Point point){
        return location.isOutside(point);
    }

}
