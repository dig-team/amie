/**
 * @author lgalarra
 * @date Sep 13, 2013
 */
package amie.data.utils;

import java.io.IOException;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;


/**
 * @author lgalarra
 *
 */
public class MaterializeInverseFunctions {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		KB db = amie.data.U.loadFiles(args);
		// TODO Auto-generated method stub
		db.schema.loadSchemaConf();
		System.out.println("Type relation: " + amie.data.Schema.typeRelation);
		Int2ObjectMap<Int2ObjectMap<IntSet>> map =
				db.resultsThreeVariables(db.map("?s"), db.map("?p"), db.map("?o"),
						db.triple("?o", "?p", "?s"));
		for(int object: map.keySet()){
			Int2ObjectMap<IntSet > predicates = map.get(object);
			for(int predicate: predicates.keySet()){
				if(db.functionality(predicate) >= db.inverseFunctionality(predicate)){
					for(int subject: predicates.get(predicate))
						System.out.println(subject + "\t" + predicate + "\t" + object);
				} else {
					for(int subject: predicates.get(predicate))
						System.out.println(object + "\t" + "<inv-" 
					+ db.unmap(predicate).subSequence(1, db.unmap(predicate).length()) + "\t" + subject);
				}
			}
		}
	}

}
