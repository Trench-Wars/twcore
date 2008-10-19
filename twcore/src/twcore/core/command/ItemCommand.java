package twcore.core.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twcore.core.BotAction;
import twcore.core.util.Tools;

/**
 * Utility class that helps with the creation and maintenance of user defined
 * "items" with various properties. The class specified in the generic wildcard
 * is the class that bot operators will be able to create through a command
 * interface using the TSM parsing engine. Because ItemCommand uses reflection
 * to create instances and assign values to the fields of the class, the class
 * and any fields that you want to be modifiable must have public access. Using
 * ItemCommand is pretty easy, just pass along message to the four command
 * methods for adding, displaying, editing, and removing.
 *
 * @author D1st0rt
 * @version 06.10.01
 */
public class ItemCommand<T> implements List<T>
{
	/** A list of the items in use by the bot. */
	private List<T> items;

	/** Default values to initialize items to. */
	private T defaults;
	private Class<? extends T> cls;

	/** The fields contained in the item. */
	private Field[] fields;

	/** The values of all of the fields contained in the item. */
	private TempSetting[] fieldParsers;

	/** The BotAction object to use when interacting with the bot. */
	private BotAction m_botAction;

	/** Regular expression pattern to strip name=value pairs from. */
	private final Pattern pattern = Pattern.compile("(\\w+)=((\\w+)|\"([^\"]+)\")");

	/**
	 * Creates a new instance of ItemCommand.
	 * @param botAction the BotAction object for this bot (for sending messages)
	 * @param defaults the default field values that all new items will be
	 *        initialized with
	 * @throws Exception if any reflection access is denied in initial creation,
	 *         the object will fail to create
	 */
	public ItemCommand(BotAction botAction, T defaults, Class<? extends T> cls) throws Exception
	{
		m_botAction = botAction;
		items = Collections.synchronizedList( new ArrayList<T>() );
		this.defaults = defaults;
		this.cls = cls;
		setUpFields();
	}

	/**
	 * Looks through all of the public fields and establishes a parser for
	 * each of them
	 */
	private void setUpFields() throws IllegalAccessException
	{
		fields = defaults.getClass().getFields();
		fieldParsers = new TempSetting[fields.length];

		// If it doesn't have any fields, there's no point in using ItemCommand
		if(fields.length < 1)
		{
			Tools.printLog("ITEMCOMMAND ERROR: no public fields for class "+
							defaults.getClass().getName());
			throw new IllegalAccessException();
		}

		for(int i = 0; i < fields.length; i++)
		{
			Class fieldClass = fields[i].getType();
			Object defVal = fields[i].get(defaults);

			if(fieldClass.getName().equals("int"))
			{
				int intVal = (Integer)defVal;
				fieldParsers[i] = new IntSetting(fields[i].getName(), intVal);
			}
			else if(fieldClass.getName().equals("double"))
			{
				double dblVal = (Double)defVal;
				fieldParsers[i] = new DoubleSetting(fields[i].getName(), dblVal);
			}
			else if(fieldClass.getName().equals("boolean"))
			{
				boolean boolVal = (Boolean)defVal;
				fieldParsers[i] = new BoolSetting(fields[i].getName(), boolVal);
			}
			else if(fieldClass.getName().equals("java.lang.String"))
			{
				String strVal = (String)defVal;
				fieldParsers[i] = new StringSetting(fields[i].getName(), strVal);
			}
			else if(fieldClass.getName().equals("twcore.core.game.Player"))
			{
				fieldParsers[i] = new PlayerSetting(fields[i].getName(), m_botAction);
			}
			else
			{
				Tools.printLog("ITEMCOMMAND ERROR: no parser for field "+
								fields[i].getName());
				throw new IllegalAccessException();
			}
		}
	}

	/**
	 * Sets the fields of a specified item object to the values stored in the
	 * defaults provided to the constructor.
	 * @param object the item to modify
	 */
	private void setDefaultValues(T object)
	{
		try{
			Field[] fields = defaults.getClass().getFields();
			for(Field field : fields)
			{
				field.set(object, field.get(defaults));
			}
		}catch(IllegalAccessException e)
		{
			Tools.printStackTrace(e);
		}
	}

	/**
	 * Retrieves the appropriate parser for a given field name
	 * @param fieldName the name of the field to get the parser for
	 * @return the parser for that field, or null if an invalid field supplied
	 */
	private TempSetting getParser(String fieldName)
	{
		TempSetting parser = null;
		for(int i = 0; i < fields.length; i++)
		{
			if(fieldName.equals(fields[i].getName()))
			{
				parser = fieldParsers[i];
				break;
			}
		}
		return parser;
	}

	/**
	 * This is used for when you want to restrict an integer setting within a certain range of numbers
	 * @param name The name of the setting to restrict
	 * @param min The minimum value to allow
	 * @param max The maximum value to allow
	 */
	public void restrictSetting(String name, int min, int max)
	{
		TempSetting t = getParser(name);
		if(t == null)
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (doesn't exist)");
		else if(! (t instanceof IntSetting))
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (not an int setting)");
		else
		{
			IntSetting iset = (IntSetting)t;
			iset.restrict(min, max);
		}
	}

	/**
	 * This is used for when you want to restrict a double setting within a certain range of numbers
	 * @param name The name of the setting to restrict
	 * @param min The minimum value to allow
	 * @param max The maximum value to allow
	 */
	public void restrictSetting(String name, double min, double max)
	{
		TempSetting t = getParser(name);
		if(t == null)
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (doesn't exist)");
		else if(!(t instanceof DoubleSetting))
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (not a double setting)");
		else
		{
			DoubleSetting dset = (DoubleSetting)t;
			dset.restrict(min, max);
		}
	}

	/**
	 * This is used for when you want to restrict a Player setting within a
	 * certain range of frequencies (inclusive)
	 * @param name The name of the setting to restrict
	 * @param min The minimum frequency >= 0 to allow
	 * @param max The maximum frequency <= 9999 to allow
	 */
	public void restrictPlayerSettingFreq(String name, int min, int max)
	{
		TempSetting t = getParser(name);
		if(t == null)
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (doesn't exist)");
		else if(!(t instanceof PlayerSetting))
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (not a Player setting)");
		else
		{
			PlayerSetting pset = (PlayerSetting)t;
			pset.restrictFreq(min, max);
		}
	}

	/**
	 * Sets whether or not a setting can be set to a player in a particular
     * ship, where ship ranges from 0 (warbird) to 7 (shark).
     * @param name The name of the setting to restrict
     * @param ship the ship to change the value for
	 * @param ok whether the ship is allowed or not
	 */
	public void restrictPlayerSettingShip(String name, int ship, boolean ok)
	{
		TempSetting t = getParser(name);
		if(t == null)
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (doesn't exist)");
		else if(!(t instanceof PlayerSetting))
			Tools.printLog("ItemCmd: Could not restrict field "+ name +" (not a Player setting)");
		else
		{
			PlayerSetting pset = (PlayerSetting)t;
			pset.setShipAllowed(ship, ok);
		}
	}

	/**
	 * Changes the value for a specific field of an existing item.
	 * @param index the index of the item in the list
	 * @param attribute the name of the field to change
	 * @param value the new value for the field
	 */
	private void setAttribute(int index, String attribute, Object value)
	{
		try{
			T item = items.get(index);
			for(Field field : fields)
			{
				if(field.getName().equals(attribute))
				{
					field.set(item, value);
				}
			}
		}catch(IllegalAccessException e)
		{
			Tools.printStackTrace(e);
		}
	}

	/**
	 * Adds a new item object to the list.
	 * @return the index of the newly created object in the list
	 */
	private int addNewObject()
	{
		return addObject(createObject());
	}

	/**
	 * Adds a provided item object to the list
	 * @param object the object to add to the list
	 * @return the index of the object in the list
	 */
	private int addObject(T object)
	{
		int index = -1;
		if(object != null)
		{
			items.add(object);
			index = items.indexOf(object);
		}

		return index;
	}

	/**
	 * Command for adding items
	 * @param name the player that sent the message
	 * @param message the parameters of the message
	 * @return the item that was created
	 */
	public T c_Add(String name, String message)
	{
		int index = addNewObject();
		Matcher regex = pattern.matcher(message);
		while(regex.find())
		{
			TempSetting parser = getParser(regex.group(1));

			if(parser != null)
			{
				String old = parser.getName();
				String val = regex.group(2);

				if(val.startsWith("\"") && val.endsWith("\""))
					val = val.substring(1, val.length()-1);

				Result r = parser.setValue(val);

				if(r.changed)
				{
					setAttribute(index, old, parser.getValue());
					m_botAction.sendPrivateMessage(name, r.response);
				}
				else
				{
					m_botAction.sendPrivateMessage(name, r.response);
				}
			}
		}

		return items.get(index);
	}

	/**
	 * Command for editing items
	 * @param name the player that sent the message
	 * @param message the parameters of the message
	 * @return the item that was modified, or null if bad item id or unchanged
	 */
	public T c_Edit(String name, String message)
	{
		if(items.size() == 0)
		{
			m_botAction.sendPrivateMessage(name, "No items to edit.");
			return null;
		}

		String indexStr = "";
		int index = -1;

		try
		{
			indexStr = message.substring(0, message.indexOf(" "));
			index = Integer.parseInt(indexStr);
		}
		catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Please specify an item number.");
		}

		if(index >= 0 && index < items.size())
		{
			boolean result = false;
			Matcher regex = pattern.matcher(message);
			while(regex.find())
			{
				TempSetting parser = getParser(regex.group(1));
				if(parser != null)
				{
					String old = parser.getName();
					String val = regex.group(2);
					if(val.startsWith("\"") && val.endsWith("\""))
						val = val.substring(1, val.length()-1);

					Result r = parser.setValue(val);

					if(r.changed)
					{
						setAttribute(index, old, parser.getValue());
						result = true;
					}
					else
					{
						m_botAction.sendPrivateMessage(name, r.response);
					}
				}
			}
			if(result)
				return items.get(index);
		}
		else
		{
			m_botAction.sendPrivateMessage(name, "Item number should be between 0 and "+ (items.size() - 1));
		}

		return null;
	}

	/**
	 * Command for removing items
	 * @param name the player that sent the message
	 * @param message the parameters of the message
	 * @return the item that was removed, or null
	 */
	public T c_Remove(String name, String message)
	{
		int index = -1;

		try
		{
			index = Integer.parseInt(message);
			if(index >= 0 && index < items.size())
			{
				T item = items.remove(index);
				m_botAction.sendPrivateMessage(name, "Item #"+ index +" removed.");
				return item;
			}
			else
			{
				m_botAction.sendPrivateMessage(name, "Item number should be between 0 and "+ (items.size() - 1));
			}
		}
		catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Please specify an item number.");
			Tools.printStackTrace(e);
		}

		return null;
	}

	/**
	 * Command for displaying items
	 * @param name the player that sent the message
	 * @param message the parameters of the message
	 */
	public void c_Display(String name, String message)
	{
		if(items.isEmpty())
		{
			m_botAction.sendPrivateMessage(name, "Nothing to display.");
		}
		else
		{
			for(int i = 0; i < items.size(); i++)
			{
				m_botAction.sendPrivateMessage(name, i +". "+ items.get(i));
			}
		}
	}

	/**
	 * Creates a new instance of the item object instantiated with the default
	 * field values given to the constructor.
	 * @return a new instance of the provided item object
	 */
	private T createObject()
	{
		T newRef = null;

		try
		{
			//newRef = ((Class<T>)defaults.getClass()).newInstance();
			newRef = cls.newInstance();
			setDefaultValues(newRef);
		}
		catch(Exception e)
		{
			Tools.printLog("Error: could not instantiate a new item object");
			Tools.printStackTrace(e);
		}

		return newRef;
	}


	//+======================================================================+//
	//|                   java.util.List Interface Compliance                |//
	//+======================================================================+//

	/**
	 * Returns the number of elements in this list.
	 * @see java.util.List#size()
	 */
	public int size()
	{
		return items.size();
	}

	/**
	 * Returns true if this list contains no elements.
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty()
	{
		return items.isEmpty();
	}

	/**
	 * Returns true if this list contains the specified element.
	 * @see java.util.List#contains(Object)
	 */
	public boolean contains(Object elem)
	{
		return items.contains(elem);
	}

	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 * @see java.util.List#iterator()
	 */
	public Iterator<T> iterator()
	{
		return items.iterator();
	}

	/**
	 * Returns an array containing all of the elements in this list in
	 * proper sequence.
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray()
	{
		return items.toArray();
	}

	/**
	 *  Returns an array containing all of the elements in this list in
	 * proper sequence; the runtime type of the returned array is that of
	 * the specified array.
	 * @see java.util.List#toArray(Object[])
	 */
	public <T> T[] toArray(T[] a)
	{
		return items.toArray(a);
	}

	/**
	 * Appends the specified element to the end of this list (optional operation).
	 * @see java.util.List#add(Object)
	 */
	public boolean add(T object)
	{
		return items.add(object);
	}

	/**
	 * Removes the first occurrence in this list of the specified element
	 * (optional operation).
	 * @see java.util.List#remove(Object)
	 */
	public boolean remove(Object o)
	{
		return items.remove(o);
	}

	/**
	 * Returns true if this list contains all of the elements of the
	 * specified collection.
	 * @see java.util.List#containsAll(Collection)
	 */
	public boolean containsAll(Collection<?> c)
	{
		return items.containsAll(c);
	}

	/**
	 * Appends all of the elements in the specified collection to the end of
	 * this list,in the order that they are returned by the specified
	 * collection's iterator (optional operation).
	 * @see java.util.List#addAll(Collection)
	 */
	public boolean addAll(Collection<? extends T> c)
	{
		return items.addAll(c);
	}

	/**
	 * Inserts all of the elements in the specified collection into this list
	 * at the specified position (optional operation).
	 * @see java.util.List#addAll(int, Collection)
	 */
	public boolean addAll(int index, Collection<? extends T> c)
	{
		return items.addAll(index, c);
	}

	/**
	 * Removes from this list all the elements that are contained in the
	 * specified collection (optional operation).
	 * @see java.util.List#removeAll(Collection)
	 */
	public boolean removeAll(Collection<?> c)
	{
		return items.removeAll(c);
	}

	/**
	 * Retains only the elements in this list that are contained in the
	 * specified collection (optional operation).
	 * @see java.util.List#retainAll(Collection)
	 */
	public boolean retainAll(Collection<?> c)
	{
		return items.retainAll(c);
	}

	/**
	 * Removes all of the elements from this list (optional operation).
	 * @see java.util.List#clear()
	 */
	public void clear()
	{
		items.clear();
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * @see java.util.List#equals(Object)
	 */
	public boolean equals(Object o)
	{
		return equals(o);
	}

	/**
	 * Returns a hash code value for the object.
	 * @see java.util.List#hashCode()
	 */
	public int hashCode()
	{
		return hashCode();
	}

	/**
	 * Returns the element at the specified position in this list.
	 * @see java.util.List#get(int)
	 */
	public T get(int index)
	{
		return items.get(index);
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element (optional operation).
	 * @see java.util.List#set(int, Object)
	 */
	public T set(int index, T element)
	{
		return items.set(index, element);
	}

	/**
	 * Inserts the specified element at the specified position in this list
	 * (optional operation).
	 * @see java.util.List#add(int, Object)
	 */
	public void add(int index, T element)
	{
		items.add(index, element);
	}

	/**
	 * Removes the element at the specified position in this list
	 * (optional operation).
	 * @see java.util.List#remove(int)
	 */
	public T remove(int index)
	{
		return items.remove(index);
	}

	/**
	 * Returns the index in this list of the first occurrence of the specified
	 * element, or -1 if this list does not contain this element.
	 * @see java.util.List#indexOf(Object)
	 */
	public int indexOf(Object o)
	{
		return items.indexOf(o);
	}

	/**
	 * Returns the index in this list of the last occurrence of the specified
	 * element, or -1 if this list does not contain this element.
	 * @see java.util.List#lastIndexOf(Object)
	 */
	public int lastIndexOf(Object o)
	{
		return items.lastIndexOf(o);
	}

	/**
	 * Returns a list iterator of the elements in this list (in proper sequence).
	 * @see java.util.List#listIterator()
	 */
	public ListIterator<T> listIterator()
	{
		return items.listIterator();
	}

	/**
	 * Returns a list iterator of the elements in this list (in proper sequence),
	 * starting at the specified position in this list.
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<T> listIterator(int index)
	{
		return items.listIterator(index);
	}

	/**
	 * Returns a view of the portion of this list between the specified
	 * fromIndex, inclusive, and toIndex, exclusive.
	 * @see java.util.List#subList(int, int)
	 */
	public List<T> subList(int fromIndex, int toIndex)
	{
		return items.subList(fromIndex, toIndex);
	}

}
