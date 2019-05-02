package amie.data.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;

import amie.data.KB;
import amie.data.Schema;
import amie.data.U;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

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
        Schema.typeRelationBS = ByteString.of(Schema.typeRelation);
        Schema.subClassRelation = "<P279>";
        Schema.subClassRelationBS = ByteString.of(Schema.subClassRelation);
        Schema.top = "<Q35120>";
        Schema.topBS = ByteString.of(Schema.top);
        System.out.println("Assuming " + Schema.typeRelation + " as type relation");
        KB kb = U.loadFiles(args, " ");
        Map<ByteString, IntHashMap<ByteString>> allEntitiesAndTypes
                = kb.resultsTwoVariables("?s", "?o", new String[]{"?s", amie.data.Schema.typeRelation, "?o"});
        PrintWriter pw  = new PrintWriter(new File("wikidataTransitiveTypes.tsv"));
        PrintWriter pw2 = new PrintWriter(new File("wikidataTypedEntities.tsv"));
        for (ByteString entity : allEntitiesAndTypes.keySet()) {
            Set<ByteString> superTypes = new LinkedHashSet<>(allEntitiesAndTypes.get(entity));
            for (ByteString type : allEntitiesAndTypes.get(entity)) {
                superTypes.addAll(amie.data.Schema.getAllSuperTypes(kb, type));
            }
            if (superTypes.contains(Schema.topBS)) {
                output(entity, superTypes, pw);
                pw2.println(entity.toString().replace("<", "").replace(">", ""));
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
    private static void output(ByteString entity, Set<ByteString> superTypes, PrintWriter pw) {
        for (ByteString type : superTypes) {
            pw.println(entity + " " + Schema.typeRelation + " " + type);
        }
    }
}
