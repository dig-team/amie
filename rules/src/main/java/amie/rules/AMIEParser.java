package amie.rules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import amie.data.KB;


import javafx.util.Pair;
import amie.data.javatools.filehandlers.TSVFile;

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
        Rule resultRule = new Rule(rulePair.getValue(), rulePair.getKey(), 0);
        return resultRule;
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
