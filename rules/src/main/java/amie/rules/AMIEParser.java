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

    private static List<String> ANYBURL_VARS = List.of("A", "B", "X", "Y", "C", "D");
    private static final String uriPattern = "<?[-&!._\\w\\p{L}:/–'\\(\\),]+>?";
    public static final Pattern triplePattern = Pattern
            .compile(String.format("(\\w+)\\((\\??\\w+|%s)\\s*,\\s*(\\??\\w+|%s)\\)", uriPattern, uriPattern));


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
                        replace = Character.valueOf(c);
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
            List<String> header = fileObj.next(); // Ignore the header
            int idxSupport = header.indexOf("Support");
            int idxPcaBodySize = header.indexOf("Pca Body Size");
            int idxBodySize = header.indexOf("Body Size");
            for (List<String> record : fileObj) {
                Rule rule = rule(record.get(0), kb);
                if (rule != null) {
                    if (idxSupport != -1)
                        rule.setSupport(Double.parseDouble(record.get(idxSupport)));
                    if (idxBodySize != -1)
                        rule.setBodySize(Long.parseLong(record.get(idxBodySize)));
                    if (idxPcaBodySize != -1)
                        rule.setPcaBodySize(Long.parseLong(record.get(idxPcaBodySize)));
                    result.add(rule);
                }
            }
        }
        return result;
    }

    public static List<Rule> parseAnyBurlRules(File f, AbstractKB kb) throws IOException {
        List<Rule> result = new ArrayList<>();
        try (TSVFile fileObj = new TSVFile(f)) {
            for (List<String> record : fileObj) {
                Rule rule = anyBurlRule(record.get(3), kb);
                if (rule != null) {
                    rule.setSupport(Double.parseDouble(record.get(1)));
                    rule.setBodySize(Long.parseLong(record.get(0)));
                    result.add(rule);
                } else {
                    System.err.println(record.get(3) + " could not be parsed as rule");
                }
            }
        }
        return result;
    }

    private static int[] anyBurlTriple(String s, String r, String o, AbstractKB kb) {
        int sm;
        if (ANYBURL_VARS.contains(s)){
            sm = kb.map("?" + s.toLowerCase());
        } else {
            sm = kb.map(s);
        }

        int rm = kb.map(r);
        int om;
        if (ANYBURL_VARS.contains(o)){
            om = kb.map("?" + o.toLowerCase());
        } else {
            om = kb.map(o);
        }
        return new int[]{sm, rm, om};
    }

    private static Rule anyBurlRule(String s, AbstractKB kb) {
        String[] ruleParts = s.split(" <= ");
        if (ruleParts.length == 0) {
            return null;
        }
        int[] head = null;
        Matcher headMatcher = triplePattern.matcher(ruleParts[0]);
        if (headMatcher.find())
            head = anyBurlTriple(headMatcher.group(2).trim(), headMatcher.group(1).trim(), headMatcher.group(3).trim(), kb);
        else
            return null;

        List<int[]> body = new ArrayList<>();
        if (ruleParts.length > 1) {
            Matcher bodyMatcher = triplePattern.matcher(ruleParts[1]);
            if (bodyMatcher.find())
                body.add(anyBurlTriple(bodyMatcher.group(2).trim(), bodyMatcher.group(1).trim(), bodyMatcher.group(3).trim(), kb));
        }

        return new Rule(head, body, 0, kb);
    }

    public static void main(String[] args) throws IOException {
        KB kb = new KB();
        kb.load(new File("/home/lgalarra/Documents/git/mm-kge/data/wn18rr/train.tsv"));
        List<Rule> rules = AMIEParser.parseAnyBurlRules(new File("/home/lgalarra/Documents/git/amie/rules-10"), kb);
        //System.out.println(rules);
        System.out.println(rules.size() + " rules parsed");
    }
}
