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
        KB kb = U.loadFiles(args, " ");
        kb.schema.typeRelation = "<P106>";
        kb.schema.typeRelationBS = kb.map(kb.schema.typeRelation);
        kb.schema.subClassRelation = "<P279>";
        kb.schema.subClassRelationBS = kb.map(kb.schema.subClassRelation);
        kb.schema.top = "<Q35120>";
        kb.schema.topBS = kb.map(kb.schema.top);
        System.out.println("Assuming " + kb.schema.typeRelation + " as type relation");
        Int2ObjectMap<IntSet> allEntitiesAndTypes
                = kb.resultsTwoVariables("?s", "?o", new String[]{"?s", kb.schema.typeRelation, "?o"});
        PrintWriter pw  = new PrintWriter(new File("wikidataTransitiveTypes.tsv"));
        PrintWriter pw2 = new PrintWriter(new File("wikidataTypedEntities.tsv"));
        for (int entity : allEntitiesAndTypes.keySet()) {
            IntSet superTypes = new IntOpenHashSet(allEntitiesAndTypes.get(entity));
            for (int type : allEntitiesAndTypes.get(entity)) {
                superTypes.addAll(kb.schema.getAllSuperTypes(kb, type));
            }
            if (superTypes.contains(kb.schema.topBS)) {
                output(entity, superTypes, pw);
                pw2.println(kb.unmap(entity).replace("<", "").replace(">", ""));
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
