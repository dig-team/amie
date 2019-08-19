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
		// TODO Auto-generated method stub
		amie.data.Schema.loadSchemaConf();
		System.out.println("Type relation: " + amie.data.Schema.typeRelation);
		KB db = amie.data.U.loadFiles(args);
		Int2ObjectMap<Int2ObjectMap<IntSet>> map = 
				db.resultsThreeVariables(KB.map("?s"), KB.map("?p"), KB.map("?o"),
						KB.triple("?o", "?p", "?s"));
		for(int object: map.keySet()){
			Int2ObjectMap<IntSet > predicates = map.get(object);
			for(int predicate: predicates.keySet()){
				if(db.functionality(predicate) >= db.inverseFunctionality(predicate)){
					for(int subject: predicates.get(predicate))
						System.out.println(subject + "\t" + predicate + "\t" + object);
				} else {
					for(int subject: predicates.get(predicate))
						System.out.println(object + "\t" + "<inv-" 
					+ KB.unmap(predicate).subSequence(1, KB.unmap(predicate).length()) + "\t" + subject);					
				}
			}
		}
	}

}
