package amie.typing.classifier;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import junit.framework.TestCase;

public class MinCutMaxClassifierTest extends TestCase {
    
    public void testLinkSortMap() throws Exception {
        Map<Integer, Double> m1 = new Int2DoubleOpenHashMap();
        int v1 = KB.map("1");
        int v2 = KB.map("2");
        int v3 = KB.map("3");
        m1.put(v2, 1.0);
        m1.put(v1, 2.0);
        m1.put(v3, 1.5);
        LinkedList<Map.Entry<Integer, Double>> result = MinCutMaxClassifier.linkSortMap(m1, Collections.reverseOrder());
        assertTrue(v1 == result.pop().getKey());
        assertTrue(v3 == result.pop().getKey());
        assertTrue(v2 == result.pop().getKey());
        assertTrue(result.isEmpty());
    }
    
    public void testClassify() throws Exception {
        
        MinCutMaxClassifier mcmc = new MinCutMaxClassifier(new KB()); 
        // Chain v1 <-> v2 <-> v3
        Int2ObjectMap<Int2DoubleMap> t1 = new Int2ObjectOpenHashMap<>();
        Int2DoubleMap m1 = new Int2DoubleOpenHashMap();
        Int2DoubleMap m2 = new Int2DoubleOpenHashMap();
        Int2DoubleMap m3 = new Int2DoubleOpenHashMap();
        int v1 = KB.map("1");
        int v2 = KB.map("2");
        int v3 = KB.map("3");
        m1.put(v2, 1.0);
        m2.put(v3, 2.0);
        m3.put(v2, 3.0);
        m2.put(v1, 4.0);
        t1.put(v1, m1);
        t1.put(v2, m2);
        t1.put(v3, m3);
        IntSet result = new IntOpenHashSet();
        
        // from v1
        result.add(v2);
        result.add(v3);
        assertEquals(result, mcmc.t_MinCutMax(t1, v1));
        result.clear();
        
        // from v2
        result.add(v3);
        assertEquals(result, mcmc.t_MinCutMax(t1, v2));
        result.clear();
        
        // from v3
        result.add(v1);
        result.add(v2);
        assertEquals(result, mcmc.t_MinCutMax(t1, v3));
        result.clear();
    }
}
