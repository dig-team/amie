package amie.data;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RecursiveMap<T> extends AbstractSet<T> {
	
	private Map<T, RecursiveMap<T>> map;
	@SuppressWarnings("unused")
	private RecursiveMap<T> parent;
	
	public RecursiveMap() {
		this.map = new HashMap<T, RecursiveMap<T>>();
		this.parent = null;
	}
	
	public RecursiveMap(RecursiveMap<T> parent) {
		this.map = new HashMap<T, RecursiveMap<T>>();
		this.parent = parent;
	}
	
	public void addAllSet(Set<T> s) {
		for(T e : s) {
			map.put(e, null);
		}
	}
	
	@Override
	public Iterator<T> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public int size() {
		return map.keySet().size();
	}
	
}
