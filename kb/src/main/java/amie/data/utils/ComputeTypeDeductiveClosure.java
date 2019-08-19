package amie.data.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import amie.data.KB;
import amie.data.U;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class ComputeTypeDeductiveClosure {

	/**
	 * Given the instance information of a KB and its type hierarchy (subclass relationships), it computes
	 * the deductive closure.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// It loads the properties from the file ../conf/schema_properties
		amie.data.Schema.loadSchemaConf();
		System.out.println("Assuming " + amie.data.Schema.typeRelation + " as type relation");
		KB kb = U.loadFiles(args);
		Int2ObjectMap<IntSet> allEntitiesAndTypes = 
				kb.resultsTwoVariables("?s", "?o", new String[]{"?s", amie.data.Schema.typeRelation, "?o"});
		PrintWriter pw = new PrintWriter(new File("inferredTypes.tsv"));
		for (int entity : allEntitiesAndTypes.keySet()) {
			IntSet superTypes = new IntOpenHashSet();
			for (int type : allEntitiesAndTypes.get(entity)) {
				superTypes.addAll(amie.data.Schema.getAllSuperTypes(kb, type));	
			}
			// And be sure we add only the new ones
			superTypes.removeAll(allEntitiesAndTypes.get(entity));
			output(entity, superTypes, pw);
		}
	}

	/**
	 * Outputs statements of the form entity rdf:type type in TSV format
	 * @param entity
	 * @param superTypes
	 * @throws FileNotFoundException 
	 */
	private static void output(int entity, IntSet superTypes, PrintWriter pw) {
		for (int type : superTypes) {
			pw.println(entity + "\t" + amie.data.Schema.typeRelation + "\t" + type);
		}
	}
}
