package twcore.core.lvz;

/**
 * The LvzObject struct can be used to build update packet to modify an LVZ object, or parse data
 * from an incoming update packet.
 * <P>
 * For in-depth help on any lvz property, see the documentation included with the LVZ toolkit
 * <P>
 * <PRE>Data Layout
 *  Field   Length  Description
 *      0        1      Update Flags
 *      1        2      Object ID & Object Type
 *      3        2      Location [& Type] (x coord)
 *      5        2      Location [& Type] (y coord)
 *      7        1      LVZ Image ID
 *      8        1      LVZ Layer
 *      9        2      Display Time & Mode
 * </PRE>
 * <p>
 * <b>Breakdown of combined bytes</b><br>
 * <i>Note:</i> This will be highly confusing. When data is spread over multiple bytes, the field order is as follows:<br>
 * Byte1[7654 3210] Byte0[FEDC BA98] (Little-endian)
 * <pre>Update Flags
 *  Field     Bits  Description
 *      0        1      Update the x-position AND anchor AND the y-position AND anchor
 *      1        1      Update the image ID
 *      2        1      Update the layer
 *      3        1      Update the display timing
 *      4        1      Update the display mode
 *      5        3      Reserved/unknown?
 *      
 * Object ID & Type
 *  Field     Bits  Description
 *      0        1      0: Screen object; 1: Map object
 *      1       15      Object ID: 0~32,767
 *      
 * Location & Anchoring Screen objects
 *  Field     Bits  Description
 *      0        4      Anchor point: 0~10 (See CoordType class)
 *      4       12      Coordinate offset: -2048~+2047
 *      
 * Location Map objects
 *  Field     Bits  Description
 *      0       16      Map coordinate in pixels: 0~65,535
 *      
 * Display Time & Mode
 *  Field     Bits  Description
 *      0        4      Display mode: 0~5
 *      4       12      Display time: 0~4095
 * </pre>
 * @author Cerium (Shamelessly ripped from Hybrid by D1st0rt)
 */
public class LvzObject implements Cloneable
{
 	/** LVZ update bytes */
 	public final byte[]	objUpdateInfo;
 	
 	private static final short UINT8_MASK = 0xFF; // Used to correctly convert Java bytes to unsigned data. (Thanks to FatRolls for pointing the bug out.)


	// Constructors
	////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Creates a new instance of the <tt>LvzObject</tt> struct, with all fields set to 0.
	 */
	public LvzObject()
	{
		this.objUpdateInfo = new byte[11];
	}

	/**
	 * Creates a new instance of the <tt>LvzObject</tt> struct, with the object ID set to the number
	 * specified.
	 *
	 * @param	intObjID	The object's object id.
	 */
	public LvzObject(int intObjID)
	{
		this.objUpdateInfo = new byte[11];
		this.setObjectID(intObjID);
	}
	
	/**
	 * Creates a new instance of the <tt>LvzObject</tt> struct, with the object ID set to the number
     * specified and converted into a map object if specified.
     * 
	 * @param intObjID      The object's object id. 
	 * @param isMapObject   True if the object is a map object, false otherwise.
	 */
	public LvzObject(int intObjID, boolean isMapObject)
    {
        this.objUpdateInfo = new byte[11];
        intObjID = (intObjID << 1) | (isMapObject?0x01:0x00);

        this.objUpdateInfo[1] = (byte) ((intObjID & 0x00FF));
        this.objUpdateInfo[2] = (byte) ((intObjID & 0xFF00) >> 8);
    }

	/**
	 * Creates a new instance of the <tt>LvzObject</tt> struct, with the byte array set to the array
	 * specified.
	 *
	 * @param	objUpdateInfo	The new byte array to use.
	 */
	public LvzObject(byte[] objUpdateInfo)
	{
		if(objUpdateInfo == null || objUpdateInfo.length != 11) { throw new IllegalArgumentException(); }
		this.objUpdateInfo = objUpdateInfo;
	}


	// Cloneable Interface Functions
	////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Returns a copy of this <tt>LvzObject</tt> struct. The copies are independant. Changes made
	 * to either object will not effect the other.
	 *
	 * @return See above.
	 */
	public Object	clone()
	{
		LvzObject objReturn = new LvzObject();
		System.arraycopy(this.objUpdateInfo, 0, objReturn.objUpdateInfo, 0, 11);

		return objReturn;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Property Get
	////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns <tt>true</tt> if the position of the specified LVZ object should be updated,
	 * <tt>false</tt> otherwise.
	 *
	 * @return See above.
	 */
	public boolean			updateLocation()	{ return (this.objUpdateInfo[0] & 0x01) != 0; }

	/**
	 * Returns <tt>true</tt> if the image of the specified LVZ object should be updated, <tt>false</tt>
	 * otherwise.
	 *
	 * @return See above.
	 */
	public boolean			updateImage()		{ return (this.objUpdateInfo[0] & 0x02) != 0; }

	/**
	 * Returns <tt>true</tt> if the layer of the specified LVZ object should be updated, <tt>false</tt>
	 * otherwise.
	 *
	 * @return See above.
	 */
	public boolean			updateLayer()		{ return (this.objUpdateInfo[0] & 0x04) != 0; }

	/**
	 * Returns <tt>true</tt> if the display time of the specified LVZ object should be updated,
	 * <tt>false</tt> otherwise.
	 *
	 * @return See above.
	 */
	public boolean			updateDisplayTime()	{ return (this.objUpdateInfo[0] & 0x08) != 0; }

	/**
	 * Returns <tt>true</tt> if the display mode of the specified LVZ object should be updated,
	 * <tt>false</tt> otherwise.
	 *
	 * @return See above.
	 */
	public boolean			updateDisplayMode()	{ return (this.objUpdateInfo[0] & 0x10) != 0; }

	/**
	 * Returns <tt>true</tt> if the this LVZ object is a map object, <tt>false</tt> if its a screen
	 * object.
	 *
	 * @return See above.
	 */
	public boolean			isMapObject()		{ return (this.objUpdateInfo[1] & 0x01) != 0; }

	/**
	 * Returns the object id for the lvz object this struct represents.
	 *
	 * @return See above.
	 */
	public int				getObjectID()		{ return ((this.objUpdateInfo[1] & UINT8_MASK) | ((this.objUpdateInfo[2] & UINT8_MASK) << 8)) >>> 1; }

	/**
	 * Returns the position of this lvz object, in pixels. For map objects, the origin is the upper left
	 * corner of the map. The origin for screen objects is dependant on the position type used.
	 *
	 * @return See above.
	 */
	public int				getXLocation()		{ return ((this.objUpdateInfo[3] & UINT8_MASK) | ((this.objUpdateInfo[4] & UINT8_MASK) << 8)) >>> (this.isMapObject() ? 0 : 4); }

	/**
	 * Returns the position of this lvz object, in pixels. For map objects, the origin is the upper left
	 * corner of the map. The origin for screen objects is dependant on the position type used.
	 *
	 * @return See above.
	 */
	public int				getYLocation()		{ return ((this.objUpdateInfo[5] & UINT8_MASK) | ((this.objUpdateInfo[6] & UINT8_MASK) << 8)) >>> (this.isMapObject() ? 0 : 4); }

	/**
	 * Returns the position type for this lvz object. The value returned by this function should be
	 * compared to the constants in the {@link CoordType} class to determine
	 * the origin to use when positioning the LVZ.
	 * <P>
	 * <B>Note</B>: This will always return <tt>0</tt> if this lvz object is not a screen object.
	 *
	 * @return See above.
	 */
	public byte				getXLocationType()	{ return (byte) (this.isMapObject() ? this.objUpdateInfo[3] & 0x0F : 0); }

	/**
	 * Returns the position type (origin) for this lvz object. The value returned by this function
	 * should be compared to the constants in the {@link CoordType} class.
	 * <P>
	 * <B>Note</B>: This will always return <tt>0</tt> if this lvz object is not a screen object.
	 *
	 * @return See above.
	 */
	public byte				getYLocationType()	{ return (byte) (this.isMapObject() ? this.objUpdateInfo[5] & 0x0F : 0); }

	/**
	 * Returns the image this lvz object is currently using. For the object to be visible, it must use a
	 * valid image id, specified in one of the lvz files currently being used in the level.
	 *
	 * @return See above.
	 */
	public int			   getImageID() 		{ return (this.objUpdateInfo[7] & UINT8_MASK ); }

	/**
	 * Returns the layer this lvz object will be displayed on. The value returned by this function can
	 * be compared to the constants in the <tt>hybrid.core.consts.LVZLayers</tt> class.
	 *
	 * @return See above.
	 */
	public int				getLayer()			{ return (this.objUpdateInfo[8] & UINT8_MASK); }

	/**
	 * Returns the amount of time the lvz object will be displayed (in centiseconds) before it will be
	 * automatically hidden.
	 *
	 * @return See above.
	 */
	public int				getDisplayTime()	{ return ((this.objUpdateInfo[9] & UINT8_MASK) | ((this.objUpdateInfo[10] & UINT8_MASK) << 8)) & 0x0FFF; }

	/**
	 * Returns the display mode for this lvz object. The value returned by this function should be
	 * compared to the constants in the {@link Mode} class.
	 *
	 * @return See above.
	 */
	public byte				getDisplayMode()	{ return (byte) ((this.objUpdateInfo[10] & UINT8_MASK) >>> 4); }


	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Property Set
	////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Toggles the 'position' flag. Should be set to <tt>true</tt> when the specified LVZ objects
	 * position should be updated.
	 *
	 * @param	boolUpdate	<tt>true</tt> when the LVZ objects position should be updated,
	 *						<tt>false</tt> otherwise.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	updateLocation(boolean boolUpdate)
	{
		if(boolUpdate) { this.objUpdateInfo[0] |= 0x01; } else { this.objUpdateInfo[0] &= 0xFE; }
		return this;
	}

	/**
	 * Toggles the 'image' flag. Should be set to <tt>true</tt> when the specified LVZ objects image
	 * should be updated.
	 *
	 * @param	boolUpdate	<tt>true</tt> when the LVZ objects image should be updated, <tt>false</tt>
	 *						otherwise.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	updateImage(boolean boolUpdate)
	{
		if(boolUpdate) { this.objUpdateInfo[0] |= 0x02; } else { this.objUpdateInfo[0] &= 0xFD; }
		return this;
	}

	/**
	 * Toggles the 'layer' flag. Should be set to <tt>true</tt> when the specified LVZ objects layer
	 * should be updated.
	 *
	 * @param	boolUpdate	<tt>true</tt> when the LVZ objects layer should be updated, <tt>false</tt>
	 *						otherwise.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	updateLayer(boolean boolUpdate)
	{
		if(boolUpdate) { this.objUpdateInfo[0] |= 0x04; } else { this.objUpdateInfo[0] &= 0xFB; }
		return this;
	}

	/**
	 * Toggles the 'display time' flag. Should be set to <tt>true</tt> when the specified LVZ objects
	 * display time should be updated.
	 *
	 * @param	boolUpdate	<tt>true</tt> when the LVZ objects display time should be updated,
	 *						<tt>false</tt> otherwise.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	updateDisplayTime(boolean boolUpdate)
	{
		if(boolUpdate) { this.objUpdateInfo[0] |= 0x08; } else { this.objUpdateInfo[0] &= 0xF7; }
		return this;
	}

	/**
	 * Toggles the 'display mode' flag. Should be set to <tt>true</tt> when the specified LVZ objects
	 * display mode should be updated.
	 *
	 * @param	boolUpdate	<tt>true</tt> when the LVZ objects display mode should be updated,
	 *						<tt>false</tt> otherwise.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	updateDisplayMode(boolean boolUpdate)
	{
		if(boolUpdate) { this.objUpdateInfo[0] |= 0x10; } else { this.objUpdateInfo[0] &= 0xEF; }
		return this;
	}

	/**
	 * Defines this lvz object as a 'map object'.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	asMapObject()
	{
		this.objUpdateInfo[1] |= 0x01;
		return this;
	}

	/**
	 * Defines this lvz object as a 'screen object'.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	asScreenObject()
	{
		this.objUpdateInfo[1] &= 0xFE;
		return this;
	}

	/**
	 * Sets the id for the lvz object this structure represents. This does not edit or replacing an
	 * existing object id.
	 *
	 * @param	intObjectID		The id for the lvz object to edit.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setObjectID(int intObjectID)
	{
		intObjectID = (intObjectID << 1) | (this.objUpdateInfo[1] & 0x01);

		this.objUpdateInfo[1] = (byte) (intObjectID & 0x00FF);
		this.objUpdateInfo[2] = (byte) ((intObjectID & 0xFF00) >> 8);

		return this;
	}

	/**
	 * Sets the distance from this objects origin, in pixels. For map objects, the origin is the upper
	 * left corner of the map. For screen objects, the origin is determined by the position type.
	 *
	 * @param	intLocation		Distance from the origin, in pixels.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setXLocation(int intLocation)
	{
		if((this.objUpdateInfo[1] & 0x01) == 0) { intLocation = (intLocation << 4) | (this.objUpdateInfo[3] & 0x0F); }

		this.objUpdateInfo[3] = (byte) (intLocation & 0x00FF);
		this.objUpdateInfo[4] = (byte) ((intLocation & 0xFF00) >> 8);

		return this;
	}

	/**
	 * Sets the distance from this objects origin, in pixels. For map objects, the origin is the upper
	 * left corner of the map. For screen objects, the origin is determined by the position type.
	 *
	 * @param	intLocation		Distance from the origin, in pixels.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setYLocation(int intLocation)
	{
		if((this.objUpdateInfo[1] & 0x01) == 0) { intLocation = (intLocation << 4) | (this.objUpdateInfo[5] & 0x0F); }

		this.objUpdateInfo[5] = (byte) (intLocation & 0x00FF);
		this.objUpdateInfo[6] = (byte) ((intLocation & 0xFF00) >> 8);

		return this;
	}

	/**
	 * Sets the position type for this lvz object.
	 * <P>
	 * <B>Note</B>: Location types only apply to screen objects. If this lvz object is a map object,
	 * 				this function will return silently.
	 *
	 * @param	bType	The position type to use. Valid position types are defined in the class
	 *					{@link CoordType}.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setXLocationType(byte bType)
	{
		if((this.objUpdateInfo[1] & 0x01) == 0) { this.objUpdateInfo[3] = (byte) ((this.objUpdateInfo[3] & 0xF0) | (bType & 0x0F)); }

		return this;
	}

	/**
	 * Sets the position type for this lvz object.
	 * <P>
	 * <B>Note</B>: Location types only apply to screen objects. If this lvz object is a map object,
	 * 				this function will return silently.
	 *
	 * @param	bType	The position type to use. Valid position types are defined in the class
	 *					{@link CoordType}.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setYLocationType(byte bType)
	{
		if((this.objUpdateInfo[1] & 0x01) == 0) { this.objUpdateInfo[5] = (byte) ((this.objUpdateInfo[5] & 0xF0) | (bType & 0x0F)); }

		return this;
	}

	/**
	 * Sets the image this lvz object should use. The image must be define in one of the level files
	 * currently in use in the arena.
	 *
	 * @param	intImageID		The new image id for this lvz object.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setImageID(int intImageID)
	{
		this.objUpdateInfo[7] = (byte) (intImageID & 0xFF);
		return this;
	}

	/**
	 * Sets the layer to display this lvz object on.
	 *
	 * @param	bLayer		The layer to display this object on. Valid layer types are defined in the
	 *						class {@link Layer}.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setLayer(byte bLayer)
	{
		this.objUpdateInfo[8] = bLayer;
		return this;
	}

	/**
	 * Sets the amount of time to display this lvz object, in centiseconds, before hiding it. This
	 * setting is ignored for lvz objcts whos display mode is 'ShowAlways'. For all other modes, setting
	 * this value to 0 will display it indefinitely.
	 *
	 * @param	intDisplayTime	Amount of time to display this object, in centiseconds.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setDisplayTime(int intDisplayTime)
	{
		this.objUpdateInfo[9] = (byte) (intDisplayTime & 0x00FF);
		this.objUpdateInfo[10] = (byte) ((intDisplayTime & 0x0F00) >> 8);

		return this;
	}

	/**
	 * Sets the display mode for this lvz object. The display mode effects when the lvz object will be
	 * displayed by the clients.
	 *
	 * @param	bDisplayMode	The new display mode for this lvz object. Value display modes are
	 *							defined in the class {@link Mode}.
	 *
	 * @return This <tt>LvzObject</tt> structure.
	 */
	public LvzObject	setDisplayMode(byte bDisplayMode)
	{
		this.objUpdateInfo[10] = (byte) ((this.objUpdateInfo[10] & 0x0F) | ((bDisplayMode << 4) & 0xF0));
		return this;
	}
}