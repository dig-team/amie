package amie.data.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import amie.data.KB;
import amie.data.Schema;
import amie.data.U;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class ComputeTypeDeductiveClosureWikidata {

    /**
     * Given the instance information of a KB and its type hierarchy (subclass
     * relationships), it computes the deductive closure.
     *
     * @param args
     * @throws IOException
     */
    
    public static void main(String[] args) throws IOException {
        Schema.typeRelation = "<P106>";
        Schema.typeRelationBS = KB.map(Schema.typeRelation);
        Schema.subClassRelation = "<P279>";
        Schema.subClassRelationBS = KB.map(Schema.subClassRelation);
        Schema.top = "<Q35120>";
        Schema.topBS = KB.map(Schema.top);
        System.out.println("Assuming " + Schema.typeRelation + " as type relation");
        KB kb = U.loadFiles(args, " ");
        Int2ObjectMap<IntSet> allEntitiesAndTypes
                = kb.resultsTwoVariables("?s", "?o", new String[]{"?s", amie.data.Schema.typeRelation, "?o"});
        PrintWriter pw  = new PrintWriter(new File("wikidataTransitiveTypes.tsv"));
        PrintWriter pw2 = new PrintWriter(new File("wikidataTypedEntities.tsv"));
        for (int entity : allEntitiesAndTypes.keySet()) {
            IntSet superTypes = new IntOpenHashSet(allEntitiesAndTypes.get(entity));
            for (int type : allEntitiesAndTypes.get(entity)) {
                superTypes.addAll(amie.data.Schema.getAllSuperTypes(kb, type));
            }
            if (superTypes.contains(Schema.topBS)) {
                output(entity, superTypes, pw);
                pw2.println(KB.unmap(entity).replace("<", "").replace(">", ""));
            }
        }
    }

    /**
     * Outputs statements of the form entity rdf:type type in TSV format
     *
     * @param entity
     * @param superTypes
     * @throws FileNotFoundException
     */
    private static void output(int entity, IntSet superTypes, PrintWriter pw) {
        for (int type : superTypes) {
            pw.println(entity + " " + Schema.typeRelation + " " + type);
        }
    }
}
