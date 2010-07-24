package twcore.bots.purepubbot.pointlocation;

import java.util.List;
import java.util.Vector;

import twcore.core.util.Point;
//Facade to Point System
public class AreaPlayer {

    private ProcessorPointLocation flagRoomLocation;
    private ProcessorPointLocation midBaseLocation;
    private List<Point> flagRoomCoords;
    private List<Point> midCoords;
    
    public AreaPlayer(){

        this.flagRoomCoords = initializeFRCoords();
        this.midCoords = initializeMidCoords();
        
        this.flagRoomLocation = new FlagRoomPointLocation(flagRoomCoords);
        this.midBaseLocation = new MidPointLocation(midCoords);
        flagRoomLocation.setSucessor(midBaseLocation);
        
    }
    
    private List<Point> initializeMidCoords(){
        this.midCoords = new Vector<Point>();

        midCoords.add(new Point(472, 286));
        midCoords.add(new Point(471, 272));
        midCoords.add(new Point(448, 272));
        midCoords.add(new Point(461, 334));
        midCoords.add(new Point(563, 334));
        midCoords.add(new Point(575, 295));
        midCoords.add(new Point(575, 272));
        midCoords.add(new Point(552, 271));
        midCoords.add(new Point(552, 286));
        midCoords.add(new Point(529, 286));
        midCoords.add(new Point(529, 283));
        midCoords.add(new Point(528, 283));
        midCoords.add(new Point(528, 286));
        midCoords.add(new Point(527, 286));
        midCoords.add(new Point(527, 287));
        midCoords.add(new Point(525, 287));
        midCoords.add(new Point(525, 294));
        midCoords.add(new Point(519, 294));
        midCoords.add(new Point(519, 283));
        midCoords.add(new Point(515, 283));
        midCoords.add(new Point(515, 278));
        midCoords.add(new Point(509, 278));
        midCoords.add(new Point(509, 283));
        midCoords.add(new Point(505, 283));
        midCoords.add(new Point(505, 294));
        midCoords.add(new Point(499, 294));
        midCoords.add(new Point(499, 286));
        midCoords.add(new Point(496, 28));
        midCoords.add(new Point(496, 283));
        midCoords.add(new Point(495, 283));
        midCoords.add(new Point(495, 286));
        return midCoords;
    }
    private List<Point> initializeFRCoords(){
        this.flagRoomCoords = new Vector<Point>();
        //flagroom coords
        flagRoomCoords.add(new Point(479, 285));
        flagRoomCoords.add(new Point(479, 249));
        flagRoomCoords.add(new Point(545, 249));
        flagRoomCoords.add(new Point(545, 285));
        flagRoomCoords.add(new Point(531, 285));
        flagRoomCoords.add(new Point(531, 282));
        flagRoomCoords.add(new Point(530, 282));
        flagRoomCoords.add(new Point(530, 281));
        flagRoomCoords.add(new Point(527, 281));
        flagRoomCoords.add(new Point(527, 282));
        flagRoomCoords.add(new Point(526, 285));
        flagRoomCoords.add(new Point(523, 285));
        flagRoomCoords.add(new Point(523, 292));
        flagRoomCoords.add(new Point(521, 292));
        flagRoomCoords.add(new Point(517, 281));
        flagRoomCoords.add(new Point(517, 278));
        flagRoomCoords.add(new Point(519, 278));
        flagRoomCoords.add(new Point(519, 276));
        flagRoomCoords.add(new Point(517, 276));
        flagRoomCoords.add(new Point(517, 274));
        flagRoomCoords.add(new Point(515, 274));
        flagRoomCoords.add(new Point(515, 276));
        flagRoomCoords.add(new Point(509, 276));
        flagRoomCoords.add(new Point(509, 274));
        flagRoomCoords.add(new Point(507, 274));
        flagRoomCoords.add(new Point(507, 276));
        flagRoomCoords.add(new Point(505, 276));
        flagRoomCoords.add(new Point(505, 278));
        flagRoomCoords.add(new Point(507, 278));
        flagRoomCoords.add(new Point(507, 281));
        flagRoomCoords.add(new Point(503, 281));
        flagRoomCoords.add(new Point(503, 292));
        flagRoomCoords.add(new Point(501, 292));
        flagRoomCoords.add(new Point(501, 285));
        flagRoomCoords.add(new Point(499, 285));
        flagRoomCoords.add(new Point(499, 284));
        flagRoomCoords.add(new Point(498, 284));
        flagRoomCoords.add(new Point(498, 282));
        flagRoomCoords.add(new Point(497, 282));
        flagRoomCoords.add(new Point(497, 281));
        flagRoomCoords.add(new Point(494,281));
        flagRoomCoords.add(new Point(494, 282));
        flagRoomCoords.add(new Point(492, 282));
        flagRoomCoords.add(new Point(493, 284));
        flagRoomCoords.add(new Point(492, 284));
        flagRoomCoords.add(new Point(492, 285));
        return flagRoomCoords;
    }
    
    public String getLocation(Point point){
        return this.flagRoomLocation.getLocation(point);
    }
}
