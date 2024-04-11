package amie.data.javatools.datatypes;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import amie.data.javatools.administrative.D;

/**
 * 
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * This class implements a HashMap with integer values.
 * 
 * @author Fabian M. Suchanek
 * 
 * @param <K>
 */
public class IntHashMap<K> extends AbstractSet<K> {

	/** Holds the keys */
	protected Object[] keys;

	/** Holds the values */
	protected int[] values;

	/** Holds size */
	protected int size = 0;
	
	/** Number of deleted items **/
	protected int delSize = 0;

	/** Constructor */
	public IntHashMap() {
		clear();
	}


	/** Returns an index where to store the object */
	protected int index(Object key, int len) {
		return (Math.abs(key.hashCode()) % len);
	}

	/** Returns an index where to store the object */
	protected int index(Object key) {
		return (index(key, keys.length));
	}

	/** Retrieves a value */
	public int get(Object key) {
		return (get(key, -1));
	}

	/** Finds a key, keys[find] will be NULL if non-existent */
	protected int find(Object key) {
		int i = index(key);
		while (true) {
			if (keys[i] == null)
				return (i);
			if (keys[i].equals(key))
				return (i);
			i++;
			if (i == keys.length)
				i = 0;
		}
	}

	/** Retrieves a value */
	public int get(Object key, int defaultValue) {
		int pos = find(key);
		if (keys[pos] == null)
			return (defaultValue);
		else
			return (values[pos]);
	}

	/** True if value is there */
	public boolean containsKey(Object key) {
		return (keys[find(key)] != null && keys[find(key)] != DEL);
	}

	/**
	 * Increases a value, true for 'added new key with delta as value', false
	 * for 'increased existing value'
	 */
	public boolean add(K key, int delta) {
		int pos = find(key);
		if (keys[pos] == null || keys[pos] == DEL) {
			keys[pos] = key;
			values[pos] = delta;
			size++;
			if (size + delSize > keys.length * 3 / 4)
				rehash();
			return (true);
		}
		values[pos] += delta;
		return (false);
	}

	/**
	 * Increases a value, true for 'added new key with value 1', false for
	 * 'increased existing value'
	 */
	public boolean increase(K key) {
		return (add(key, 1));
	}

	/** Returns keys. Can be used only once. */
	public PeekIterator<K> keys() {
		final Object[] e = keys;
		return (new PeekIterator<K>() {

			int pos = -1;

			@SuppressWarnings("unchecked")
			@Override
			protected K internalNext() throws Exception {
				pos++;
				for (; pos < keys.length; pos++) {
					if (e[pos] != null && e[pos] != DEL) {
						return ((K) e[pos]);
					}
				}
				return (null);
			}

		});
	}

	/**
	 * Adds a key, true for 'added the key as new', false for 'overwrote
	 * existing value'
	 */
	public boolean put(K key, int value) {
		if (put(keys, values, key, value)) {
			size++;
			if (size + delSize > keys.length * 3 / 4)
				rehash();
			return (true);
		}
		return (false);
	}

	/**
	 * Adds a key, true for 'added the key as new', false for 'overwrote
	 * existing value'
	 */
	protected boolean put(Object[] keys, int[] values, Object key, int value) {
		int i = index(key, keys.length);
		while (true) {
			if (keys[i] == null || keys[i]==DEL) {
				keys[i] = key;
				values[i] = value;
				return (true);
			}
			if (keys[i].equals(key)) {
				values[i] = value;
				return (false);
			}
			i++;
			if (i == keys.length)
				i = 0;
		}
	}

	protected Object DEL=new Object();
	
	@Override
	public boolean remove(Object arg0) {
		int pos=find(arg0);
		return remove(pos);
	}
	
	private boolean remove(int pos) {
		if(keys[pos]==null) return(false);
		keys[pos]=DEL;
		values[pos]=0;
		size--;
		++delSize;
		if (size + delSize > keys.length * 3 / 4) {
			rehash();	
		}
		return(true);
	}
	
	/** Rehashes */
	protected void rehash() {
		Object[] newKeys = new Object[size * 2];
		int[] newValues = new int[size * 2];
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null && keys[i]!=DEL)
				put(newKeys, newValues, keys[i], values[i]);
		}
		keys = newKeys;
		values = newValues;
		delSize = 0;
	}

	/** Test */
	public static void main(String[] args) throws Exception {
		IntHashMap<String> m = new IntHashMap<String>();
		for (int i = 1; i < 3000; i *= 2)
			m.put("#" + i, i);
		m.put("#0", 17);		
		m.remove("#300000");
		m.remove("#32");
		for (String key : m.keys())
			D.p(key, m.get(key));
	}

	@Override
	public Iterator<K> iterator() {
		return keys().iterator();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean add(K e) {
		return (increase(e));
	};

	@Override
	public void clear() {
		size = 0;
		keys = new Object[10];
		values = new int[10];
	}

	@Override
	public boolean contains(Object o) {
		return containsKey(o);
	}

	@Override
	public String toString() {
		if (isEmpty())
			return ("{}");
		StringBuilder b = new StringBuilder("{");
		int counter = 30;
		for (K key : keys()) {
			if (counter-- == 0) {
				b.append("..., ");
				break;
			}
			b.append(key).append('=').append(get(key)).append(", ");
		}
		b.setLength(b.length() - 2);
		return (b.append("}").toString());
	}

	/** returns the keys in increasing order*/
	public List<K> increasingKeys() {
		List<K> result = keys().asList();
		Collections.sort(result, new Comparator<K>() {
			@Override
			public int compare(K o1, K o2) {
				int i1 = get(o1);
				int i2 = get(o2);
				return (i1 < i2 ? -1 : i1 > i2 ? 1 : 0);
			}
		});
		return (result);
	}

	/** returns the keys in decreasing order */
	public List<K> decreasingKeys() {
		List<K> result = keys().asList();
		Collections.sort(result, new Comparator<K>() {

			@Override
			public int compare(K o1, K o2) {
				int i1 = get(o1);
				int i2 = get(o2);
				return (i1 < i2 ? 1 : i1 > i2 ? -1 : 0);
			}
		});
		return (result);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IntHashMap<?>))
			return (false);
		IntHashMap<?> other = (IntHashMap<?>) o;
		if (other.size() != this.size())
			return (false);
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null && values[i] != other.get(keys[i]))
				return (false);
		}
		return (true);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

	/** Computes the sum*/
	public long computeSum() {
		long sum = 0;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null)
				sum += values[i];
		}
		return (sum);
	}

}
