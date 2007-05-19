
//////////////////////////////////////////////////////
//
// Filename:
// N/A
//
// Description: 
// Generic x/y container with utility functions.
//
// Usage:
// Nothing special
// 
//
//////////////////////////////////////////////////////


package twcore.bots.ballbot;


public class Position
{
	int m_x;
	int m_y;
	
	Position( int x, int y )
	{
		m_x = x;
		m_y = y;
	}
	
	public int getX()
	{
		return m_x;
	}
	
	public int getY()
	{
		return m_y;
	}
	
	public double GetLength()
	{
		return Math.sqrt( (m_x*m_x)+(m_y*m_y) );
	}
	
	public boolean IsInsideCircle( Position center, double radius )
	{
		return distanceFrom( center ) < radius;	
	}
	
	public String toString()
	{
		return "("+m_x+","+m_y+")";
	}
	
	public double distanceFrom( Position there )
	{
		double factor = 8.0;
		double dx = there.getX() - getX();
		double dy = there.getY() - getY();
		dx/= factor;
		dy/= factor;
		double distance = Math.sqrt( (dx*dx)+(dy*dy) );
		distance*= factor;
		
		return distance;
	}
}

class Math2D
{
	// Accuracy is key for good dink prediction
	public static double IntersectsXAt( Position p, Position v, double x )
	{
		double width = (x - p.getX());
		double height = (width*v.getY())/v.getX();
		return p.getY() + height;
	}
}