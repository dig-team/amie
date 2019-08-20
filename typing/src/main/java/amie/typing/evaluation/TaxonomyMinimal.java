/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.evaluation;

import amie.data.KB;
import amie.data.Schema;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javatools.filehandlers.FileLines;

/**
 * Ensure a result set of classes is minimal wrt the taxonomy.
 * Test 1: doesn't contain duplicates.
 * Test 2: no class is a subclass of another class.
 * Write minimal result to a new file otherwise.
 * @author jlajus
 */
public class TaxonomyMinimal {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: TaxonomyMinimal <taxonomyFile> <resultFile1> ...");
            System.exit(1);
        }
        KB taxo = new KB();
        taxo.setDelimiter(" ");
        Schema.subClassRelation = "<P279>";
                Schema.subClassRelationBS = KB.map(Schema.subClassRelation);
        taxo.load(new File(args[0]));
        
        for (int i = 1; i < args.length; i++) {
            IntSet results = new IntOpenHashSet();
            boolean clean = false;

            File resultFile = new File(args[i]);
            for (String line : new FileLines(resultFile, "UTF-8", null)) {
                int t = KB.map(line.trim());
                if (results.contains(t)) {
                    clean = false;
                    System.err.println("ERROR:"+args[i]+": Duplicates found");
                }
                results.add(t);
            }

            IntSet cleanedResults = new IntOpenHashSet(results);
            for (int c1 : results) {
                for (int c2 : results) {
                    if (c1 == c2) {
                        continue;
                    }
                    if (Schema.isTransitiveSuperType(taxo, c2, c1)) {
                        clean = false;
                        System.err.println("ERROR:"+args[i]+": "+ KB.unmap(c1) + " in " + KB.unmap(c2));
                        cleanedResults.remove(c1);
                        break;
                    }
                }
            }
            if (!clean) {
                BufferedWriter w = new BufferedWriter(new FileWriter(args[i]+"_cleaned"));
                for (int c : cleanedResults) {
                    w.write(KB.unmap(c)+"\n");
                }
                w.close();
            }
        }
    }
}
