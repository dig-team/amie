/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import static amie.data.KB.DIFFERENTFROMbs;
import static amie.data.KB.EQUALSbs;
import static amie.data.KB.EXISTSINVbs;
import static amie.data.KB.EXISTSbs;
import static amie.data.KB.NOTEXISTSINVbs;
import static amie.data.KB.NOTEXISTSbs;
import static amie.data.KB.TRANSITIVETYPEbs;
import static amie.data.KB.isVariable;
import static amie.data.KB.numVariables;
import static amie.data.KB.toString;
import static amie.data.KB.varpos;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author jlajus
 */
public class KBLFTJ extends KB {
    
	private IntSet getAllVariables(List<int[]> query) {
		IntSet result = new IntOpenHashSet();
		for(int[] atom : query) {
			for(int entity : atom) {
				if(isVariable(entity))
					result.add(entity);
			}
		}
		return result;
	}
	
	public static int varposInv(int variable, int[] triple) {
		for(int i = 0; i < triple.length; i++) {
			if(isVariable(triple[i]) && triple[i]!=(variable))
				return i;
		}
		return -1;
	}
	
	private IntSet getPossibleValues(int variable, int[] triple) {
		switch(numVariables(triple)) {
		case 1:
			return resultsOneVariable(triple);
		case 2:
			if (triple[1] == TRANSITIVETYPEbs) {
				switch(varpos(variable, triple)) {
				case 0:
					return get(relation2subject2object, Schema.typeRelationBS).keySet();
				case 2:
					if (Schema.isTaxonomyMaterialized()) 
						return Schema.getAllDefinedTypes();
					return get(relation2object2subject, Schema.typeRelationBS).keySet();
					/*
					 * Return a map from all types to all entities of sub-classes
					 */
				}
			}
			if (triple[1]==(DIFFERENTFROMbs) || triple[1]==(EQUALSbs))
				return subject2object2relation.keySet();
			if (triple[1]==(EXISTSbs) || triple[1]==(EXISTSINVbs) || triple[1]==(NOTEXISTSbs) || triple[1]==(NOTEXISTSINVbs)) {
				switch(varpos(variable, triple)) {
				case 0: return subject2object2relation.keySet();
				case 2: return relation2subject2object.keySet();
				}
			}
			
			switch (varpos(variable, triple)) {
			case 0:
				switch (varposInv(variable, triple)) {
				case 1:
					return (get(object2subject2relation, triple[2]).keySet());
				case 2:
					return (get(relation2subject2object, triple[1]).keySet());
				}
				break;
			case 1:
				switch (varposInv(variable, triple)) {
				case 0:
					return get(object2relation2subject, triple[2]).keySet();
				case 2:
					return get(subject2relation2object, triple[0]).keySet();
				}
				break;
			case 2:
				switch (varposInv(variable, triple)) {
				case 0:
					return get(relation2object2subject, triple[1]).keySet();
				case 1:
					return get(subject2object2relation, triple[0]).keySet();
				}
				break;
			}
			break;
		case 3:
			switch(varpos(variable, triple)) {
			case 0:
				return subject2relation2object.keySet();
			case 1:
				return relation2subject2object.keySet();
			case 2:
				return object2subject2relation.keySet();
			}
			break;
		}
		throw new IllegalArgumentException("Invalid variable " + variable + " in triple: " + toString(triple));
	}
	
	/** returns the instances that fulfill a certain condition */
	
	public boolean existsBSLFTJ(List<int[]> query) {
		
		Iterator<int[]> it = query.iterator();
		List<int[]> newQuery = new ArrayList<>(query.size());
		
		// copy query and get rid of all ground atom
		while(it.hasNext()) {
			int[] atom = it.next();
			if(numVariables(atom) == 0) {
				if(!contains(atom)) {
					return false;
				}
			} else {
				newQuery.add(atom);
			}
		}
		if(newQuery.isEmpty())
			return true;
		
		IntSet variables = getAllVariables(newQuery);
		int variable = variables.iterator().next();
		
		IntSet possibleValues = new IntOpenHashSet();
		boolean first = true;
		for(int[] atom : newQuery) {
			if(varpos(variable, atom) == -1)
				continue;
			if(first) {
				possibleValues = new IntOpenHashSet(getPossibleValues(variable, atom));
				first = false;
			} else {
				possibleValues.retainAll(getPossibleValues(variable, atom));
			}
		}
		
		Instantiator insty = new Instantiator(newQuery, variable);
		boolean result = false;
		
		for(int value : possibleValues) {
			if(existsBSLFTJ(insty.instantiate(value))) {
				result = true;
				break;
                        }
		}
		insty.close();
		
		return result;
	}
	
	public IntSet selectDistinctLFTJ(int variable,
			List<int[]> query) {
		
		Iterator<int[]> it = query.iterator();
		IntSet result = new IntOpenHashSet();
		List<int[]> newQuery = new ArrayList<>(query.size());
		
		// copy query and get rid of all ground atom
		while(it.hasNext()) {
			int[] atom = it.next();
			if(numVariables(atom) == 0) {
				if(!contains(atom)) {
					return result;
				}
			} else {
				newQuery.add(atom);
			}
		}
		// generate the list of all possible values
		IntSet possibleValues = new IntOpenHashSet();
		boolean first = true;
		for(int[] atom : newQuery) {
			if(varpos(variable, atom) == -1)
				continue;
			if(first) {
				possibleValues = new IntOpenHashSet(getPossibleValues(variable, atom));
				first = false;
			} else {
				possibleValues.retainAll(getPossibleValues(variable, atom));
			}
		}
		// if there is no other variables return the possible values
		IntSet variables = getAllVariables(newQuery);
		variables.remove(variable);
		if (variables.isEmpty())
			return possibleValues;
		
		// otherwise instantiate and filter the possible values
		Instantiator insty = new Instantiator(newQuery, variable);
		for(int value : possibleValues) {
			if(existsBSLFTJ(insty.instantiate(value))) 
				result.add(value);
		}
		insty.close();
		
		return result;
	}
	
	public Int2ObjectMap<IntSet> selectDistinctLFTJ(int var1, int var2,
			List<int[]> query) {
		
		Iterator<int[]> it = query.iterator();
		Int2ObjectMap<IntSet> result = new Int2ObjectOpenHashMap<>();
		List<int[]> newQuery = new ArrayList<>(query.size());
		
		while(it.hasNext()) {
			int[] atom = it.next();
			if(numVariables(atom) == 0) {
				if(!contains(atom)) {
					return result;
				}
			} else {
				newQuery.add(atom);
			}
		}
		IntSet possibleValues = new IntOpenHashSet();
		boolean first = true;
		for(int[] atom : newQuery) {
			if(varpos(var1, atom) == -1)
				continue;
			if(first) {
				possibleValues = new IntOpenHashSet(getPossibleValues(var1, atom));
				first = false;
			} else {
				possibleValues.retainAll(getPossibleValues(var1, atom));
			}
		}
		
		Instantiator insty = new Instantiator(newQuery, var1);
		for(int value : possibleValues) {
			IntSet innerResult = selectDistinct(var2, insty.instantiate(value));
			if(!innerResult.isEmpty()) 
				result.put(value, innerResult);
		}
		insty.close();
		
		return result;
	}
	
	/**
	 * Count pairs with LFTJ.
	 * @TODO Had count cache and/or remove duplicate countDistinctPairs
	 * @param var1
	 * @param var2
	 * @param query
	 * @return
	 */
	@SuppressWarnings({ "unused" })
	private long countDistinctLFTJ(int var1, int var2,
			List<int[]> query) {
		
		Iterator<int[]> it = query.iterator();
		long result = 0;
		List<int[]> newQuery = new ArrayList<>(query.size());
		
		while(it.hasNext()) {
			int[] atom = it.next();
			if(numVariables(atom) == 0) {
				if(!contains(atom)) {
					return result;
				}
			} else {
				newQuery.add(atom);
			}
		}
		IntSet possibleValues = new IntOpenHashSet();
		boolean first = true;
		for(int[] atom : newQuery) {
			if(varpos(var1, atom) == -1)
				continue;
			if(first) {
				possibleValues = new IntOpenHashSet(getPossibleValues(var1, atom));
				first = false;
			} else {
				possibleValues.retainAll(getPossibleValues(var1, atom));
			}
		}
		
		Instantiator insty = new Instantiator(newQuery, var1);
		for(int value : possibleValues) {
			result += selectDistinctLFTJ(var2, insty.instantiate(value)).size();
		}
		insty.close();
		
		return result;
	}
	
	public Int2ObjectMap<Int2ObjectMap<IntSet>> selectDistinctLFTJ(int var1, int var2, int var3,
			List<int[]> query) {
		
		Iterator<int[]> it = query.iterator();
		Int2ObjectMap<Int2ObjectMap<IntSet>> result = new Int2ObjectOpenHashMap<>();
		List<int[]> newQuery = new ArrayList<>(query.size());
		
		while(it.hasNext()) {
			int[] atom = it.next();
			if(numVariables(atom) == 0) {
				if(!contains(atom)) {
					return result;
				}
			} else {
				newQuery.add(atom);
			}
		}
		IntSet possibleValues = new IntOpenHashSet();
		boolean first = true;
		for(int[] atom : newQuery) {
			if(varpos(var1, atom) == -1)
				continue;
			if(first) {
				possibleValues = new IntOpenHashSet(getPossibleValues(var1, atom));
				first = false;
			} else {
				possibleValues.retainAll(getPossibleValues(var1, atom));
			}
		}
		
		Instantiator insty = new Instantiator(newQuery, var1);
		for(int value : possibleValues) {
			Int2ObjectMap<IntSet> innerResult = selectDistinctLFTJ(var2, var3, insty.instantiate(value));
			if(!innerResult.isEmpty())
				result.put(value, innerResult);
		}
		insty.close();
		
		return result;
	}
}
