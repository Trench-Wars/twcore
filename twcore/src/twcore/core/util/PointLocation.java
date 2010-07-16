package twcore.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to tell if a Point is inside a polygon (list of Point)
 * 
 * Reference: http://www.assemblysys.com/dataServices/php_pointinpolygon.php
 */

public class PointLocation {

	public static final int POINT_VERTEX = 2;
	public static final int POINT_BOUNDARY = 3;
	public static final int POINT_INSIDE = 4;
	public static final int POINT_OUTSIDE = 5;
	
	private boolean pointOnVertex = true;
	private List<Point> vertices;
	
	public PointLocation(List<Point> vertices, boolean pointOnVertex) {
		this.vertices = vertices;
		this.pointOnVertex = pointOnVertex;
	}
	
	public boolean isInside(Point point) {
		int result = isPointInPolygon(point);
		if (result==POINT_INSIDE)
			return true;
		else if (pointOnVertex && (result==POINT_BOUNDARY || result==POINT_VERTEX))
			return true;
		else
			return false;
	}
	
	public boolean isOutside(Point point) {
		return !isInside(point);
	}
	
	private int isPointInPolygon(Point point) {
		
		if (pointOnVertex && isPointOnVertex(point)== true)
			return POINT_VERTEX;
		
		int intersections = 0;
		
		for(int i=1; i<vertices.size(); i++) {
			
			Point v1 = vertices.get(i-1);
			Point v2 = vertices.get(i);
			
			if (v1.y == v2.y 
					&& v1.y == point.y 
					&& point.x > Math.min(v1.x, v2.x) 
					&& point.x < Math.max(v1.x, v2.x))
				return POINT_BOUNDARY;
			
			if (point.y > Math.min(v1.y, v2.y)
				&& point.y <= Math.max(v1.y, v2.y)
				&& point.x <= Math.max(v1.x, v2.x)
				&& v1.y != v2.y) {
				
				int x_inters = (point.y - v1.y) * (v2.x - v1.x) / (v2.y - v1.y) + v1.x;
				
				if (x_inters == point.x)
					return POINT_BOUNDARY;
				
				if (v1.x == v2.x || point.x <= x_inters)
					intersections++;
			}
				
		}
		
		if (intersections%2 != 0)
			return POINT_INSIDE;
		else
			return POINT_OUTSIDE;
		
	}
	
	private boolean isPointOnVertex(Point point) {
		for(Point vertex: vertices) {
			if (point.x == vertex.x && point.y == vertex.y) {
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		
		List<Point> vertices = new ArrayList<Point>();
		vertices.add(new Point(50,50));
		vertices.add(new Point(60,50));
		vertices.add(new Point(60,60));
		vertices.add(new Point(50,60));
		
		
		PointLocation location = new PointLocation(vertices, true);

		System.out.println(location.isPointInPolygon(new Point(49,50)) + ":5");
		System.out.println(location.isPointInPolygon(new Point(51,50)) + ":3");
		System.out.println(location.isPointInPolygon(new Point(51,51)) + ":4");
		System.out.println(location.isPointInPolygon(new Point(60,61)) + ":5");
		System.out.println(location.isPointInPolygon(new Point(55,55)) + ":4");
		System.out.println(location.isPointInPolygon(new Point(55,60)) + ":3");
		
	}
	
}

