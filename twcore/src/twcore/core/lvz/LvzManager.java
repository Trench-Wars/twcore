package twcore.core.lvz;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;

import twcore.core.Session;
import twcore.core.net.GamePacketGenerator;
import twcore.core.util.ByteArray;

/**
    This class does advanced Lvz operations using the extended protocol.
    It allows complete control over almost every aspect of lvz objects,
    and is a subclass of Objset so it can still be used for all of those
    functions. Note that while it does keep track of the status of the
    objects, it is only aware of the data it is provided, so unless you
    set a property of an object it will not be recorded in the LvzObject.

    @author D1st0rt
    @version 06.02.11
*/
public class LvzManager extends twcore.core.lvz.Objset
{
    /** A local copy of data on all objects the bot has used */
    private HashMap<Short, LvzObject> objects;

    /** A queued list of objects to modify */
    private Vector<Short> changeQueue;

    /** The packet generator used to send the object modify packets */
    private GamePacketGenerator m_gpg;

    /**
        Creates a new instance of LvzManager
    */
    public LvzManager()
    {
        super();
        objects = new HashMap<Short, LvzObject>();
        changeQueue = new Vector<Short>();
        m_gpg = ((Session)Thread.currentThread()).getGamePacketGenerator();
    }

    /**
        Queues a layer change for the specified object
        @param id the id of the intended object
        @param layer the intended layer for the object
    */
    public void setLayerToChange(short id, Layer layer)
    {
        if(!changeQueue.contains(id))
            changeQueue.add(id);

        LvzObject obj = getObjectSafely(id);
        obj = obj.setLayer((byte)layer.ordinal());
        obj = obj.updateLayer(true);
        objects.put(id, obj);
    }

    /**
        Queues a mode change for the specified object
        @param id the id of the intended object
        @param mode the intended layer for the object
    */
    public void setModeToChange(short id, Mode mode)
    {
        if(!changeQueue.contains(id))
            changeQueue.add(id);

        LvzObject obj = getObjectSafely(id);
        obj = obj.setLayer((byte)mode.ordinal());
        obj = obj.updateDisplayMode(true);
        objects.put(id, obj);
    }

    /**
        Queues a location change for the specified object
        @param id the id of the intended object
        @param xpixels the intended x location for the object, in pixels
        @param ypixels the intended y location for the object, in pixels
    */
    public void setLocationToChange(short id, int xpixels, int ypixels)
    {
        if(!changeQueue.contains(id))
            changeQueue.add(id);

        LvzObject obj = getObjectSafely(id);
        obj = obj.setXLocation(xpixels);
        obj = obj.setYLocation(ypixels);
        obj = obj.updateLocation(true);
        objects.put(id, obj);
    }

    /**
        Queues a relative positioning change for the specified object
        @param id the id of the intended object
        @param xtype the intended reference point for the x coordinate of the object
        @param ytype the intended reference point for the y coordinate of the object
    */
    public void setLocationTypeToChange(short id, CoordType xtype, CoordType ytype)
    {
        if(!changeQueue.contains(id))
            changeQueue.add(id);

        LvzObject obj = getObjectSafely(id);
        obj = obj.setXLocationType((byte)xtype.ordinal());
        obj = obj.setYLocationType((byte)ytype.ordinal());
        obj = obj.updateLocation(true);
        objects.put(id, obj);
    }

    /**
        Queues an image id change for the specified object
        @param id the id of the intended object
        @param image the id of the intended image for the object
    */
    public void setImageToChange(short id, int image)
    {
        if(!changeQueue.contains(id))
            changeQueue.add(id);

        LvzObject obj = getObjectSafely(id);
        obj = obj.setImageID(image);
        obj = obj.updateImage(true);
        objects.put(id, obj);
    }

    /**
        Queues a display time change for the specified object
        @param id the id of the intended object
        @param time the intended display time for the object, in 1/10 seconds
    */
    public void setTimeToChange(short id, int time)
    {
        if(!changeQueue.contains(id))
            changeQueue.add(id);

        LvzObject obj = getObjectSafely(id);
        obj = obj.setDisplayTime(time);
        obj = obj.updateDisplayTime(true);
        objects.put(id, obj);
    }

    /**
        Executes queued modifications for all objects by sending the object modify packet
        to the entire arena
        @param clearQueue whether to clear the queue after the packet has been sent
    */
    public void doChanges(boolean clearQueue)
    {
        doChanges((short) - 1, clearQueue);
    }

    /**
        Executes queued modifications for all objects by sending the object modify packet
        to the specified player
        @param playerID the player to send the object modifications to
        @param clearQueue whether to clear the queue after the packet has been sent
    */
    public void doChanges(short playerID, boolean clearQueue)
    {
        ByteArray objData = null;

        // Construct the data.
        if( changeQueue.size() > 0 ) {
            objData = new ByteArray( changeQueue.size() * 11);

            for(short id : changeQueue)
            {
                LvzObject obj = getObject(id);
                objData.addByteArray(obj.objUpdateInfo);
            }
        }

        // Check if anything needs to be sent.
        if(objData == null)
            return;

        // Automatically break up the data if it's too big.
        int totalSize = objData.size();
        int i = totalSize;
        int size;

        while( i > 0 ) {
            size = 440;             // Sent in chunks of 440 bytes at max (40 objects)

            if( i < size ) {
                size = i;
            }

            ByteArray objPacket = new ByteArray( size + 4 );
            objPacket.addByte( 0x0A ); // Encapsulating packet type byte.
            objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) );
            objPacket.addByte( 0x36 );
            objPacket.addPartialByteArray( objData, totalSize - i, size );
            m_gpg.sendReliableMessage( objPacket );

            i -= size;
        }

        if(clearQueue)
            changeQueue.clear();
    }

    /**
        Retrieves a LvzObject object with the specified id
        @param id the id of the intended object
        @return the object with the specified id if it exists, or null
    */
    public LvzObject getObject(short id)
    {
        LvzObject obj = objects.get(id);
        return obj;
    }

    /**
        Safely retrieves a LvzObject object with the specified id
        @param id the id of the intended object
        @return the object with the specified id. If it does not exist
        a new object is created, added to the collection and returned.
    */
    public LvzObject getObjectSafely(short id)
    {
        LvzObject obj = objects.get(id);

        if(obj == null) {
            objects.put(id, new LvzObject(id));
            obj = objects.get(id);
        }

        return obj;
    }

    /**
        Retrieves a list of all the object IDs that will be changed.
        @return Vector containing all the IDs.
    */
    public Vector<Short> getQueue() {
        return changeQueue;
    }

    public LinkedList<LvzObject> getObjectModifications() {
        LinkedList<LvzObject> objs = new LinkedList<LvzObject>();

        for(Short id : changeQueue) {
            objs.add(objects.get(id));
        }

        return objs;
    }
}
