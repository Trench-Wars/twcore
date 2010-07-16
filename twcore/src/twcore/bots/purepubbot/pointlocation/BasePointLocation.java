package twcore.bots.purepubbot.pointlocation;

import java.util.List;
import java.util.Vector;

import twcore.core.util.Point;
import twcore.core.util.PointLocation;

public class BasePointLocation {

    private PointLocation location;
    
    public BasePointLocation(){

        List<Point> pubBaseMapVertices = new Vector();
        
        //flagroom coords
        pubBaseMapVertices.add(new Point(479, 285));
        pubBaseMapVertices.add(new Point(479, 249));
        pubBaseMapVertices.add(new Point(545, 249));
        pubBaseMapVertices.add(new Point(545, 285));
        pubBaseMapVertices.add(new Point(531, 285));
        pubBaseMapVertices.add(new Point(531, 282));
        pubBaseMapVertices.add(new Point(530, 282));
        pubBaseMapVertices.add(new Point(530, 281));
        pubBaseMapVertices.add(new Point(527, 281));
        pubBaseMapVertices.add(new Point(527, 282));
        pubBaseMapVertices.add(new Point(526, 285));
        pubBaseMapVertices.add(new Point(523, 285));
        pubBaseMapVertices.add(new Point(523, 292));
        pubBaseMapVertices.add(new Point(521, 292));
        pubBaseMapVertices.add(new Point(517, 281));
        pubBaseMapVertices.add(new Point(517, 278));
        pubBaseMapVertices.add(new Point(519, 278));
        pubBaseMapVertices.add(new Point(519, 276));
        pubBaseMapVertices.add(new Point(517, 276));
        pubBaseMapVertices.add(new Point(517, 274));
        pubBaseMapVertices.add(new Point(515, 274));
        pubBaseMapVertices.add(new Point(515, 276));
        pubBaseMapVertices.add(new Point(509, 276));
        pubBaseMapVertices.add(new Point(509, 274));
        pubBaseMapVertices.add(new Point(507, 274));
        pubBaseMapVertices.add(new Point(507, 276));
        pubBaseMapVertices.add(new Point(505, 276));
        pubBaseMapVertices.add(new Point(505, 278));
        pubBaseMapVertices.add(new Point(507, 278));
        pubBaseMapVertices.add(new Point(507, 281));
        pubBaseMapVertices.add(new Point(503, 281));
        pubBaseMapVertices.add(new Point(503, 292));
        pubBaseMapVertices.add(new Point(501, 292));
        pubBaseMapVertices.add(new Point(501, 285));
        pubBaseMapVertices.add(new Point(499, 285));
        pubBaseMapVertices.add(new Point(499, 284));
        pubBaseMapVertices.add(new Point(498, 284));
        pubBaseMapVertices.add(new Point(498, 282));
        pubBaseMapVertices.add(new Point(497, 282));
        pubBaseMapVertices.add(new Point(497, 281));
        pubBaseMapVertices.add(new Point(494,281));
        pubBaseMapVertices.add(new Point(494, 282));
        pubBaseMapVertices.add(new Point(492, 282));
        pubBaseMapVertices.add(new Point(493, 284));
        pubBaseMapVertices.add(new Point(492, 284));
        pubBaseMapVertices.add(new Point(492, 285));
        //

        //mid coords
        pubBaseMapVertices.add(new Point(472, 286));
        pubBaseMapVertices.add(new Point(471, 272));
        pubBaseMapVertices.add(new Point(448, 272));
        pubBaseMapVertices.add(new Point(461, 334));
        pubBaseMapVertices.add(new Point(563, 334));
        pubBaseMapVertices.add(new Point(575, 295));
        pubBaseMapVertices.add(new Point(575, 272));
        pubBaseMapVertices.add(new Point(552, 271));
        pubBaseMapVertices.add(new Point(552, 286));
        pubBaseMapVertices.add(new Point(529, 286));
        pubBaseMapVertices.add(new Point(529, 283));
        pubBaseMapVertices.add(new Point(528, 283));
        pubBaseMapVertices.add(new Point(528, 286));
        pubBaseMapVertices.add(new Point(527, 286));
        pubBaseMapVertices.add(new Point(527, 287));
        pubBaseMapVertices.add(new Point(525, 287));
        pubBaseMapVertices.add(new Point(525, 294));
        pubBaseMapVertices.add(new Point(519, 294));
        pubBaseMapVertices.add(new Point(519, 283));
        pubBaseMapVertices.add(new Point(515, 283));
        pubBaseMapVertices.add(new Point(515, 278));
        pubBaseMapVertices.add(new Point(509, 278));
        pubBaseMapVertices.add(new Point(509, 283));
        pubBaseMapVertices.add(new Point(505, 283));
        pubBaseMapVertices.add(new Point(505, 294));
        pubBaseMapVertices.add(new Point(499, 294));
        pubBaseMapVertices.add(new Point(499, 286));
        pubBaseMapVertices.add(new Point(496, 28));
        pubBaseMapVertices.add(new Point(496, 283));
        pubBaseMapVertices.add(new Point(495, 283));
        pubBaseMapVertices.add(new Point(495, 286));

        location = new PointLocation(pubBaseMapVertices, true);

    }
    
    public boolean isInside(Point point){
        return location.isInside(point);
    }
    
    public boolean isOutSide(Point point){
        return location.isOutside(point);
    }

}
