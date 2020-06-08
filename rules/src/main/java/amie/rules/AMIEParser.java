package amie.rules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;


import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.TSVFile;

/**
 * Parses a file of AMIE rules
 *
 * @author Fabian
 */
public class AMIEParser {

    /**
     * Parsers an AMIE rule from a string.
     *
     * @param s
     * @return
     */
    public static Rule rule(String s) {
        Pair<List<int[]>, int[]> rulePair = KB.rule(s);
        if (rulePair == null) return null;
        Rule resultRule = new Rule(rulePair.second, rulePair.first, 0);
        return resultRule;
    }

    public static void normalizeRule(Rule q) {
        char c = 'a';
        Int2ObjectMap<Character> charmap = new Int2ObjectOpenHashMap<Character>();
        for (int[] triple : q.getTriples()) {
            for (int i = 0; i < triple.length; ++i) {
                if (KB.isVariable(triple[i])) {
                    Character replace = charmap.get(triple[i]);
                    if (replace == null) {
                        replace = new Character(c);
                        charmap.put(triple[i], replace);
                        c = (char) (c + 1);
                    }
                    triple[i] = KB.map("?" + replace);
                }
            }
        }
    }

    public static List<Rule> rules(File f) throws IOException {
        List<Rule> result = new ArrayList<>();
        for (List<String> record : new TSVFile(f)) {
            Rule query = rule(record.get(0));

            if (query != null)
                result.add(query);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(AMIEParser.rule("=> ?a <hasChild> ?b"));
    }
}
