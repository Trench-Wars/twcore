package twcore.bots.zonerbot;

import java.util.*;

public class VectorMap implements Map
{
  private Vector keys;
  private Vector values;

  public VectorMap()
  {
    keys = new Vector();
    values = new Vector();
  }

  /**
   * This method gets the size of the VectorMap.
   *
   * @return the number of items in the VectorMap is returned.
   */
  public int size()
  {
    return keys.size();
  }

  /**
   * This method checks to see if the VectorMap is empty.
   *
   * @return true is returend if the VectorMap is empty.
   */
  public boolean isEmpty()
  {
    return keys.isEmpty();
  }

  /**
   * This method checks to see if the VectorMap contains a certain key.
   *
   * @param o is the key to check.
   * @return true is returned if o is a key in the VectorMap.
   */
  public boolean containsKey(Object o)
  {
    return keys.contains(o);
  }

  /**
   * This method checks to see if the VectorMap contains a certain value.
   *
   * @param o is the value to check.
   * @return true is returned if o is a value in the VectorMap.
   */
  public boolean containsValue(Object o)
  {
    return values.contains(o);
  }

  /**
   * This method gets the index of a key in the key Vector.
   *
   * @param o is the key to search for.
   * @return the index of o in the key Vector is returned.  If o is not a key
   * then -1 is returned.
   */
  public int indexOfKey(Object o)
  {
    return keys.indexOf(o);
  }

  /**
   * This method gets the value specified by a certain key.
   *
   * @param o is the key to search for.
   * @return the value indexed by o is returned.  If o is not a key then null
   * is returned.
   */
  public Object get(Object o)
  {
    int index = indexOfKey(o);

    if(index == -1)
      return null;
    return values.get(index);
  }

  /**
   * This method gets the first value in the VectorMap.
   *
   * @return the first value in the VectorMap is returned.
   */
  public Object firstValue()
  {
    if(values.isEmpty())
      return null;
    return values.firstElement();
  }

  /**
   * This method gets the first key in the VectorMap.
   *
   * @return the first key in the VectorMap is returned.
   */
  public Object firstKey()
  {
    if(keys.isEmpty())
      return null;
    return keys.firstElement();
  }

  /**
   * This method gets the last value in the VectorMap.
   *
   * @return the last value in the VectorMap is returned.
   */
  public Object lastValue()
  {
    if(values.isEmpty())
      return null;
    return values.lastElement();
  }

  /**
   * This method gets the last key in the VectorMap.
   *
   * @return the last key in the VectorMap is returned.
   */
  public Object lastKey()
  {
    if(keys.isEmpty())
      return null;
    return keys.lastElement();
  }

  /**
   * This method gets a value at a certain index.
   *
   * @param index is the index to get from.
   * @return the value at the specified index is returned.  If the index is
   * invalid then null is returned.
   */
  public Object get(int index)
  {
    if(index < 0 || index >= values.size())
      return null;
    return values.get(index);
  }

  /**
   * This method gets a key at a certain index.
   *
   * @param index is the index to get from.
   * @return the key at the specified index is returned.  If the index is
   * invalid then null is returned.
   */
  public Object getKey(int index)
  {
    if(index < 0 || index >= keys.size())
      return null;
    return keys.get(index);
  }

  /**
   * This method puts an object into the VectorMap.  If the key is already
   * present then the old value is replaced.  The object is placed at the end
   * of the map.
   *
   * @param key is the key to add.
   * @param value is the value to add.
   * @return the value that was added is returned.
   */
  public Object put(Object key, Object value)
  {
    int index = indexOfKey(key);

    if(index != -1)
      values.set(index, value);
    else
    {
      keys.add(key);
      values.add(value);
    }
    return value;
  }

  /**
   * This method puts an object into the VectorMap.  If the key is already
   * present then the old value is replaced.  The object is placed at a
   * specified index.
   *
   * @param index is the index where the value will be placed.
   * @param key is the key to add.
   * @param value is the value to add.
   * @return the value that was added is returned.
   */
  public Object put(int index, Object key, Object value)
  {
    int oldIndex = indexOfKey(key);

    if(oldIndex != -1)
      remove(oldIndex);
    keys.add(index, key);
    keys.add(index, value);
    return value;
  }

  /**
   * This method clears the mapping of a key from the VectorMap.
   *
   * @param o is the key to remove.
   * @return the value that was removed is returned.
   */
  public Object remove(Object o)
  {
    int index = indexOfKey(o);

    if(index == -1)
      return null;

    keys.remove(index);
    return values.remove(index);
  }

  /**
   * This method clears the mapping of a key at a specified index from the
   * VectorMap.
   *
   * @param index is the location of the value to remove.
   * @return the value that was removed is returned.
   */
  public Object remove(int index)
  {
    if(index < 0 || index >= keys.size())
      return null;
    keys.remove(index);
    return values.remove(index);
  }

  /**
   * This method places all of the mappings from a Map m into the current
   * VectorMap.
   *
   * @param m is the map to copy.
   */
  public void putAll(Map m)
  {
    Set set = m.keySet();
    Iterator iterator = set.iterator();
    Object key;
    Object value;


    while(iterator.hasNext())
    {
      key = iterator.next();
      value = m.get(key);
      put(key, value);
    }
  }

  /**
   * This method clears all mappings in the VectorMap.
   */
  public void clear()
  {
    keys.clear();
    values.clear();
  }

  /**
   * This method returns a set containing the keys of the map.  A LinkedHashSet
   * is returned.
   *
   * @return the set of keys is returned.
   */
  public Set keySet()
  {
    LinkedHashSet set = new LinkedHashSet(keys);
    return set;
  }

  /**
   * This method gets the values of the map.
   *
   * @return the values of the map is returned.
   */
  public Collection values()
  {
    return values;
  }

  /**
   * This method is not supported.
   *
   * @return a RuntimeException is thrown.
   */
  public Set entrySet()
  {
    throw new RuntimeException("VectorMap does not support entrySet()");
  }
}
