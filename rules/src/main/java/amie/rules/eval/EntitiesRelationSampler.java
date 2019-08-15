package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;

import amie.data.KB;

/**
 * For each relation r of a given KB, it takes a sample of the entities x in the domain of the relation r
 * and outputs all the facts of the form r(x,y)
 * @author galarrag
 *
 */
public class EntitiesRelationSampler {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		KB db = new KB();
		int maxOccurrencePerRelation = Integer.parseInt(args[0]);		
		db.load(new File(args[1]));
		Set<ByteString> allEntities = db.selectDistinct(ByteString.of("?s"), KB.triples(KB.triple(ByteString.of("?s"), ByteString.of("?p"), ByteString.of("?o"))));
		List<ByteString> allRelations = new ArrayList<ByteString>(db.selectDistinct(ByteString.of("?p"), KB.triples(KB.triple(ByteString.of("?s"), ByteString.of("?p"), ByteString.of("?o")))));
		List<ByteString> entitiesArray = new ArrayList<ByteString>(allEntities); 
		Map<ByteString, List<Pair<ByteString, ByteString>>> relationsMap = new HashMap<ByteString, List<Pair<ByteString, ByteString>>>();
		IntHashMap<ByteString> relationEntityCount = new IntHashMap<ByteString>();
		
		for(ByteString relation: allRelations){
			relationEntityCount.put(relation, 0);
			relationsMap.put(relation, new ArrayList<Pair<ByteString, ByteString>>());
		}
		
		while(!allRelations.isEmpty() && !entitiesArray.isEmpty()){
			//Pick a random entity
			Random r = new Random();
			int position = r.nextInt(entitiesArray.size());
			ByteString entity = entitiesArray.get(position);
			swap(entitiesArray, position, entitiesArray.size() - 1);
			entitiesArray.remove(entitiesArray.size() - 1);
			
			//Now take all the triples about this entity
			List<ByteString[]> query = KB.triples(KB.triple(entity, ByteString.of("?p"), ByteString.of("?o")));
			Map<ByteString, Set<ByteString>> predicateObjects = db.selectDistinct(ByteString.of("?p"), ByteString.of("?o"), query);
			
			for(ByteString relation: predicateObjects.keySet()){				
				if(relationEntityCount.get(relation) >= maxOccurrencePerRelation){
					continue;
				}
				
				relationEntityCount.increase(relation);
				List<Pair<ByteString, ByteString>> facts = relationsMap.get(relation);
				for(ByteString object: predicateObjects.get(relation)){
					facts.add(new Pair<ByteString, ByteString>(entity, object));
				}
				
				if(relationEntityCount.get(relation) >= maxOccurrencePerRelation)
					allRelations.remove(relation);
			}
		}
		
		for(ByteString relation: relationsMap.keySet()){
			for(Pair<ByteString, ByteString> entityObject: relationsMap.get(relation))
				System.out.println(entityObject.first + "\t" + relation + "\t" + entityObject.second);
		}
		
	}

	private static void swap(List<ByteString> entitiesArray, int i, int j) {
		ByteString tmp = entitiesArray.get(i);
		entitiesArray.set(i, entitiesArray.get(j));
		entitiesArray.set(j, tmp);
	}

}
