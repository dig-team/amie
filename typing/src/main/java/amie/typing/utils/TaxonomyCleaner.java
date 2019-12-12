/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.utils;

import amie.data.KB;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javatools.filehandlers.FileLines;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 *
 * @author jlajus
 */
public class TaxonomyCleaner {

    public static void main(String[] args) throws IOException {
        Class c = DefaultEdge.class;
        DirectedGraph<Integer, DefaultEdge> g 
                = new SimpleDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
        for (String line : new FileLines(new File(args[0]), "UTF-8", null)) {
            String[] split = line.split("/t");
            if (split.length == 4) {
                int s = KB.map(split[1]);
                int o = KB.map(split[3]);
                if (s == (o)) {
                    System.err.println(KB.unmap(s));
                } else {
                    g.addVertex(s);
                    g.addVertex(o);
                    g.addEdge(s, o);
                }
            }
        }
        StrongConnectivityAlgorithm<Integer, DefaultEdge> gi = new KosarajuStrongConnectivityInspector<>(g);
        List<Set<Integer>> scc = gi.stronglyConnectedSets();
        for (Set<Integer> scs : scc) {
            if (scs.size() > 1) {
                for (int v : scs) {
                    System.out.print(KB.unmap(v) + " ");
                }
                System.out.println();
            }
        }
    }
}
