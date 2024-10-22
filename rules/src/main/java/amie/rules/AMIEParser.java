package amie.rules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import amie.data.AbstractKB;
import amie.data.KB;

import amie.data.Schema;
import amie.data.javatools.datatypes.Pair;
import amie.data.javatools.filehandlers.TSVFile;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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
    public static Rule rule(String s, AbstractKB kb) {
        Pair<List<int[]>, int[]> rulePair = kb.rule(s);
        if (rulePair == null)
            return null;
        Rule resultRule = new Rule(rulePair.second, rulePair.first, 0, kb);
        return resultRule;
    }

    public static void normalizeRule(Rule q, KB kb) {
        char c = 'a';
        Int2ObjectMap<Character> charmap = new Int2ObjectOpenHashMap<Character>();
        for (int[] triple : q.getTriples()) {
            for (int i = 0; i < triple.length; ++i) {
                if (Schema.isVariable(triple[i])) {
                    Character replace = charmap.get(triple[i]);
                    if (replace == null) {
                        replace = new Character(c);
                        charmap.put(triple[i], replace);
                        c = (char) (c + 1);
                    }
                    triple[i] = kb.map("?" + replace);
                }
            }
        }
    }

    public static List<Rule> parseRules(File f, AbstractKB kb) throws IOException {
        List<Rule> result = new ArrayList<>();
        try (TSVFile fileObj = new TSVFile(f)) {
            fileObj.next(); // Ignore the header
            for (List<String> record : fileObj) {
                Rule rule = rule(record.get(0), kb);
                if (rule != null)
                    result.add(rule);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        KB kb = new KB(new Schema());
        System.out.println(AMIEParser.rule("=> ?a <hasChild> ?b", kb));
    }
}
