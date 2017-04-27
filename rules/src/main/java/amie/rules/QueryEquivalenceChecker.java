package amie.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javatools.datatypes.ByteString;
import amie.data.KB;

/**
 * This class takes two lists of atoms representing conjunctive queries
 * and determines whether they are equivalent or not.
 * 
 * @author galarrag
 *
 */
public class QueryEquivalenceChecker {
	
	static class Node implements Comparable<Node>{
		ByteString[] data;
		
		boolean removed;
		
		boolean visited;
		
		boolean headNode;
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Node [data=" + Arrays.toString(data) + "]";
		}

		Node(ByteString[] data, boolean isHead){
			this.data = data;
			this.removed = false;
			this.visited = false;
			this.headNode = isHead;
		}

		@Override
		public int compareTo(Node otherNode) {
			int comparePred = compare(this.data[1], otherNode.data[1]);			
			if(comparePred == 0){
				int compareObj = compare(this.data[2], otherNode.data[2]);
				if(compareObj == 0){
					return compare(this.data[0], otherNode.data[0]);
				}else{
					return compareObj;
				}
			}
			
			return comparePred;
		}

		private int compare(ByteString b1, ByteString b2) {
			// TODO Auto-generated method stub
			if(KB.isVariable(b1)){
				if(KB.isVariable(b2)){
					return 0;
				}else{
					return -1;
				}
			}else{
				if(KB.isVariable(b2)){
					return 1;
				}else{
					return b1.toString().compareTo(b2.toString());
				}				
			}
		}
	}
	
	/**
	 * The graph representation of a conjunctive query on binary atoms.
	 * @author galarrag
	 *
	 */
	static class QueryGraph{
		List<Node> nodes;
		List<List<int[]>> outgoingEdges;
		List<List<int[]>> incomingEdges;
		int nEdges;
		
		QueryGraph(List<Node> nodes, List<List<int[]>> outgoingEdges, List<List<int[]>> ingoingEdges, int nedges){
			this.nodes = nodes;
			this.outgoingEdges = outgoingEdges;
			this.incomingEdges = ingoingEdges;
			this.nEdges = nedges;
		}

		public boolean equivalent(QueryGraph graph) {			
			if(nodes.size() != graph.nodes.size() || nEdges != graph.nEdges || nodes.get(0).compareTo(graph.nodes.get(0)) != 0 ){
				return false;
			}
			
			List<int[]> knownMappings = new ArrayList<int[]>();
			
			for(int i = 0; i < nodes.size(); ++i){
				boolean match = false;
				for(int j = 0; j < nodes.size(); ++j){
					boolean eq = equivalent(this, i, graph, j, knownMappings);
					if(nodes.get(i).headNode && graph.nodes.get(j).headNode && !eq) return false; //Subtle bug
					graph.unvisit();
					if(eq){
						match = true;
						break;					
					}
				}
				
				if(!match)
					return false;
			}
			
			return true;
		}

		private void unvisit() {
			// TODO Auto-generated method stub
			for(Node node: nodes)
				node.visited = false;
			
		}
		
		/**
		 * It determines whether two query graphs are equivalent.
		 * @param g1
		 * @param i
		 * @param g2
		 * @param j
		 * @param mappings
		 * @return
		 */
		private boolean equivalent(QueryGraph g1, int i, QueryGraph g2, int j, List<int[]> mappings) {
			if(g2.nodes.get(j).removed){
				//It means g2.nodes[j] did actually found a match which is not g1.node[i]
				if(existsMapping(i,j,mappings))
					return true;
				
				return false;
			}
			
			if(g2.nodes.get(j).visited) //Avoid visiting the node again and again
				return true;
			
			//Mark it as visited
			g2.nodes.get(j).visited = true;
			
			if(g1.nodes.get(i).compareTo(g2.nodes.get(j)) != 0)
				return false;
							
			List<int[]> oe1 = g1.outgoingEdges.get(i);
			List<int[]> ie1 = g1.incomingEdges.get(i);
			
			List<int[]> oe2 = g2.outgoingEdges.get(j);			
			List<int[]> ie2 = g2.incomingEdges.get(j);
			
			List<int[]> e1 = new ArrayList<int[]>(oe1);
			e1.addAll(ie1);
			
			List<int[]> e2 = new ArrayList<int[]>(oe2);
			e2.addAll(ie2);
			
			if(e1.size() != e2.size())
				return false;
			
			//Make sure the outgoingEdges match 
			for(int[] edge1: e1){
				boolean match = false;
				for(int[] edge2: e2){
					if((
						(edge1[1] == edge2[1] && edge1[2] == edge2[2]) || (edge1[1] == edge2[2] && edge1[2] == edge2[1])
						) && 
						equivalent(g1, edge1[0], g2, edge2[0], mappings)){
						match = true;
						break;
					}
				}
				
				if(!match){
					return false;
				}
			}
			
			//Remove the node and add the mapping
			g2.nodes.get(j).removed = true;			
			int newMapping[] = new int[2];
			newMapping[0] = i;
			newMapping[1] = j;
			mappings.add(newMapping);
			
			return true;	
		}

		/**
		 * Determines whether there exists a mapping between nodes i and j in the
		 * given list of mappings.
		 * @param i
		 * @param j
		 * @param mappings
		 * @return
		 */
		private boolean existsMapping(int i, int j, List<int[]> mappings) {
			// TODO Auto-generated method stub
			for(int[] mapping: mappings){
				if(mapping[0] == i && mapping[1] == j)
					return true;
			}
			
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "QueryGraph [nodes=" + nodes + ", outgoingEdges=" + outgoingEdges
					+ ", nEdges=" + nEdges + "]";
		}
	}
	
	/**
	 * It determines whether two conjunctive queries represented as list of atoms
	 * are equivalent.
	 * @param q1
	 * @param q2
	 * @return
	 */
	public static boolean areEquivalent(List<ByteString[]> q1, List<ByteString[]> q2){
		if(q1.size() == q2.size() && q1.size() == 1){
			return Rule.areEquivalent(q1.get(0), q2.get(0));
		}else{
			return q1.size() == q2.size() && buildQueryGraph(q1).equivalent(buildQueryGraph(q2));
		}
	}

	/**
	 * It returns the query graph representation of a conjunctive query.
	 * @param q2
	 * @return
	 */
	private static QueryGraph buildQueryGraph(List<ByteString[]> q2) {
		SortedSet<Node> nodes = new TreeSet<Node>();
		List<Node> hardNodes = new ArrayList<Node>();
		List<Node> nodeList = null;
		int edgesCount = 0;
		List<List<int[]>> outEdges = new ArrayList<List<int[]>>();
		List<List<int[]>> inEdges = new ArrayList<List<int[]>>();
		
		Node headNode = new Node(q2.get(0), true);
		
		for(ByteString[] triple: q2.subList(1, q2.size())){
			Node newNode = new Node(triple, false);
			if(nodes.contains(newNode))
				hardNodes.add(newNode);
			else
				nodes.add(newNode);
		}
		
		nodeList = new ArrayList<Node>();
		nodeList.add(headNode);
		for(Node node: nodes){
			nodeList.add(node);
			nodeList.addAll(findEquivalentNodes(node, hardNodes));
		}
		
		//Now build the outgoingEdges
		for(int i = 0; i < nodeList.size(); ++i){
			outEdges.add(new ArrayList<int[]>());
			inEdges.add(new  ArrayList<int[]>());
			for(int j = i + 1; j < nodeList.size(); ++j){
				for(int i1 = 0; i1 < nodeList.get(i).data.length; ++i1){
					for(int j1 = 0; j1 < nodeList.get(j).data.length; ++j1){
						ByteString vari, varj;
						vari = nodeList.get(i).data[i1];
						varj = nodeList.get(j).data[j1];
						if(KB.isVariable(vari) && vari.equals(varj)){
							int[] edge = new int[3];
							edge[0] = j; // Target pattern
							edge[1] = i1; //Position in local pattern
							edge[2] = j1; //Position in target pattern
							outEdges.get(i).add(edge);
							++edgesCount;
						}
					}
				}
			}
		}
		
		//Now build the incoming edges
		for(int i = 0; i < nodeList.size(); ++i){
			for(int[] outEdge: outEdges.get(i)){
				List<int[]> targetList = inEdges.get(outEdge[0]);
				int newInEdge[] = new int[3];
				newInEdge[0] = i;
				newInEdge[1] = outEdge[1];
				newInEdge[2] = outEdge[2];
				targetList.add(newInEdge);
				++edgesCount;
			}
		}
		
		return new QueryGraph(nodeList, outEdges, inEdges, edgesCount);
	}
	
	/**
	 * It returns a list of potentially equivalent candidates for the key
	 * in the list of hard nodes.
	 * @param key
	 * @param hardNodes
	 * @return
	 */
	private static Collection<Node> findEquivalentNodes(Node key, List<Node> hardNodes) {
		List<Node> result = new ArrayList<Node>();
		for(Node node: hardNodes){
			if(node.compareTo(key) == 0)
				result.add(node);
		}
		
		return result;
	}
}