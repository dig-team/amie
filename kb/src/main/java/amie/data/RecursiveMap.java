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


	@Override
	public Iterator<T> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public int size() {
		return map.keySet().size();
	}

}
