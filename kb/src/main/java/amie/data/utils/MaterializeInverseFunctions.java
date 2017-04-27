/**
 * @author lgalarra
 * @date Sep 13, 2013
 */
package amie.data.utils;

import java.io.IOException;
import java.util.Map;

import amie.data.KB;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

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
		Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> map = 
				db.resultsThreeVariables(ByteString.of("?s"), ByteString.of("?p"), ByteString.of("?o"),
						KB.triple("?o", "?p", "?s"));
		for(ByteString object: map.keySet()){
			Map<ByteString, IntHashMap<ByteString> > predicates = map.get(object);
			for(ByteString predicate: predicates.keySet()){
				if(db.functionality(predicate) >= db.inverseFunctionality(predicate)){
					for(ByteString subject: predicates.get(predicate))
						System.out.println(subject + "\t" + predicate + "\t" + object);
				} else {
					for(ByteString subject: predicates.get(predicate))
						System.out.println(object + "\t" + "<inv-" 
					+ predicate.subSequence(1, predicate.length()) + "\t" + subject);					
				}
			}
		}
	}

}
