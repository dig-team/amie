package amie.rules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                rule.setSupport(Double.parseDouble(record.get(4)));
                rule.setBodySize(Long.parseLong(record.get(5)));
                rule.setPcaBodySize(Long.parseLong(record.get(6)));
                if (rule != null)
                    result.add(rule);
            }
        }
        return result;
    }

    public static List<Rule> parseAnyBurlFormattedRules(File f, AbstractKB kb) throws IOException {
        List<Rule> result = new ArrayList<>();
        try (TSVFile fileObj = new TSVFile(f)) {
            for (List<String> record : fileObj) {
                Rule rule = anyburlRule(record.get(3), kb);
                if (rule != null) {
                    rule.setSupport(Double.parseDouble(record.get(1)));
                    rule.setBodySize(Long.parseLong(record.get(0)));
                    rule.setPcaBodySize(Long.parseLong(record.get(0)));
                    result.add(rule);
                }
            }
        }
        return result;
    }

    private static Rule anyburlRule(String s, AbstractKB kb) {
        String[] rulePair = s.split(" <= ");
        if (rulePair.length != 2)
            return null;
        String triplePatternRegex = "([-\\w<>_.:\\&\\/]+)\\(([-\\w<>_.:\\&\\/]+)\\s*,\\s*([-\\w<>_.:\\&\\/]+)\\)";
        Matcher headAtomMatcher = Pattern.compile(triplePatternRegex).matcher(rulePair[0]);
        if (!headAtomMatcher.find())
            return null;
        String predicate = headAtomMatcher.group(1);
        String subject = headAtomMatcher.group(2);
        String object = headAtomMatcher.group(3);

        int headAtom[] = new int[]{kb.map(subject), kb.map(predicate), kb.map(object)};
        Matcher bodyAtomMatcher = Pattern.compile(triplePatternRegex).matcher(rulePair[1]);

        List<int[]> bodyAtoms = new ArrayList<>();
        while(bodyAtomMatcher.find()) {
            predicate = bodyAtomMatcher.group(1);
            subject = bodyAtomMatcher.group(2);
            object = bodyAtomMatcher.group(3);
            if (predicate == null || subject == null || object == null)
                return null;

            bodyAtoms.add(new int[]{kb.map(subject), kb.map(predicate), kb.map(object)});
        }

        Rule resultRule = new Rule(headAtom, bodyAtoms, 0, kb);
        return resultRule;
    }

    public static void main(String[] args) {
        KB kb = new KB(new Schema());
        System.out.println(AMIEParser.rule("=> ?a <hasChild> ?b", kb));
    }
}
