package amie.typing.classifier;

import amie.data.KB;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javatools.datatypes.ByteString;
import junit.framework.TestCase;

public class MinCutMaxClassifierTest extends TestCase {
    
    public void testLinkSortMap() throws Exception {
        Map<ByteString, Double> m1 = new HashMap<>();
        ByteString v1 = ByteString.of("1");
        ByteString v2 = ByteString.of("2");
        ByteString v3 = ByteString.of("3");
        m1.put(v2, 1.0);
        m1.put(v1, 2.0);
        m1.put(v3, 1.5);
        LinkedList<Map.Entry<ByteString, Double>> result = MinCutMaxClassifier.linkSortMap(m1, Collections.reverseOrder());
        assertEquals(v1, result.pop().getKey());
        assertEquals(v3, result.pop().getKey());
        assertEquals(v2, result.pop().getKey());
        assertTrue(result.isEmpty());
    }
    
    public void testClassify() throws Exception {
        
        MinCutMaxClassifier mcmc = new MinCutMaxClassifier(new KB()); 
        // Chain v1 <-> v2 <-> v3
        Map<ByteString, Map<ByteString, Double>> t1 = new HashMap<>();
        Map<ByteString, Double> m1 = new HashMap<>();
        Map<ByteString, Double> m2 = new HashMap<>();
        Map<ByteString, Double> m3 = new HashMap<>();
        ByteString v1 = ByteString.of("1");
        ByteString v2 = ByteString.of("2");
        ByteString v3 = ByteString.of("3");
        m1.put(v2, 1.0);
        m2.put(v3, 2.0);
        m3.put(v2, 3.0);
        m2.put(v1, 4.0);
        t1.put(v1, m1);
        t1.put(v2, m2);
        t1.put(v3, m3);
        Set<ByteString> result = new HashSet<>();
        
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
