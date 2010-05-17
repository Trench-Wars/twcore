package twcore.core.util;

public class Point {
	
	public final int x;
	public final int y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public boolean equals(Object p) {
		if (this.x == ((Point)p).x && this.y == ((Point)p).y)
			return true;
		return false;
	}
	
}