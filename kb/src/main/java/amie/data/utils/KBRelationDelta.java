package amie.data.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import amie.data.KB;
import amie.data.Schema;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;


public class KBRelationDelta {

	/**
	 * Utility that verifies for every pair entity, relation whether there has been 
	 * a change between two versions of the KB.
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		// Old KB
		KB db1 = new KB();
		// New KB
		KB db2 = new KB();
		
		List<File> oldFiles = new ArrayList<>();
		List<File> newFiles = new ArrayList<>();
		
		for (String file : args) {
			if (file.startsWith(":t")) {
				newFiles.add(new File(file.substring(2, file.length())));
			} else {
				oldFiles.add(new File(file));
			}
		}
		
		
		db1.load(oldFiles);
		db2.load(newFiles);
		
		IntList r1 = db1.getRelationsList();
		Queue<Integer> r2 = new LinkedList<>(db2.getRelationsList());
		
		r2.retainAll(r1);
		
		final IntCollection allEntitiesOldKB = db1.getAllEntities();
		
		while (!r2.isEmpty()) {
			List<Thread> threads = new ArrayList<>();
			for (int i = 0; i < Math.max(1, Runtime.getRuntime().availableProcessors() / 2); ++i) {
				final int relation = r2.poll();
				System.out.println("Analyzing relation " + relation);
				if (relation == 0)
					break;
				
				final KB ddb1 = db1;
				final KB ddb2 = db2;
				
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						System.out.println("Starting " + relation);
						PrintWriter out = null;
						try {
							out = new PrintWriter(new FileWriter(new File(relation + ".hasChanged.tsv")));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
                                                        System.exit(2);
						}
						int domain = 0;
						int qVariable = 0;
						String relationlabel = null;
						List<int[]> query = KB.triples(KB.triple(KB.map("?s"), relation, KB.map("?o")));
						int[] query2 = KB.triple(KB.map("?s"), relation, KB.map("?o"));
						boolean isFunctional = ddb2.isFunctional(relation); 
						if (isFunctional) {
							domain = Schema.getRelationDomain(ddb2, relation);
							qVariable = query.get(0)[0];
							relationlabel = KB.unmap(relation);
						} else {
							domain = Schema.getRelationRange(ddb2, relation);
							qVariable = query.get(0)[2];
							relationlabel = KB.unmap(relation).replace(">", "-inv>");
						}
						System.out.println("Getting the values for the domain");
						IntSet entities = new IntOpenHashSet(Schema.getAllEntitiesForType(ddb2, domain));					
						entities.addAll(ddb2.selectDistinct(qVariable, query));
						System.out.println("Checking those that appear in the old database");
						entities.retainAll(allEntitiesOldKB);			
						System.out.println("Checking for changes");
						for (int entity : entities) {
							if (isFunctional) {
								query2[0] = entity;
								query2[2] = KB.map("?o");
							} else {
								query2[2] = entity;
								query2[0] = KB.map("?s");
							}
							IntSet s1 = ddb1.resultsOneVariable(query2);
							IntSet s2 = ddb2.resultsOneVariable(query2);
							String outcome = null;
							if (s1.equals(s2)) {
								outcome = "No change";
								out.println(entity + "\t<hasNotChanged>\t" + relation);
							} else if (s2.containsAll(s1)) {
								outcome = "Addition";
								out.println(entity + "\t<hasChanged>\t" + relation);
							} else if (s1.containsAll(s2)) {
								outcome = "Deletion";
							} else {
								outcome = "Other";
							}
							out.println(entity + "\t" + relationlabel + "\t" + outcome);
						}
					}
				});
				
				System.out.println("Starting the thread");
				t.start();
				System.out.println("Adding the thread to list of current threads");
				threads.add(t);
			}
			
			for (Thread t : threads)
				t.join();
		}
	}
}
