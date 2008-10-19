/**
 * @(#)ShortMap.java
 *
 * A synchronized map implementation (key -> value) where keys are ints instead of Integer objects. Methods
 * that take a key are overridden with int versions.
 *
 * This is intended for situations where the keys are shorts, or ints that vary mostly in their lower 2 bytes
 * (eg. numbers between 0-65535) such as player IDs.
 *
 * The equals() method considers mappings to be (Integer) -> Object
 * This map outperforms Hashtable and HashMap for basic operations (put, get, remove, contains) as
 * long as there are less than about 4000 entries. Arenas in subgame can't hold more than about 250 players.
 *
 * @author flibb <ER>
 * @version 1.00 2007/10/6
 */
package twcore.core.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ShortMap<V> implements Map<Integer, V> {

	private final static int	TABLE_LEN = 256;
	private final Entry<V>[]	m_table = new Entry[TABLE_LEN];
	private int					m_size = 0;
	private Set<Map.Entry<Integer, V>>	m_entrySet = null;
	private Set<Integer>		m_keySet = null;
	private Collection<V>		m_values = null;

    public ShortMap() {
    }

    public ShortMap(Map<? extends Integer, ? extends V> m) {
    	putAll(m);
    }

    public synchronized void clear() {
    	for(int i = 0; i < TABLE_LEN; i++) {
    		m_table[i] = null;
    	}
    	m_size = 0;
    }

    public synchronized boolean containsKey(Object key) {
    	if(key instanceof Number) {
	    	return containsKey(((Number)key).intValue());
		}
    	return false;
	}

    public synchronized boolean containsKey(int key) {
		return getEntry(key) != null;
    }

	public synchronized boolean containsValue(Object value) {
		for(int i = 0; i < TABLE_LEN; i++) {
			for(Entry<V> e = m_table[i]; e != null; e = e.m_next) {
				if(value == e.m_value || (value != null && value.equals(e.m_value))) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized Set<Map.Entry<Integer, V>> entrySet() {
		return m_entrySet == null ? m_entrySet = new EntrySet() : m_entrySet;
	}

	public synchronized Set<Integer> keySet() {
		return m_keySet == null ? m_keySet = new KeySet() : m_keySet;
	}

	public synchronized Collection<V> values() {
		return m_values == null ? m_values = new Values() : m_values;
	}

	public synchronized boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		if(o instanceof Map) {
			Map mo = (Map)o;
			if(mo.size() != m_size) {
				return false;
			}
			for(Object e : mo.entrySet()) {
				Object ko = ((Map.Entry)e).getKey();
				Object vo = ((Map.Entry)e).getValue();
				if(ko == null) {
					return false;
				}
				V v = get(ko);
				if(vo == null) {
					if(v != null) {
						return false;
					}
				} else if(!vo.equals(v)) {
						return false;
				}
			}
			return true;
		}
		return false;
	}

 	public synchronized V get(Object key) {
 		if(key instanceof Number) {
	 		return get(((Number)key).intValue());
 		}
		return null;
 	}

 	public synchronized V get(int key) {
 		Entry<V> e = getEntry(key);
 		return e == null ? null : e.getValue();
 	}

	public synchronized boolean isEmpty() {
		return m_size == 0;
	}

 	public V put(Integer key, V value) {
 		if(key == null) {
 			throw new IllegalArgumentException("null key");
 		}
 		return put(key.intValue(), value);
 	}

 	public synchronized V put(int key, V value) {
 		final int i = getIndex(key);
 		final Entry<V> e = getEntry(i, key);
 		if(e == null) {
			m_table[i] = new Entry<V>(key, value, m_table[i]);
			m_size++;
			return null;
 		} else {
 			return e.setValue(value);
 		}
 	}

 	public synchronized void putAll(Map<? extends Integer,? extends V> m) {
 		for(Map.Entry<? extends Integer,? extends V> e : m.entrySet()) {
 			put(e.getKey().intValue(), e.getValue());
 		}
 	}

 	public synchronized V remove(final Object key) {
 		if(key instanceof Number) {
	 		return remove(((Number)key).intValue());
 		}
		throw new IllegalArgumentException();
 	}

	@SuppressWarnings("unchecked")
 	public synchronized V remove(final int key) {
 		final int i = getIndex(key);
 		final Entry[] tab = m_table;
 		Entry<V> e = tab[i];
 		Entry<V> prev = null;
 		while(e != null) {
 			if(e.m_key == key) {
 				if(prev == null) {
 					tab[i] = e.m_next;
 				} else {
 					prev.m_next = e.m_next;
 				}
 				m_size--;
 				return e.m_value;
 			}
 			prev = e;
 			e = e.m_next;
 		}
 		return null;
 	}

 	public synchronized int size() {
 		return m_size;
 	}


	/* utils */

	private int getIndex(final int key) {
		return (key ^ (key >> 8)) & 0xff;
	}

	private Entry<V> getEntry(final int key) {
		return getEntry(getIndex(key), key);
	}

	private Entry<V> getEntry(final int index, final int key) {
		Entry<V> e = m_table[index];
		while(e != null && e.m_key != key) {
			e = e.m_next;
		}
		return e;
	}

	private Entry<V> getEntry(final Object key) {
		if(key instanceof Number) {
			return getEntry(((Number)key).intValue());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private boolean removeEntry(final Map.Entry entry) {
		Object key = entry.getKey();
		if(key instanceof Number)  {
			final int k = ((Number)key).intValue();
			final int i = getIndex(k);
			final Entry[] tab = m_table;
			Entry<V> e = tab[i];
			Entry<V> prev = null;
			while(e != null) {
				if(e.equals(entry)) {
					if(prev == null) {
						tab[i] = e.m_next;
					} else {
						prev.m_next = e.m_next;
					}
					m_size--;
					return true;
				}
				prev = e;
				e = e.m_next;
			}
		}
		return false;
	}


	/* inner classes */

	static class Entry<V> implements Map.Entry<Integer, V> {
		final int	m_key;
		V			m_value;
		Entry<V>	m_next = null;

		public Entry(int key, V value) {
			m_key = key;
			m_value = value;
		}

		public Entry(int key, V value, Entry<V> next) {
			m_key = key;
			m_value = value;
			m_next = next;
		}

		public boolean equals(Object o) {
			if(o instanceof Map.Entry) {
	            final Map.Entry<Integer, V> e = (Map.Entry<Integer, V>)o;
	            final Object key = e.getKey();
	            if(key instanceof Number) {
		            final int ko = ((Number)key).intValue();
		            if(ko == m_key) {
		                final V v = m_value;
		                final Object vo = e.getValue();
		                if(vo == v || (vo != null && vo.equals(v))) {
		                    return true;
		                }
		            }
	            }
			}
            return false;
		}

		public Integer getKey() {
			return Integer.valueOf(m_key);
		}

		public int getkey() {
			return m_key;
		}

		public V getValue() {
			return m_value;
		}

		public V setValue(V value) {
			V oldVal = m_value;
			m_value = value;
			return oldVal;
		}

		public int hashCode() {
			return m_value == null ? m_key : m_key ^ m_value.hashCode();
		}
	}

	private class EntrySet extends AbstractSet<Map.Entry<Integer, V>> {
		public Iterator<Map.Entry<Integer, V>> iterator() {
			return new EntryIterator<Map.Entry<Integer, V>>() {
				public Map.Entry<Integer, V> next() {
					return nextEntry();
				}
			};
		}

        public boolean contains(Object o) {
        	if(o instanceof Map.Entry) {
	        	final Map.Entry eo = (Map.Entry)o;
				final Entry<V> e = getEntry(eo.getKey());
				if(e != null) {
					return e.equals(eo);
				}
        	}
       		return false;
        }
        public boolean remove(Object o) {
        	if(o instanceof Map.Entry) {
	        	return removeEntry((Map.Entry)o);
        	}
        	return false;
        }
        public int size() {
        	return ShortMap.this.size();
        }
	}

	private abstract class EntryIterator<E> implements Iterator<E> {
		Entry<V>	m_next;
		Entry<V>	m_current;
		int			m_index;

		@SuppressWarnings("unchecked")
		public EntryIterator() {
			final Entry[] t = m_table;
			Entry<V> n = null;
			int i = 0;
			if(m_size != 0) {
				while(i < TABLE_LEN && (n = t[i++]) == null) {}
			}
			m_next = n;
			m_index = i;
		}
		public boolean hasNext() {
			return m_next != null;
		}

		@SuppressWarnings("unchecked")
		public Entry<V> nextEntry() {
			if(m_next == null) {
				throw new NoSuchElementException();
			}
			final Entry[] tab = m_table;
			final Entry<V> e = m_next;
			Entry<V> n = e.m_next;
			int i = m_index;
			while(n == null && i < TABLE_LEN) {
				n = tab[i++];
			}
			m_next = n;
			m_index = i;
			m_current = e;
			return e;
		}

		public void remove() {
            if(m_current == null) {
                throw new IllegalStateException();
            }
            ShortMap.this.remove(m_current.m_key);
            m_current = null;
		}
	}

	private class KeyIterator extends EntryIterator<Integer> {
		public Integer next() {
			return nextEntry().getKey();
		}
	}

	private class ValueIterator extends EntryIterator<V> {
		public V next() {
			return nextEntry().getValue();
		}
	}

	private class KeySet extends AbstractSet<Integer> {
        public Iterator<Integer> iterator() {
        	synchronized(ShortMap.this) {
	            return new KeyIterator();
        	}
        }
        public int size() {
            return ShortMap.this.size();
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
        	synchronized(ShortMap.this) {
        		final int oldSize = m_size;
	            ShortMap.this.remove(o);
	            return m_size != oldSize;
        	}
        }
        public void clear() {
            ShortMap.this.clear();
        }
	}

	private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
        	synchronized(ShortMap.this) {
	            return new ValueIterator();
        	}
        }
        public int size() {
            return ShortMap.this.size();
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            ShortMap.this.clear();
        }
	}
}