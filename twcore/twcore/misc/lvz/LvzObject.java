package twcore.misc.lvz;

import twcore.core.ByteArray;

/**
 * Represents one of an arena's lvz objects. Also stores and manipulates data
 * for the lvz object modify packet:
 * 
 * <code><pre> 
 * +-----------------------------+
 * | Offset  Length  Description |
 * +-----------------------------+
 * | 0       1       Type Byte   |
 * | 1       2       Target pID  |
 * | 2       1       Subtype Byte|
 * | 3       1       Update flags|
 * | 4       2       Obj ID/Type |
 * | 7       2       X Position  |
 * | 9       2       Y Position  |
 * | 11      1       Image ID    |
 * | 12      1       Layer       |
 * | 13      2       Time/Mode   |
 * +-----------------------------+</code></pre>
 * 
 * Offsets 3-13 (The lvz bitfield) can optionally repeat to group multiple
 * objects together. 
 * 
 * @author D1st0rt
 * @version 06.01.21
 */
public class LvzObject
{
	//Packet data
	private byte changes;
	private short id;
	private short x;
	private short y;
	private byte image;
	private byte layer;
	private short time;

	//other storage
	boolean mapObj;

	/**
	 * Creates a new LvzObject with the specified id
	 * @param id the object id (objon number) of this object
	 */
	public LvzObject(short id)
	{
		changes = 0;
		this.id = id;
		x = 0;
		y = 0;
		image = 0;
		layer = 0;
		time = 0;
	}

	/**
	 * Gets the id of this object
	 * @return the lvz object id (objon number) of this object
	 */
	public short getID()
	{
		return (short)((id << 1) >> 1);
	}

	/**
	 * Converts this LvzObject into a bytearray for use in a packet
	 * @return the lvz bitfield for this object
	 */
	public ByteArray toByteArray()
	{
		ByteArray data = new ByteArray(11);
		data.addByte(changes);
		data.addLittleEndianShort(id);
		data.addLittleEndianShort(x);
		data.addLittleEndianShort(y);
		data.addByte(image);
		data.addByte(layer);
		data.addLittleEndianShort(time);
		return data;
	}

	/**
	 * Sets this object's status as a map or screen object
	 * @param mapObj whether this object is a map object
	 */
	void setMapObject(boolean mapObj)
	{
		this.mapObj = mapObj;

		if(mapObj)
			id |= 0x8000;
		else
			id &= 0x7FFF;
	}

	/**
	 * Gets this object's status as a map or screen object
	 * @return true if the object is a map object
	 */
	public boolean isMapObject()
	{
		return mapObj;
	}

	/**
	 * Changes this object's mode
	 * @param mode the object's new mode
	 * @see twcore.misc.lvz.Mode
	 */
	void setMode(Mode mode)
	{
		changes |= 8;
		short temptime = (short)((this.time >> 4) << 4);
		short modeval = (short)(0xFFF + mode.ordinal());
		this.time = Short.MIN_VALUE;
		this.time &= temptime;
		this.time &= modeval;
	}

	/**
	 * Gets this object's mode
	 * @return the object's mode
	 * @see twcore.misc.lvz.Mode
	 */
	public Mode getMode()
	{
		short mode = (short)((time << 12) >> 12);
		Mode m = Mode.ShowAlways;
		return m.fromOrdinal(mode);
	}

	/**
	 * Sets the location of this object on the map or screen
	 * @param xpixels the x coordinate in pixels
	 * @param ypixels the y coordinate in pixels
	 */
	public void setLocation(short xpixels, short ypixels)
	{
		changes |= 128;

		if(!mapObj)
		{
			short xmode = (short)((x << 12) >> 12);
			short ymode = (short)((y << 12) >> 12);

			xpixels <<= 4;
			ypixels <<= 4;

			xpixels += 0xF;
			ypixels += 0xF;

			x = Short.MIN_VALUE;
			y = Short.MIN_VALUE;

			x &= xpixels;
			x &= xmode;
			y &= ypixels;
			y &= ymode;
		}
		else
		{
			x = xpixels;
			y = ypixels;
		}

	}

	/**
	 * Gets this object's x location on the map or screen
	 * @return the x coordinate in pixels
	 */
	public short getXLocation()
	{
		if(!mapObj)
			return (short)(x >> 4);
		else
			return x;
	}

	/**
	 * Gets this object's y location on the map or screen
	 * @return the y coordinate in pixels
	 */
	public short getYLocation()
	{
		if(!mapObj)
			return (short)(y >> 4);
		else
			return y;
	}

	/**
	 * Sets the relative location of this object on the screen
	 * Note that this does not apply to map objects and does nothing.
	 * @param xtype the point to base x coordinate positions off of
	 * @param ytype the point to base y coordinate positions off of
	 * @see twcore.misc.lvz.CoordType
	 */
	void setRelativeLocation(CoordType xtype, CoordType ytype)
	{
		if(!mapObj)
		{
			changes |= 128;

			short tempx = (short)((x >> 12) << 12);
			short tempy = (short)((y >> 12) << 12);

			short xmode = (short)(0xFFF + xtype.ordinal());
			short ymode = (short)(0xFFF + ytype.ordinal());

			x = Short.MIN_VALUE;
			y = Short.MIN_VALUE;

			x &= tempx;
			x &= xmode;
			y &= tempy;
			y &= ymode;
		}
	}

	/**
	 * Gets the relative x location of this object on the screen
	 * @return the reference point for the x coordinate
	 */
	public CoordType getXRelative()
	{
		short type = (short)((x << 12) >> 12);
		CoordType t = CoordType.C;
		return t.fromOrdinal(type);
	}

	/**
	 * Gets the relative y location of this object on the screen
	 * @return the reference point for the y coordinate
	 */
	public CoordType getYRelative()
	{
		short type = (short)((y << 12) >> 12);
		CoordType t = CoordType.C;
		return t.fromOrdinal(type);
	}

	/**
	 * Sets the image id for this object
	 * @param image the id of the image this object is to use
	 */
	void setImage(byte image)
	{
		changes |= 64;
		this.image = image;
	}

	/**
	 * Gets the image id for this object
	 * @return the id of the image this object uses
	 */
	public byte getImage()
	{
		return image;
	}

	/**
	 * Sets the display time for this object
	 * @param time the display time the object is to use, in 1/10 seconds
	 */
	void setTime(short time)
	{
		changes |= 16;
		short tempmode = (short)((this.time >> 12) << 12);
		tempmode = (short)(0xFFF + tempmode);
		this.time = Short.MIN_VALUE;
		this.time &= (time << 4);
		this.time &= tempmode;

	}

	/**
	 * Gets the display time for this object
	 * @return the display time for this object, in 1/10 seconds
	 */
	public short getTime()
	{
		return time;
	}

	/**
	 * Sets the object's display layer
	 * @param layer the layer the object is to be displayed on
	 * @see twcore.misc.lvz.Layer
	 */
	void setLayer(Layer layer)
	{
		changes |= 32;
		this.layer = (byte)layer.ordinal();
	}

	/**
	 * Gets the object's display layer
	 * @return the layer the object is displayed on
	 * @see twcore.misc.lvz.Layer
	 */
	public Layer getLayer()
	{
		Layer l = Layer.BelowAll;
		return l.fromOrdinal(layer);
	}
}
