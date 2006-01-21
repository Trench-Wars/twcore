package twcore.misc.lvz;

import java.util.HashMap;
import java.util.Vector;

import twcore.core.ByteArray;
import twcore.core.GamePacketGenerator;
import twcore.core.Objset;
import twcore.core.Session;

/**
 * This class does advanced Lvz operations using the extended protocol.
 * It allows complete control over almost every aspect of lvz objects,
 * and is a subclass of Objset so it can still be used for all of those
 * functions. Note that while it does keep track of the status of the
 * objects, it is only aware of the data it is provided, so unless you
 * set a property of an object it will not be recorded in the LvzObject
 * object.
 * 
 * @author D1st0rt
 * @version 06.01.21
 */
public class LvzManager extends Objset
{
	/** A local copy of data on all objects the bot has used */
	private HashMap<Short, LvzObject> objects;
	
	/** A queued list of objects to modify */	
	private Vector<Short> changeQueue;
	
	/** The packet generator used to send the object modify packets */
	private GamePacketGenerator m_gpg;

	/**
	 * Creates a new instance of LvzManager
	 */
	public LvzManager()
	{
		super();
		objects = new HashMap<Short, LvzObject>();
		changeQueue = new Vector<Short>();
		m_gpg = ((Session)Thread.currentThread()).getGamePacketGenerator();
	}

	/**
	 * Queues a layer change for the specified object
	 * @param id the id of the intended object
	 * @param layer the intended layer for the object
	 */
	public void setToChange(short id, Layer layer)
	{
		if(!changeQueue.contains(id))
			changeQueue.add(id);

		getObjectSafely(id).setLayer(layer);
	}

	/**
	 * Queues a mode change for the specified object
	 * @param id the id of the intended object
	 * @param mode the intended layer for the object
	 */
	public void setToChange(short id, Mode mode)
	{
		if(!changeQueue.contains(id))
			changeQueue.add(id);

		getObjectSafely(id).setMode(mode);
	}

	/**
	 * Queues a location change for the specified object
	 * @param id the id of the intended object
	 * @param xpixels the intended x location for the object, in pixels
	 * @param ypixels the intended y location for the object, in pixels
	 */
	public void setToChange(short id, short xpixels, short ypixels)
	{
		if(!changeQueue.contains(id))
			changeQueue.add(id);

		getObjectSafely(id).setLocation(xpixels, ypixels);
	}

	/**
	 * Queues a relative positioning change for the specified object
	 * @param id the id of the intended object
	 * @param xtype the intended reference point for the x coordinate of the object
	 * @param ytype the intended reference point for the y coordinate of the object
	 */
	public void setToChange(short id, CoordType xtype, CoordType ytype)
	{
		if(!changeQueue.contains(id))
			changeQueue.add(id);

		getObjectSafely(id).setRelativeLocation(xtype, ytype);
	}

	/**
	 * Queues an image id change for the specified object
	 * @param id the id of the intended object
	 * @param image the id of the intended image for the object
	 */
	public void setToChange(short id, byte image)
	{
		if(!changeQueue.contains(id))
			changeQueue.add(id);

		getObjectSafely(id).setImage(image);
	}

	/**
	 * Queues a display time change for the specified object
	 * @param id the id of the intended object
	 * @param time the intended display time for the object, in 1/10 seconds
	 */
	public void setToChange(short id, short time)
	{
		if(!changeQueue.contains(id))
			changeQueue.add(id);

		getObjectSafely(id).setTime(time);
	}

	/**
	 * Executes queued modifications for all objects by sending the object modify packet
	 * to the entire arena
	 * @param clearQueue whether to clear the queue after the packet has been sent
	 */
	public void doChanges(boolean clearQueue)
	{
		ByteArray data = new ByteArray(4 +(11 * changeQueue.size()));
		data.addByte(0x0A);
		data.addLittleEndianShort((short)-1);
		data.addByte(0x36);

		for(short id : changeQueue)
		{
			LvzObject obj = getObject(id);
			data.addByteArray(obj.toByteArray());
		}

		m_gpg.composeHighPriorityPacket(data, data.size());

		if(clearQueue)
			changeQueue.clear();
	}

	/**
	 * Executes queued modifications for all objects by sending the object modify packet
	 * to the specified player
	 * @param playerID the player to send the object modifications to
	 * @param clearQueue whether to clear the queue after the packet has been sent
	 */
	public void doChanges(short playerID, boolean clearQueue)
	{
		ByteArray data = new ByteArray(4 +(11 * changeQueue.size()));
		data.addByte(0x0A);
		data.addLittleEndianShort(playerID);
		data.addByte(0x36);

		for(short id : changeQueue)
		{
			LvzObject obj = getObject(id);
			data.addByteArray(obj.toByteArray());
		}

		m_gpg.composeHighPriorityPacket(data, data.size());
		
		if(clearQueue)
			changeQueue.clear();
	}

	/**
	 * Retrieves a LvzObject object with the specified id
	 * @param id the id of the intended object
	 * @return the object with the specified id if it exists, or null 
	 */
	public LvzObject getObject(short id)
	{
		LvzObject obj = objects.get(id);
		return obj;
	}

	/**
	 * Safely retrieves a LvzObject object with the specified id
	 * @param id the id of the intended object
	 * @return the object with the specified id. If it does not exist
	 * a new object is created, added to the collection and returned.
	 */
	public LvzObject getObjectSafely(short id)
	{
		LvzObject obj = objects.get(id);
		if(obj == null)
			obj = objects.put(id, new LvzObject(id));
		return obj;
	}
}
