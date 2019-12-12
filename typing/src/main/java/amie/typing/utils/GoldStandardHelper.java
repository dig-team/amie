/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.utils;

import amie.data.KB;
import amie.data.Schema;
import amie.typing.heuristics.TypingHeuristic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javatools.datatypes.Pair;

/**
 * 
 * @author jlajus
 */
public class GoldStandardHelper {
    
    protected double getStandardConfidenceWithThreshold(List<int[]> head, List<int[]> body, int variable, int threshold, boolean unsafe) {
        List<int[]> bodyC = (unsafe) ? new LinkedList<>(body) : body;
        long bodySize = db.countDistinct(variable, bodyC);
        bodyC.addAll(head);
        long support = db.countDistinct(variable, bodyC);
        if (support < threshold || bodySize == 0) {
            return 0;
        }
        return ((double) support) / bodySize;
    }
    
    public static final int supportThreshold = 50;
    
    private String query;
    private Set<GSnode> marked;
        
    private GSnode current;
    public List<GSnode> ids;
    private Int2ObjectMap<GSnode> index;
    
    public GoldStandardHelper(KB source) {
        this.marked = new HashSet<>();
        this.index = new Int2ObjectOpenHashMap<>();
        db = source;
        //handler = this;
    }
    
    public GoldStandardHelper(String query) {
        this.marked = new HashSet<>();
        this.index = new Int2ObjectOpenHashMap<>();
        this.query = query;
        current = new GSnode(Schema.topBS);
        index.put(Schema.topBS, current);
        Pair<List<int[]>, Integer> queryPair = queryStrToQuery(query);
        current.generate(supportThreshold, queryPair.first, queryPair.second);
    }
    
    private Pair<List<int[]>, Integer> queryStrToQuery(String query) {
        List<int[]> queryL = KB.triples(KB.triple(KB.map("?x"), KB.map(query.substring(0, query.length()-1)), KB.map("?y")));
        int variable = KB.map("?"+query.substring(query.length()-1));
        return new Pair<>(queryL, variable);
    }
    
    public static GoldStandardHelper handler = null;
    private static KB db;

    public class GSnode implements Comparable {
        List<GSnode> parents;
        List<GSnode> children;
        int className;
        
        public GSnode(int className) { 
            this.className = className; 
            this.children = new LinkedList<>();
            this.parents = new LinkedList<>();
        }
        
        public void generate(int supportThreshold, List<int[]> query, int variable) {
            for (int subClass : Schema.getSubTypes(db, className)) {
                GSnode stc = index.get(subClass);
                if (stc != null) {
                    children.add(stc);
                    stc.parents.add(this);
                } else if (getStandardConfidenceWithThreshold(TypingHeuristic.typeL(subClass, variable), query, variable, supportThreshold, true) != 0) {
                    stc = new GSnode(subClass);
                    index.put(subClass, stc);
                    children.add(stc);
                    stc.parents.add(this);
                    stc.generate(supportThreshold, query, variable);
                }
            }
        }

        @Override
        public int compareTo(Object t) {
            return Integer.compare(className, ((GSnode) t).className);
        }
    }
    
    public static void help() {
        System.out.println();
        System.out.println(String.join("\n",
        "identifiers:",
        "    [0-9]",
        "    `[0-9]+`",
        "special:",
        "    c: current",
        "    a: all",
        "directions:",
        "    p: parents",
        "    c: children",
        "    s: siblings",
        "commands:",
        "    h: show help",
        "    m: move [identifier]",
        "    l: list [a|direction|l]",
        "    c: print current",
        "    x: mark [c|identifier|range|a]",
        "    u: unmark [c|identifier|range|a]",
        "    s: search `.+`",
        "    f: finish",
        "    q: query `<.*>(x|y)`",
        "    qq: quit"));
    }
    
    public static void newQuery(String query) {
        if (query.equals("q")) { quit(); }
        else {
            try {
                int id = Integer.parseInt(query.substring(0, query.length()-1));
                query = KB.unmap(handler.ids.get(id).className) + query.substring(query.length() - 1);
            } catch (Exception e) {}
            System.out.print(" Loading query '"+query+"'... ");
            handler = new GoldStandardHelper(query);
            System.out.println("Done.");
        }
    }
    
    private static String getString() throws IOException {
        int input = System.in.read();
        if (input == -1) { quit(); }
        else {
            if ((char) input == '`') {
                String result = "";
                input = System.in.read();
                if (input == -1) { quit(); }
                while((char) input != '`') {
                    result += Character.toString((char) input);
                    input = System.in.read();
                    if (input == -1) { quit(); break; }
                }
                return result;
            } else {
                return Character.toString((char) input);
            }
        }
        return null;
    }
    
    public static void parse() throws IOException {
        String arg;
        while(handler != null) {
            int input = System.in.read();
            if (input == -1) { quit(); }
            else {
                switch((char) input) {
                    case 'h': help(); break;
                    case 'm': 
                        arg = getString();
                        if (arg != null && handler != null) handler.move(arg);
                        break;
                    case 'l':
                        arg = getString();
                        if (arg != null && handler != null) handler.list(arg);
                        break;
                    case 'c': handler.current(); break;
                    case 'x':
                        arg = getString();
                        if (arg != null && handler != null) handler.mark(arg);
                        break;
                    case 'u':
                        arg = getString();
                        if (arg != null && handler != null) handler.unmark(arg);
                        break;
                    case 's':
                        arg = getString();
                        if (arg != null && handler != null) handler.search(arg);
                        break;
                    case 'f': handler.finish(); break;
                    case 'q': 
                        arg = getString();
                        if (arg != null && handler != null) newQuery(arg);
                        break;
                    default:
                        System.out.println(" Invalid command: "+Character.toString((char) input));
                        System.out.println("Press h for help.");
                }
            }
        }
    }
    
    public static void quit() {
        System.out.println(" Quitting...");
        handler = null;
    }
    
    private String _markTag(GSnode n) {
        if (marked.contains(n)) return "\tX";
        for (GSnode m : marked) {
            if (Schema.isTransitiveSuperType(db, m.className, n.className)) return "\tx\t"+KB.unmap(m.className);
        }
        //if (_isMarked(n)) return "\tx";
        return "";
    }
    
    private boolean _isMarked(GSnode n) {
        for (GSnode m : marked) {
            if (n == m || Schema.isTransitiveSuperType(db, m.className, n.className)) return true;
        }
        return false;
    }
    
    public void current() {
        System.out.println();
        System.out.println(KB.unmap(current.className) + _markTag(current));
    }
    
    public void move(String id) {
        try {
            current = ids.get(Integer.parseInt(id));
            System.out.println();
        } catch (Exception e) {
            System.out.println(" Invalid id: "+id);
        }
    }
    
    public void mark(String id) {
        if (id.equals("c")) {
            marked.add(current);
            System.out.println();
        } else if (id.equals("a")) {
            marked.addAll(ids);
            System.out.println();
        } else if (id.contains("-")) {
            try {
                String[] rangeStr = id.split("-");
                marked.addAll(ids.subList(Integer.parseInt(rangeStr[0]), Integer.parseInt(rangeStr[1])+1));
                System.out.println();
            } catch (Exception e) {
                System.out.println(" Invalid range: "+id);
            }
        } else {
            try {
                marked.add(ids.get(Integer.parseInt(id)));
                System.out.println();
            } catch (Exception e) {
                System.out.println(" Invalid id: "+id);
            }
        }
    }
    
    public void unmark(String id) {
        if (id.equals("c")) {
            marked.remove(current);
            System.out.println();
        } else if (id.equals("a")) {
            marked.removeAll(ids);
            System.out.println();
        } else if (id.contains("-")) {
            try {
                String[] rangeStr = id.split("-");
                marked.removeAll(ids.subList(Integer.parseInt(rangeStr[0]), Integer.parseInt(rangeStr[1])+1));
                System.out.println();
            } catch (Exception e) {
                System.out.println(" Invalid range: "+id);
            }
        } else {
            try {
                marked.remove(ids.get(Integer.parseInt(id)));
                System.out.println();
            } catch (Exception e) {
                System.out.println(" Invalid id: "+id);
            }
        }
    }
    
    public void list(String dir) {
        switch(dir) {
            case "l": break;
            case "a":
                ids = new LinkedList<>(index.values());
                break;
            case "p":
                ids = current.parents;
                break;
            case "c":
                ids = current.children;
                break;
            case "s":
                ids = new LinkedList<>();
                for (GSnode p: current.parents) {
                    for (GSnode c: p.children) {
                        if (c != current && !ids.contains(c)) ids.add(c);
                    }
                }
                break;
            case "q":
                listQueries();
                break;
            default:
                System.out.println(" Invalid list direction: "+dir);
                return;
        }
        _printIds();
    }
    
    public void search(String s) {
        ids = new LinkedList<>();
        for (int c: index.keySet()) {
            if(KB.unmap(c).contains(s)) ids.add(index.get(c));
        }
        _printIds();
    }
    
    private void _printIds() {
        Collections.sort(ids);
        System.out.println();
        int i = 0;
        for (GSnode n : ids) {
            System.out.println("["+Integer.toString(i)+"] "+KB.unmap(n.className)+_markTag(n));
            ++i;
        }
    }
    
    public void finish() throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter("gs_" + query.replaceAll("<", "").replaceAll(">", "_")));
        Set<GSnode> cleanedResults = new LinkedHashSet<>(marked);
        for (GSnode c1 : marked) {
            for (GSnode c2 : marked) {
                if (c1 == c2) {
                    continue;
                }
                if (Schema.isTransitiveSuperType(db, c2.className, c1.className)) {
                    cleanedResults.remove(c1);
                    break;
                }
            }
        }
        for (GSnode c : cleanedResults) {
            output.write(KB.unmap(c.className)+"\n");
        }
        output.close();
        System.out.println(" GS of '"+query+"' written to file");
    }
    
    public void listQueries() {
        List<int[]> query = new ArrayList<>(1);
        query.add(KB.triple(KB.map("?x"), KB.map("?y"), KB.map("?z")));
        IntSet relations = db.selectDistinct(KB.map("?y"), query);
        relations.remove(Schema.typeRelationBS);
        relations.remove(Schema.subClassRelationBS);
        
        ids = new LinkedList<>();
        System.out.println(" Append x or y to:");
        for (int r : relations) {
            //System.out.println(r.toString());
            ids.add(new GSnode(r));
        }
    }
    
    public static void main(String[] args) throws IOException {
        List<File> dataSource = new ArrayList<>(args.length);
        for(int i = 0; i < args.length; i++) {
            dataSource.add(i, new File(args[i]));
        }
        KB kb = new KB();
        kb.load(dataSource);
        GoldStandardHelper root = new GoldStandardHelper(kb);
        GoldStandardHelper.handler = root;
        while(true) {
            try {
                GoldStandardHelper.parse();
                break;
            } catch (NullPointerException e) {
                System.out.println();
                System.err.println("ERROR: NullPointerException catch in main loop");
                e.printStackTrace();
                System.err.println("Resuming from empty query !");
                GoldStandardHelper.handler = root;
            } catch (Exception e) {
                System.out.println();
                System.err.println("ERROR: Unexpected exception "+e.toString());
                e.printStackTrace();
                System.err.println("Resuming from empty query !");
                GoldStandardHelper.handler = root;
            }
        }
    }
}
