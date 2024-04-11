/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.utils;

import amie.data.KB;
import amie.mining.AMIE;
import amie.mining.assistant.MiningAssistant;
import amie.rules.AMIEParser;
import amie.rules.Rule;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import amie.data.javatools.datatypes.MultiMap;
import amie.data.javatools.filehandlers.TSVFile;

/**
 *
 * @author jlajus
 */
public class TSVRuleIsSkyline {
    
    
    public static void main(String args[]) throws IOException {
        if (args.length < 1) {
            System.out.println("TSVRuleIsSkyline <file1>");
            System.exit(1);
        }
        
        KB kb = new KB();
        AMIE amie = AMIE.getVanillaSettingInstance(kb);
        MiningAssistant assistant = amie.getAssistant();

        File file1 = new File(args[0]);
        TSVFile tsv1 = new TSVFile(file1);
        HashSet<Rule> rules1 = new HashSet<>();
        MultiMap<Integer, Rule> indexedOutputSet = new MultiMap<>();
        //Map<Query, List<String>> recordsMap = new LinkedHashMap<Query, List<String>>();

        //Preprocess one of the files
        for (List<String> record1 : tsv1) {
            if (record1.size() < 7) {
                continue;
            }
            Rule q = AMIEParser.rule(record1.get(0));
            if (q == null) continue;
            q.setSupport(Integer.parseInt(record1.get(4)));
            q.setBodySize(Integer.parseInt(record1.get(5)));
            q.setPcaBodySize(Integer.parseInt(record1.get(6)));
            q.setGeneration(q.getLength());
            if (rules1.contains(q)) {
                System.err.println("[DUP] Duplicate rule in file:");
                System.err.println(q.getRuleString());
            } else {
                rules1.add(q);
                indexedOutputSet.put(q.alternativeParentHashCode(), q);
            }
        }
        
        int validrules = 0;
        for (Rule q : rules1) {
            assistant.setAdditionalParents(q, indexedOutputSet);
            if (!assistant.testConfidenceThresholds(q)) {
                System.err.println("[ERROR] TestConfidenceThresholds FAILED for rule:");
                System.err.println(q.getRuleString());
            } else {
                validrules += 1;
            }
        }
        System.out.println("Valid rules remaining: " + validrules);
    }
}
