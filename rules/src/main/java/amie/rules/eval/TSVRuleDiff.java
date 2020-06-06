/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.rules.eval;

import amie.rules.AMIEParser;
import amie.rules.Rule;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javatools.filehandlers.TSVFile;

/**
 *
 * @author jlajus
 */
public class TSVRuleDiff {

    public static void diff(Set<Rule> rules1, Set<Rule> rules2) {
        Set<Rule> rules2copy = new HashSet<>(rules2);
        for (Rule r : rules1) {
            if (!rules2copy.contains(r)) {
                System.err.println("< " + r.getRuleString());
            } else {
                rules2copy.remove(r);
            }
        }

        for (Rule r : rules2copy) {
            System.err.println("> " + r.getRuleString());
        }
    }

    public static void main(String args[]) throws IOException {
        if (args.length < 2) {
            System.out.println("TSVRuleDiff <file1> <file2>");
            System.exit(1);
        }

        File file1 = new File(args[0]);
        File file2 = new File(args[1]);
        TSVFile tsv1 = new TSVFile(file1);
        HashMap<Rule, Rule> rules1 = new HashMap<>();
        HashMap<Rule, Rule> rules2 = new HashMap<>();
        TSVFile tsv2 = new TSVFile(file2);
        //Map<Query, List<String>> recordsMap = new LinkedHashMap<Query, List<String>>();
        Map<String, List<String>> recordsMap = new LinkedHashMap<String, List<String>>();

        //Preprocess one of the files
        for (List<String> record1 : tsv1) {
            Rule q = AMIEParser.rule(record1);
            if (q == null) continue;
            if (rules1.containsKey(q)) {
                System.err.println("[DUP] in first file:");
                System.err.println(q.getRuleString());
                System.err.println(rules1.get(q).getRuleString());
            } else {
                rules1.put(q, q);
            }
        }

        for (List<String> record2 : tsv2) {
            Rule q = AMIEParser.rule(record2);
            if (q == null) continue;
            if (rules2.containsKey(q)) {
                System.err.println("[DUP] in second file:");
                System.err.println(q.getRuleString());
                System.err.println(rules2.get(q).getRuleString());
            } else {
                rules2.put(q, q);
            }
        }

        diff(rules1.keySet(), rules2.keySet());
    }
}
