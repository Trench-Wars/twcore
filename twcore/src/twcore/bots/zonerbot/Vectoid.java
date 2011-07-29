package twcore.bots.zonerbot;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Map and double Vector hybrid that implements the functionality of a Map's key-value
 * pairing but also maintains an index for each key-value mapping in the list. 
 * 
 * @param <K> Key to represent a Value
 * @param <V> Value to be mapped to Key
 */
public class Vectoid<K, V> implements Map<K, V> {
    private Vector<K> keys;
    private Vector<V> values;

    public Vectoid() {
        keys = new Vector<K>();
        values = new Vector<V>();
    }

    /**
     * This method gets the size of the Vectoid.
     * @return the number of items in the Vectoid is returned.
     */
    public int size() {
        return keys.size();
    }

    /**
     * This method checks to see if the Vectoid is empty.
     * @return true is returned if the Vectoid is empty.
     */
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    /**
     * This method checks to see if the Vectoid contains a certain key.
     * @param o
     *            is the key to check.
     * @return true is returned if o is a key in the Vectoid.
     */
    public boolean containsKey(Object o) {
        if (o instanceof String)
            o = ((String) o).toLowerCase();
        return keys.contains(o);
    }

    /**
     * This method checks to see if the Vectoid contains a certain value.
     * @param o
     *            is the value to check.
     * @return true is returned if o is a value in the Vectoid.
     */
    public boolean containsValue(Object o) {
        return values.contains(o);
    }

    /**
     * This method gets the index of a key in the key Vector.
     * @param o
     *            is the key to search for.
     * @return the index of o in the key Vector is returned. If o is not a key
     *         then -1 is returned.
     */
    public int indexOfKey(Object o) {
        String k = "";
        if (o instanceof String) {
            k = ((String) o).toLowerCase();
            return keys.indexOf(k);
        } else
            return keys.indexOf(o);
    }

    /**
     * This method gets the value specified by a certain key.
     * @param o
     *            is the key to search for.
     * @return the value indexed by o is returned. If o is not a key then null
     *         is returned.
     */
    public V get(Object o) {
        String str = "";
        int index = -1;
        if (o instanceof String) {
            str = ((String) o).toLowerCase();
            index = indexOfKey(str);
        } else
            index = indexOfKey(o);

        if (index == -1)
            return null;
        return values.get(index);
    }

    /**
     * This method gets the first value in the Vectoid.
     * @return the first value in the Vectoid is returned.
     */
    public V firstValue() {
        if (values.isEmpty())
            return null;
        return values.firstElement();
    }

    /**
     * This method gets the first key in the Vectoid.
     * @return the first key in the Vectoid is returned.
     */
    public K firstKey() {
        if (keys.isEmpty())
            return null;
        return keys.firstElement();
    }

    /**
     * This method gets the last value in the Vectoid.
     * @return the last value in the Vectoid is returned.
     */
    public V lastValue() {
        if (values.isEmpty())
            return null;
        return values.lastElement();
    }

    /**
     * This method gets the last key in the Vectoid.
     * @return the last key in the Vectoid is returned.
     */
    public K lastKey() {
        if (keys.isEmpty())
            return null;
        return keys.lastElement();
    }

    /**
     * This method gets a value at a certain index.
     * @param index
     *            is the index to get from.
     * @return the value at the specified index is returned. If the index is
     *         invalid then null is returned.
     */
    public V get(int index) {
        if (index < 0 || index >= values.size())
            return null;
        return values.get(index);
    }

    /**
     * This method gets a key at a certain index.
     * @param index
     *            is the index to get from.
     * @return the key at the specified index is returned. If the index is
     *         invalid then null is returned.
     */
    public K getKey(int index) {
        if (index < 0 || index >= keys.size())
            return null;
        return keys.get(index);
    }

    /**
     * This method puts an object into the Vectoid. If the key is already
     * present then the old value is replaced. The object is placed at the end
     * of the map.
     * @param key
     *            is the key to add.
     * @param value
     *            is the value to add.
     * @return the value that was added is returned.
     */
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (key instanceof String)
            key = (K) ((String) key).toLowerCase();
            
        int index = indexOfKey(key);

        if (index != -1)
            values.set(index, value);
        else {
            keys.add(key);
            values.add(value);
        }
        return value;
    }

    /**
     * This method puts an object into the Vectoid. If the key is already
     * present then the old value is replaced. The object is placed at a
     * specified index.
     * 
     * @param index
     *            is the index where the value will be placed.
     * @param key
     *            is the key to add.
     * @param value
     *            is the value to add.
     * @return the value that was added is returned.
     */
    @SuppressWarnings("unchecked")
    public V put(int index, K key, V value) {
        if (key instanceof String)
            key = (K) ((String) key).toLowerCase();
        int oldIndex = indexOfKey(key);

        if (oldIndex != -1)
            remove(oldIndex);
        keys.add(index, key);
        values.add(index, value);
        return value;
    }

    /**
     * This method clears the mapping of a key from the Vectoid.
     * 
     * @param o
     *            is the key to remove.
     * @return the value that was removed is returned.
     */
    public V remove(Object o) {
        int index = indexOfKey(o);

        if (index == -1)
            return null;

        keys.remove(index);
        return values.remove(index);
    }

    /**
     * This method clears the mapping of a key at a specified index from the
     * Vectoid.
     * 
     * @param index
     *            is the location of the value to remove.
     * @return the value that was removed is returned.
     */
    public V remove(int index) {
        if (index < 0 || index >= keys.size())
            return null;
        keys.remove(index);
        return values.remove(index);
    }

    /**
     * This method places all of the mappings from a Map m into the current
     * Vectoid.
     * 
     * @param m
     *            is the map to copy.
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        Set<? extends K> set = m.keySet();
        Iterator<? extends K> iterator = set.iterator();
        K key;
        V value;

        while (iterator.hasNext()) {
            key = iterator.next();
            value = m.get(key);
            put(key, value);
        }
    }

    /**
     * This method clears all mappings in the Vectoid.
     */
    public void clear() {
        keys.clear();
        values.clear();
    }

    /**
     * This method returns a set containing the keys of the map. A LinkedHashSet
     * is returned.
     * 
     * @return the set of keys is returned.
     */
    public Set<K> keySet() {
        LinkedHashSet<K> set = new LinkedHashSet<K>(keys);
        return set;
    }

    /**
     * This method gets the values of the map.
     * 
     * @return the values of the map is returned.
     */
    public Collection<V> values() {
        return values;
    }

    /**
     * This method is not supported.
     * 
     * @return a RuntimeException is thrown.
     */
    public Set<Map.Entry<K, V>> entrySet() {
        throw new RuntimeException("Vectoid does not support entrySet()");
    }
}