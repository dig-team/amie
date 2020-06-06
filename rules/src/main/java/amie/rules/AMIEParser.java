package amie.rules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javatools.datatypes.Pair;
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
        Pair<List<int[]>, int[]> rulePair = KB.rule(s.trim());
        if (rulePair == null) return null;
        Rule resultRule = new Rule(rulePair.second, rulePair.first, 0);
        resultRule.setGeneration(resultRule.getLength());
        return resultRule;
    }

    public static Rule rule(List<String> record) {
        Rule rule = rule(record.get(0));
        if (rule == null)
            return null;

        if (record.size() > 1) {
            rule.setHeadCoverage(Double.parseDouble(record.get(1)));
            rule.setSupport(Double.parseDouble(record.get(4)));
            rule.setBodySize(Long.parseLong(record.get(5)));
            rule.setPcaBodySize(Double.parseDouble(record.get(6)));

            int functionalVariablePosition = KB.varpos(KB.map(record.get(7)), rule.getHead());
            rule.setFunctionalVariablePosition(functionalVariablePosition);
        }

        return rule;
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
        TSVFile tsvFile = new TSVFile(f);
        List<Rule> result = new ArrayList<>();
        for (List<String> record : tsvFile) {
            Rule q = AMIEParser.rule(record);

            if (q == null)
                continue;

            result.add(q);
        }
        return (result);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(AMIEParser.rule("=> ?a <hasChild> ?b"));
    }
}
