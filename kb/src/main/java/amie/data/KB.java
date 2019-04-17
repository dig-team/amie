package amie.data;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.OperationNotSupportedException;

import javatools.administrative.Announce;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;
import javatools.datatypes.Triple;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.parsers.NumberFormatter;

/**
 * Class KB
 * 
 * This class implements an in-memory knowledge base (KB) for facts without identifiers. 
 * It supports a series of conjunctive queries.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class KB {

	// ---------------------------------------------------------------------------
	// Indexes
	// ---------------------------------------------------------------------------

	/** Index */
	protected final Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> subject2relation2object = new IdentityHashMap<ByteString, Map<ByteString, IntHashMap<ByteString>>>();

	/** Index */
	protected final Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> relation2object2subject = new IdentityHashMap<ByteString, Map<ByteString, IntHashMap<ByteString>>>();

	/** Index */
	protected final Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> object2subject2relation = new IdentityHashMap<ByteString, Map<ByteString, IntHashMap<ByteString>>>();

	/** Index */
	protected final Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> relation2subject2object = new IdentityHashMap<ByteString, Map<ByteString, IntHashMap<ByteString>>>();

	/** Index */
	protected final Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> object2relation2subject = new IdentityHashMap<ByteString, Map<ByteString, IntHashMap<ByteString>>>();

	/** Index */
	protected final Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> subject2object2relation = new IdentityHashMap<ByteString, Map<ByteString, IntHashMap<ByteString>>>();

	/** Number of facts per subject */
	protected final IntHashMap<ByteString> subjectSize = new IntHashMap<ByteString>();

	/** Number of facts per object */
	protected final IntHashMap<ByteString> objectSize = new IntHashMap<ByteString>();

	/** Number of facts per relation */
	protected final IntHashMap<ByteString> relationSize = new IntHashMap<ByteString>();

	// ---------------------------------------------------------------------------
	// Statistics
	// ---------------------------------------------------------------------------
	
	/**
	 * Subject-subject overlaps
	 */
	protected final Map<ByteString, IntHashMap<ByteString>> subject2subjectOverlap = new IdentityHashMap<ByteString, IntHashMap<ByteString>>();

	/**
	 * Subject-object overlaps
	 */
	protected final Map<ByteString, IntHashMap<ByteString>> subject2objectOverlap = new IdentityHashMap<ByteString, IntHashMap<ByteString>>();

	/**
	 * Object-object overlaps
	 */
	protected final Map<ByteString, IntHashMap<ByteString>> object2objectOverlap = new IdentityHashMap<ByteString, IntHashMap<ByteString>>();

	/** Number of facts */
	protected long size;
	
	// ---------------------------------------------------------------------------
	// Constants
	// ---------------------------------------------------------------------------

	/** X transitiveType T predicate **/
    public static final String TRANSITIVETYPEstr = "transitiveType";
	
	public static final ByteString TRANSITIVETYPEbs = ByteString.of(TRANSITIVETYPEstr);
	
	/** (X differentFrom Y Z ...) predicate */
	public static final String DIFFERENTFROMstr = "differentFrom";

	/** (X differentFrom Y Z ...) predicate */
	public static final ByteString DIFFERENTFROMbs = ByteString
			.of(DIFFERENTFROMstr);

	/** (X equals Y Z ...) predicate */
	public static final String EQUALSstr = "equals";

	/** (X equals Y Z ...) predicate */
	public static final ByteString EQUALSbs = ByteString.of(EQUALSstr);
	
	/** r(X, y') exists for some y', predicate */
	public static final String EXISTSstr = "exists";
	
	/** r(X, y') exists for some y', predicate */
	public static final ByteString EXISTSbs = ByteString.of(EXISTSstr);
	
	/** r(y', X) exists for some y', predicate */
	public static final String EXISTSINVstr = "existsInv";
	
	/** r(y', X) exists for some y', predicate */
	public static final ByteString EXISTSINVbs = ByteString.of(EXISTSINVstr);	
	
	/** r(X, y') does NOT exists for some y', predicate */
	public static final String NOTEXISTSstr = "~exists";
	
	/** r(X, y') does NOT exists for some y', predicate */
	public static final ByteString NOTEXISTSbs = ByteString.of(NOTEXISTSstr);
	
	/** r(y', X) does NOT exists for some y', predicate */
	public static final String NOTEXISTSINVstr = "~existsInv";
	
	/** r(y', X) does NOT exists for some y', predicate */
	public static final ByteString NOTEXISTSINVbs = ByteString.of(NOTEXISTSINVstr);
	
	/** Variable sign (as defined in SPARQL) **/
	public static final char VariableSign = '?';
	
	public static final List<ByteString> specialRelations = Collections.unmodifiableList(Arrays.asList(TRANSITIVETYPEbs, DIFFERENTFROMbs, 
			EQUALSbs, EXISTSbs, EXISTSINVbs, NOTEXISTSbs, NOTEXISTSINVbs));

	/** Identifiers for the overlap maps */
	public static final int SUBJECT2SUBJECT = 0;

	public static final int SUBJECT2OBJECT = 2;

	public static final int OBJECT2OBJECT = 4;
	
	public enum Column { Subject, Relation, Object };
	
	public static final String hasNumberOfValuesEquals = "hasNumberOfValuesEquals";
	
	public static final String hasNumberOfValuesEqualsInv = "hasNumberOfValuesEqualsInv";
	
	public static final ByteString hasNumberOfValuesEqualsBS = ByteString.of(hasNumberOfValuesEquals);
	
	public static final ByteString hasNumberOfValuesEqualsInvBS = ByteString.of(hasNumberOfValuesEqualsInv);
	
	public static final String hasNumberOfValuesGreaterThan = "hasNumberOfValuesGreaterThan";
	
	public static final String hasNumberOfValuesGreaterThanInv = "hasNumberOfValuesGreaterThanInv";
	
	public static final ByteString hasNumberOfValuesGreaterThanBS = ByteString.of(hasNumberOfValuesGreaterThan);
	
	public static final ByteString hasNumberOfValuesGreaterThanInvBS = ByteString.of(hasNumberOfValuesGreaterThanInv);
	
	public static final String hasNumberOfValuesSmallerThan = "hasNumberOfValuesSmallerThan";
			
	public static final String hasNumberOfValuesSmallerThanInv = "hasNumberOfValuesSmallerThanInv";		
	
	public static final ByteString hasNumberOfValuesSmallerThanBS = ByteString.of(hasNumberOfValuesSmallerThan);
	
	public static final ByteString hasNumberOfValuesSmallerThanInvBS = ByteString.of(hasNumberOfValuesSmallerThanInv);		
	
	// ---------------------------------------------------------------------------
	// Loading
	// ---------------------------------------------------------------------------

	protected String delimiter = "\t";
	
	public void setDelimiter(String newDelimiter) {
		delimiter = newDelimiter;
	}
	
	public KB() {}

	/** Methods to add single facts to the KB **/
	protected boolean add(ByteString subject, ByteString relation,
			ByteString object,
			Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> map) {
		synchronized (map) {
			Map<ByteString, IntHashMap<ByteString>> relation2object = map
					.get(subject);
			if (relation2object == null)
				map.put(subject,
						relation2object = new IdentityHashMap<ByteString, IntHashMap<ByteString>>());
			IntHashMap<ByteString> objects = relation2object.get(relation);
			if (objects == null)
				relation2object.put(relation,
						objects = new IntHashMap<ByteString>());
			return (objects.add(object));
		}
	}

	/**
	 * Adds a fact to the KB
	 * @param fact
	 * @return TRUE if the KB was changed, i.e., the fact did not exist before.
	 */
	public boolean add(CharSequence... fact) {
		if (fact.length == 3) {
			return (add(compress(fact[0]), compress(fact[1]), compress(fact[2])));
		} else if (fact.length == 4) {
			return (add(compress(fact[1]), compress(fact[2]), compress(fact[3])));
		} else {
			throw new IllegalArgumentException("Incorrect fact: " + Arrays.toString(fact));
		}
			
	}

	/**
	 * Adds a fact to the KB
	 * @param fact
	 * @return TRUE if the KB was changed, i.e., the fact did not exist before.
	 */
	public boolean add(ByteString... fact) {
		if (fact.length == 3) {
			return add(fact[0], fact[1], fact[2]);
		} else if (fact.length == 4) {
			return add(fact[1], fact[2], fact[3]);
		} else {
			throw new IllegalArgumentException("Incorrect fact: " + Arrays.toString(fact));
		}
	}

	/**
	 * Adds a fact to the KB
	 * @param subject
	 * @param relation
	 * @param object
	 * @return TRUE if the KB was changed, i.e., the fact did not exist before.
	 */	
	protected boolean add(ByteString subject, ByteString relation, ByteString object) {
		if (!add(subject, relation, object, subject2relation2object))
			return (false);
		add(relation, object, subject, relation2object2subject);
		add(object, subject, relation, object2subject2relation);
		add(relation, subject, object, relation2subject2object);
		add(object, relation, subject, object2relation2subject);
		add(subject, object, relation, subject2object2relation);
		synchronized (subjectSize) {
			subjectSize.increase(subject);
		}
		synchronized (relationSize) {
			relationSize.increase(relation);
		}
		synchronized (objectSize) {
			objectSize.increase(object);
		}

		synchronized (subject2subjectOverlap) {
			IntHashMap<ByteString> overlaps = subject2subjectOverlap
					.get(relation);
			if (overlaps == null) {
				subject2subjectOverlap.put(relation,
						new IntHashMap<ByteString>());
			}
		}

		synchronized (subject2objectOverlap) {
			IntHashMap<ByteString> overlaps = subject2objectOverlap
					.get(relation);
			if (overlaps == null) {
				subject2objectOverlap.put(relation,
						new IntHashMap<ByteString>());
			}
		}

		synchronized (object2objectOverlap) {
			IntHashMap<ByteString> overlaps = object2objectOverlap
					.get(relation);
			if (overlaps == null) {
				object2objectOverlap.put(relation,
						new IntHashMap<ByteString>());
			}
		}

		size++;
		return (true);
	}
	

	/**
	 * Add all the facts of the given KB into the current one.
	 * @param otherKb
	 */
	public int add(KB otherKb) {
		int count = 0;
		for (ByteString subject: otherKb.subject2relation2object.keySet()) {
			Map<ByteString, IntHashMap<ByteString>> subjectMap = 
					otherKb.subject2relation2object.get(subject);
			for (ByteString relation : subjectMap.keySet()) {
				for (ByteString object : subjectMap.get(relation)) {
					if (this.add(subject, relation, object))
						++count;
				}
			}
		}
		return count;
	}

	/** 
	 * Returns the number of facts in the KB. 
	 **/
	public long size() {
		return (size);
	}

	/**
	 * Returns the number of distinct entities in one column of the database.
	 * @param column 0 = Subject, 1 = Relation/Predicate, 2 = Object
	 * @return
	 */
	public long size(Column column) {
		switch (column) {
		case Subject:
			return subjectSize.size();
		case Relation:
			return relationSize.size();
		case Object:
			return objectSize.size();
		default:
			throw new IllegalArgumentException(
					"Unrecognized column position. "
					+ "Accepted values: Subject, Predicate, Object");
		}
	}
	
	/**
	 * Returns the number of relations in the database.
	 * @return
	 */
	public long relationsSize() {
		return size(Column.Relation);
	}
	
	/**
	 * Returns the number of entities in the database.
	 * @return
	 */
	public long entitiesSize() {
		Set<ByteString> entities = new HashSet<ByteString>(subjectSize);
		entities.addAll(objectSize);
		return entities.size();
	}

	/** TRUE if the ByteString is a SPARQL variable */
	public static boolean isVariable(CharSequence s) {
		return (s.length() > 0 && s.charAt(0) == VariableSign);
	}
	
        
        public static boolean isOpenableVariable(CharSequence s) {
            return (s.length() > 1 && s.charAt(0) == VariableSign && s.charAt(1) != '_');
        }

	/**
	 * It clears the overlap tables and rebuilds them. Recommended when new facts has been added
	 * to the KB after the initial loading.
	 */
	public void rebuildOverlapTables() {
		resetOverlapTables(); 
		buildOverlapTables();
	}

	/**
	 * It clears all overlap tables.
	 */
	private void resetOverlapTables() {
		resetMap(subject2subjectOverlap);
		resetMap(subject2objectOverlap);
		resetMap(object2objectOverlap);
		
	}

	/**
	 * It resets an overlap index.
	 * @param map
	 */
	private void resetMap(Map<ByteString, IntHashMap<ByteString>> map) {
		for (ByteString key : map.keySet()) {
			map.put(key, new IntHashMap<ByteString>());
		}
	}

	/**
	 * It builds the overlap tables for relations. They contain the number of subjects and
	 * objects in common between pairs of relations. They can be used for join cardinality estimation.
	 */
	public void buildOverlapTables() {
		for (ByteString r1 : relationSize) {
			Set<ByteString> subjects1 = relation2subject2object.get(r1)
					.keySet();
			Set<ByteString> objects1 = relation2object2subject.get(r1)
					.keySet();
			for (ByteString r2 : relationSize) {
				Set<ByteString> subjects2 = relation2subject2object.get(r2)
						.keySet();
				Set<ByteString> objects2 = relation2object2subject.get(r2)
						.keySet();

				if (!r1.equals(r2)) {
					int ssoverlap = computeOverlap(subjects1, subjects2);
					subject2subjectOverlap.get(r1).put(r2, ssoverlap);
					subject2subjectOverlap.get(r2).put(r1, ssoverlap);
				} else {
					subject2subjectOverlap.get(r1).put(r1, subjects2.size());
				}

				int soverlap1 = computeOverlap(subjects1, objects2);
				subject2objectOverlap.get(r1).put(r2, soverlap1);
				int soverlap2 = computeOverlap(subjects2, objects1);
				subject2objectOverlap.get(r2).put(r1, soverlap2);

				if (!r1.equals(r2)) {
					int oooverlap = computeOverlap(objects1, objects2);
					object2objectOverlap.get(r1).put(r2, oooverlap);
					object2objectOverlap.get(r2).put(r1, oooverlap);
				} else {
					object2objectOverlap.get(r1).put(r1, objects2.size());
				}
			}
		}
	}

	/**
	 * Calculates the number of elements in the intersection of two sets of ByteStrings.
	 * @param s1
	 * @param s2
	 * @return
	 */
	private static int computeOverlap(Set<ByteString> s1, Set<ByteString> s2) {
		int overlap = 0;
		for (ByteString r : s1) {
			if (s2.contains(r))
				++overlap;
		}
		return overlap;
	}

	/**
	 * It loads the contents of the given files into the in-memory database.
	 * @param files
	 * @throws IOException
	 */
	public void load(File... files) throws IOException {
		load(Arrays.asList(files));
	}

	/**
	 * It loads the contents of the given files into the in-memory database.
	 * @param files
	 * @throws IOException
	 */
	public void load(List<File> files) throws IOException {
		if (files.isEmpty())
			return;
		
		long size = size();
		long time = System.currentTimeMillis();
		long memory = Runtime.getRuntime().freeMemory();
		Announce.doing("Loading files");
		final int[] running = new int[1];
		for (final File file : files) {
			running[0]++;
			new Thread() {
				public void run() {
					try {
						synchronized (Announce.blanks) {
							Announce.message("Starting " + file.getName());
						}
						load(file, null);
					} catch (Exception e) {
						e.printStackTrace();
					}
					synchronized (Announce.blanks) {
						Announce.message("Finished " + file.getName()
								+ ", still running: " + (running[0] - 1));
						synchronized (running) {
							if (--running[0] == 0) {
								running.notify();
							}
						}
					}
				}
			}.start();
		}

		try {
			synchronized (running) {
				running.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Announce.done("Loaded " + (size() - size) + " facts in "
				+ NumberFormatter.formatMS(System.currentTimeMillis() - time)
				+ " using "
				+ ((Runtime.getRuntime().freeMemory() - memory) / 1000000)
				+ " MB");
	}
	
	/**
	 * It loads the contents of the given file into the in-memory database.
	 * @param f
	 * @param message
	 * @throws IOException
	 */
	protected void load(File f, String message)
			throws IOException {
		long size = size();
		if (f.isDirectory()) {
			long time = System.currentTimeMillis();
			Announce.doing("Loading files in " + f.getName());
			for (File file : f.listFiles())
				load(file);
			Announce.done("Loaded "
					+ (size() - size)
					+ " facts in "
					+ NumberFormatter.formatMS(System.currentTimeMillis()
							- time));
		}
		for (String line : new FileLines(f, "UTF-8", message)) {
			if (line.endsWith("."))
				line = Char17.cutLast(line);
                        String[] split = line.trim().split(delimiter);
			if (split.length == 3) {
				add(split[0].trim(), split[1].trim(), split[2].trim());
			} else if (split.length == 4)
				add(split[1].trim(), split[2].trim(), split[3].trim());
			/*String[] split = line.trim().split(">" + delimiter);
			if (split.length == 3) {
				add(split[0].trim() +">", split[1].trim()+">", split[2].trim());
			} else if (split.length == 4)
				add(split[0].trim() +">", split[1].trim()+">", split[2].trim()+">");*/
		}

		if (message != null)
			Announce.message("     Loaded", (size() - size), "facts");
	}

	/** Loads the files */
	public void loadSequential(List<File> files)
			throws IOException {
		long size = size();
		long time = System.currentTimeMillis();
		long memory = Runtime.getRuntime().freeMemory();
		Announce.doing("Loading files");
		for (File file : files)
			load(file);
		Announce.done("Loaded " + (size() - size) + " facts in "
				+ NumberFormatter.formatMS(System.currentTimeMillis() - time)
				+ " using "
				+ ((Runtime.getRuntime().freeMemory() - memory) / 1000000)
				+ " MB");
	}

	// ---------------------------------------------------------------------------
	// Functionality
	// ---------------------------------------------------------------------------

	/**
	 * It returns the harmonic functionality of a relation, as defined in the PARIS paper 
	 * https://www.lri.fr/~cr/Publications_Master_2013/Brigitte_Safar/p157_fabianmsuchanek_vldb2011.pdf
	 **/
	public double functionality(ByteString relation) {
		if (relation.equals(EQUALSbs)) {
			return 1.0;
		} else {
			if (relation2subject2object.containsKey(relation)) {
				return ((double) relation2subject2object.get(relation).size() / relationSize
					.get(relation));
			} else {
				throw new IllegalArgumentException("The relation " + relation + " was not found in the KB");
			}
		}
	}

	/**
	 * It returns the harmonic functionality of a relation, as defined in the PARIS paper 
	 * https://www.lri.fr/~cr/Publications_Master_2013/Brigitte_Safar/p157_fabianmsuchanek_vldb2011.pdf
	 **/
	public double functionality(CharSequence relation) {
		return (functionality(compress(relation)));
	}

	/**
	 * Returns the harmonic inverse functionality, as defined in the PARIS paper
	 * https://www.lri.fr/~cr/Publications_Master_2013/Brigitte_Safar/p157_fabianmsuchanek_vldb2011.pdf
	 */
	public double inverseFunctionality(ByteString relation) {
		if (relation.equals(EQUALSbs)) {
			return 1.0;
		} else {
			if (relation2object2subject.containsKey(relation)) {
				return ((double) relation2object2subject.get(relation).size() / relationSize
						.get(relation));
			} else {
				throw new IllegalArgumentException("The relation " + relation + " was not found in the KB");
			}
		}
	}

	/**
	 * Returns the harmonic inverse functionality, as defined in the PARIS paper
	 * https://www.lri.fr/~cr/Publications_Master_2013/Brigitte_Safar/p157_fabianmsuchanek_vldb2011.pdf
	 */
	public double inverseFunctionality(CharSequence relation) {
		return (inverseFunctionality(compress(relation)));
	}

	/**
	 * Functionality of a relation given the position.
	 * @param relation
	 * @param col Subject = functionality, Object = Inverse functionality
	 * @return
	 */
	public double colFunctionality(ByteString relation, Column col) {
		if (col == Column.Subject)
			return functionality(relation);
		else if (col == Column.Object)
			return inverseFunctionality(relation);
		else
			return -1.0;
	}
	
	/**
	 * Determines whether a relation is functional, i.e., its harmonic functionality
	 * is greater than its inverse harmonic functionality.
	 * @param relation
	 * @return
	 */
	public boolean isFunctional(ByteString relation) {
		return functionality(relation) >= inverseFunctionality(relation);
	}
	
	/**
	 * It returns the functionality or the inverse functionality of a relation.
	 * @param relation
	 * @param inversed If true, the method returns the inverse functionality, otherwise
	 * it returns the standard functionality.
	 * @return
	 */
	public double functionality(ByteString relation, boolean inversed) {
		if (inversed)
			return inverseFunctionality(relation);
		else 
			return functionality(relation);
	}
	
	/**
	 * It returns the functionality or the inverse functionality of a relation.
	 * @param inversed If true, the method returns the functionality of a relation,
	 * otherwise it returns the inverse functionality.
	 * @return
	 */
	public double inverseFunctionality(ByteString relation, boolean inversed) {
		if (inversed)
			return functionality(relation);
		else 
			return inverseFunctionality(relation);
	}
	
	/**
	 * It returns the variance of the number of objects associated to a single 
	 * subject.
	 * @param relation
	 * @return
	 */
	public double inverseVariance(ByteString relation) {
		// First calculate the average
		double avg = 1.0 / inverseFunctionality(relation);
		// Now compute the formula
		double sum = 0.0;
		Map<ByteString, IntHashMap<ByteString>> targetMap = 
				relation2object2subject.get(relation);
		for (ByteString object : targetMap.keySet()) {
			sum += Math.pow(avg - targetMap.get(object).size(), 2.0);
		}
		
		return sum / targetMap.size();
	}

	/**
	 * It returns the variance of the number of subjects associated to a single
	 * object.
	 * @param relation
	 * @return
	 */
	public double variance(ByteString relation) {
		// First calculate the average
		double avg = 1.0 / functionality(relation);
		// Now compute the formula
		double sum = 0.0;
		Map<ByteString, IntHashMap<ByteString>> targetMap = 
				relation2subject2object.get(relation);
		for (ByteString subject : targetMap.keySet()) {
			sum += Math.pow(avg - targetMap.get(subject).size(), 2.0);
		}
		
		return sum / targetMap.size();
	}

	// ---------------------------------------------------------------------------
	// Statistics
	// ---------------------------------------------------------------------------

	/**
	 * Given two relations, it returns the number of entities in common (the overlap) between 
	 * two of their columns
	 * @param relation1
	 * @param relation2
	 * @param overlap 0 = Subject-Subject, 2 = Subject-Object, 4 = Object-Object
	 * @return
	 */
	public int overlap(ByteString relation1, ByteString relation2, int overlap) {
		switch (overlap) {
		case SUBJECT2SUBJECT:
			return subject2subjectOverlap.get(relation1).get(relation2);
		case SUBJECT2OBJECT:
			return subject2objectOverlap.get(relation1).get(relation2);
		case OBJECT2OBJECT:
			return object2objectOverlap.get(relation1).get(relation2);
		default:
			throw new IllegalArgumentException(
					"The argument map must be either 0 (subject-subject overlap), "
					+ "2 (subject-object overlap) or 4 (object to object overlap)");
		}
	}

	/**
	 * It returns the number of facts of a relation in the KB.
	 * @param relation
	 * @return
	 */
	public int relationSize(ByteString relation) {
		return relationSize.get(relation);
	}

	/**
	 * It returns the number of distinct instance of one of the arguments (columns)
	 * of a relation.
	 * @param relation
	 * @param column. Subject or Object
	 * @return
	 */
	public int relationColumnSize(ByteString relation, Column column) {
		switch (column) {
		case Subject:
			return relation2subject2object.get(relation).size();
		case Object:
			return relation2object2subject.get(relation).size();
		default:
			throw new IllegalArgumentException(
					"Argument column can be 0 (subject) or 2 (object)");
		}
	}

	// ---------------------------------------------------------------------------
	// Single triple selections
	// ---------------------------------------------------------------------------

	/**
	 * It returns TRUE if the 0th component is different from the 2n, 3rd, 4th, etc. 
	 **/
	public static boolean differentFrom(CharSequence... triple) {
		return (differentFrom(triple(triple)));
	}

	/**
	 * It returns TRUE if the 0th component is different from the 2n, 3rd, 4th, etc. 
	 **/
	public static boolean differentFrom(ByteString... triple) {
		if (!triple[1].equals(DIFFERENTFROMbs))
			throw new IllegalArgumentException(
					"DifferentFrom can only be called with a differentFrom predicate: "
							+ toString(triple));
		for (int i = 2; i < triple.length; i++) {
			if (triple[0].equals(triple[i]))
				return (false);
		}
		return (true);
	}

	/**
	 * It returns TRUE if the 0th component is different from the 2n, 3rd, 4th, etc. 
	 **/
	public static boolean equalTo(CharSequence... triple) {
		return (equalTo(triple(triple)));
	}

	/**
	 * It returns TRUE if the 0th component is different from the 2n, 3rd, 4th, etc. 
	 **/
	public static boolean equalTo(ByteString... triple) {
		if (!triple[1].equals(EQUALSbs))
			throw new IllegalArgumentException(
					"equalTo can only be called with a equals predicate: "
							+ toString(triple));
		for (int i = 2; i < triple.length; i++) {
			if (!triple[0].equals(triple[i]))
				return (false);
		}
		return (true);
	}

	/**
	 * It returns the third level values of a map given the keys for the first
	 * and second level.  
	 * @param key1
	 * @param key2
	 * @param map A 3-level map 
	 * @return
	 * */
	protected IntHashMap<ByteString> get(
			Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> map,
			ByteString key1, ByteString key2) {
		Map<ByteString, IntHashMap<ByteString>> m = map.get(key1);
		if (m == null)
			return (new IntHashMap<>());
		IntHashMap<ByteString> r = m.get(key2);
		if (r == null)
			return (new IntHashMap<>());
		return (r);
	}
	
	/**
	 * It returns the second and third level values of a map given the keys for the first
	 * level.  
	 * @param key
	 * @param map A 3-level map 
	 * @return
	 * */
	protected Map<ByteString, IntHashMap<ByteString>> get(
			Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> map,
			ByteString key) {
		Map<ByteString, IntHashMap<ByteString>> m = map.get(key);
		if (m == null)
			return (Collections.emptyMap());
		else
			return (m);
	}

	/**
	 * Returns the results of the triple pattern query, if it contains exactly 1
	 * variable
	 */
	public Set<ByteString> resultsOneVariable(CharSequence... triple) {
		if (numVariables(triple) != 1)
			throw new IllegalArgumentException(
					"Triple should contain exactly one variable: "
							+ toString(triple));
		return (resultsOneVariable(triple(triple)));
	}

	/**
	 * Returns the results of the triple pattern query, if it contains exactly 1
	 * variable
	 */
	public IntHashMap<ByteString> resultsOneVariable(ByteString... triple) {
		if (triple[1].equals(TRANSITIVETYPEbs)) {
			if (isVariable(triple[0])) {
				/*
				 * Return all the entities in subclasses of triple[2]
				 */
				IntHashMap<ByteString> result = new IntHashMap<>();
				for (ByteString subtype : Schema.getAllSubTypes(this, triple[2])) {
					result.addAll(get(relation2object2subject, Schema.typeRelationBS, subtype));
				}
				return result;
			} else { // assert(isVariable(triple[2]));
				/*
				 * Return all the super-classes of an entity
				 */
				return Schema.getAllTypesForEntity(this, triple[0]);
			}
		}
		if (triple[1].equals(DIFFERENTFROMbs))
			throw new IllegalArgumentException("Cannot query differentFrom: "
					+ toString(triple));
		if (triple[1].equals(EQUALSbs)) {
			IntHashMap<ByteString> result = new IntHashMap<>();
			if (isVariable(triple[0]))
				result.add(triple[2]);
			else
				result.add(triple[0]);
			return (result);
		}		
		if (triple[1].equals(EXISTSbs)) {
			if (isVariable(triple[0])) 
				return (new IntHashMap<ByteString>(get(subject2relation2object, triple[2]).keySet()));
			else 
				return (new IntHashMap<ByteString>(get(relation2subject2object, triple[0]).keySet()));
		}		
		if (triple[1].equals(EXISTSINVbs)) {
			if (isVariable(triple[0])) 
				return (new IntHashMap<ByteString>(get(object2relation2subject, triple[2]).keySet()));
			else 
				return (new IntHashMap<ByteString>(get(relation2object2subject, triple[0]).keySet()));
		}
		
		if (triple[1].equals(NOTEXISTSbs)) {
			Set<ByteString> values = new LinkedHashSet<>();
			if (isVariable(triple[0])) {
				values.addAll(relationSize);
				values.removeAll(get(subject2relation2object, triple[2]).keySet());
			} else {
				values.addAll(subjectSize);
				values.removeAll(get(relation2subject2object, triple[0]).keySet());
			}			
			return new IntHashMap<>(values);
		}
		
		if (triple[1].equals(NOTEXISTSINVbs)) {
			Set<ByteString> values = new LinkedHashSet<>();
			if (isVariable(triple[0])) {
				values.addAll(relationSize);
				values.removeAll(get(object2relation2subject, triple[2]).keySet());
			} else { 
				values.addAll(objectSize);
				values.removeAll(get(relation2object2subject, triple[0]).keySet());
			}
			return new IntHashMap<>(values);
		}
		
		Pair<ByteString, Integer> cardinalityRelation = 
				parseCardinalityRelation(triple[1]);
		if (cardinalityRelation != null) {
			if (isVariable(triple[2])) {
				throw new UnsupportedOperationException("The relation " + triple[1] 
						+ " does not support variables in the object position");
			}
			IntHashMap<ByteString> results = new IntHashMap<>();
			if (cardinalityRelation.first.equals(hasNumberOfValuesEqualsBS) 
					|| cardinalityRelation.first.equals(hasNumberOfValuesEqualsInvBS)) {
				Map<ByteString, IntHashMap<ByteString>> map = 
						cardinalityRelation.first.equals(hasNumberOfValuesEqualsBS) ?
								get(relation2subject2object, triple[2]) :
								get(relation2object2subject, triple[2]);			
				if (cardinalityRelation.second.intValue() > 0) {
					for (ByteString entity : map.keySet()) {
						if (map.get(entity).size() == cardinalityRelation.second.intValue())
							results.increase(entity);
					}
				} else {
					IntHashMap<ByteString> set = cardinalityRelation.first.equals(hasNumberOfValuesEqualsBS) ?
							subjectSize : objectSize;
					for (ByteString entity : set) {
						if (!map.containsKey(entity)) {
							results.increase(entity);
						}
					}
				}
			} else if (cardinalityRelation.first.equals(hasNumberOfValuesGreaterThanBS)
					|| cardinalityRelation.first.equals(hasNumberOfValuesGreaterThanInvBS)) {
				Map<ByteString, IntHashMap<ByteString>> map = 
						cardinalityRelation.first.equals(hasNumberOfValuesGreaterThanBS) ?
								get(relation2subject2object, triple[2]) :
								get(relation2object2subject, triple[2]);	
				
				if (cardinalityRelation.second.intValue() == 0) {
					return new IntHashMap<>(map.keySet());
				}
					
				for (ByteString entity : map.keySet()) {
					if (map.get(entity).size() > cardinalityRelation.second.intValue())
						results.increase(entity);
				}
			} else {
				IntHashMap<ByteString> set = cardinalityRelation.first.equals(hasNumberOfValuesSmallerThanBS) ?
						subjectSize : objectSize;
				Map<ByteString, IntHashMap<ByteString>> map = 
						cardinalityRelation.first.equals(hasNumberOfValuesSmallerThanBS) ?
								get(relation2subject2object, triple[2]) :
								get(relation2object2subject, triple[2]);	
				for (ByteString entity : set) {
					if (map.containsKey(entity)) {
						if (cardinalityRelation.second.intValue() == 1)
							continue;
						
						if (map.get(entity).size() < cardinalityRelation.second.intValue())
							results.increase(entity);	
					} else {
						results.increase(entity);
					}
				}
			}
			
			return results;
		}
		
		if (isVariable(triple[0]))
			return (get(relation2object2subject, triple[1], triple[2]));
		if (isVariable(triple[1]))
			return (get(object2subject2relation, triple[2], triple[0]));
		return (get(subject2relation2object, triple[0], triple[1]));
	}

	/**
	 * It returns TRUE if the database contains this fact (no variables). If the fact
	 * containst meta-relations (e.g. differentFrom, equals, exists), it returns TRUE
	 * if the expression evaluates to TRUE.
	 * @param fact A triple without variables, e.g., [Barack_Obama, wasBornIn, Hawaii]
	 **/
	public boolean contains(CharSequence... fact) {
		if (numVariables(fact) != 0)
			throw new IllegalArgumentException(
					"Triple should not contain a variable: " + toString(fact));
		return (contains(triple(fact)));
	}

	/**
	 * It returns TRUE if the database contains this fact (no variables). If the fact
	 * containst meta-relations (e.g. differentFrom, equals, exists), it returns TRUE
	 * if the expression evaluates to TRUE.
	 * @param fact A triple without variables, e.g., [Barack_Obama, wasBornIn, Hawaii]
	 **/
	protected boolean contains(ByteString... fact) {
		if (fact[1] == TRANSITIVETYPEbs) {
			for (ByteString type : get(this.subject2relation2object, fact[0], Schema.typeRelationBS)) {
				if (Schema.isTransitiveSuperType(this, fact[2], type)) {
					return true;
				}
			}
			return false;
		}
		if (fact[1] == DIFFERENTFROMbs)
			return (differentFrom(fact));
		if (fact[1] == EQUALSbs)
			return (equalTo(fact));
		if (fact[1] == EXISTSbs) 
			return (get(relation2subject2object, fact[0])
					.containsKey(fact[2]));
		if (fact[1] == EXISTSINVbs)
			return (get(relation2object2subject, fact[0])
					.containsKey(fact[2]));
		if (fact[1] == NOTEXISTSbs) 
			return (!get(relation2subject2object, fact[0])
					.containsKey(fact[2]));
		if (fact[1] == NOTEXISTSINVbs)
			return (!get(relation2object2subject, fact[0])
					.containsKey(fact[2]));
		Pair<ByteString, Integer> cardinalityRelation = 
				parseCardinalityRelation(fact[1]);
		if (cardinalityRelation != null) {
			if (cardinalityRelation.first.equals(hasNumberOfValuesEqualsBS)) {
				if (get(subject2relation2object, fact[0]).containsKey(fact[2])) {
					return get(subject2relation2object, fact[0]).get(fact[2]).size() 
							== cardinalityRelation.second.intValue();
				} else {
					return cardinalityRelation.second == 0;
				}
			} else if (cardinalityRelation.first.equals(hasNumberOfValuesEqualsInvBS)) {
				if (get(object2relation2subject, fact[0]).containsKey(fact[2])) {
					return get(object2relation2subject, fact[0]).get(fact[2]).size() 
					== cardinalityRelation.second.intValue();
				} else {
					return cardinalityRelation.second == 0;
				}
			} else if (cardinalityRelation.first.equals(hasNumberOfValuesGreaterThanBS)) {
				if (get(subject2relation2object, fact[0]).containsKey(fact[2])) {
					return get(subject2relation2object, fact[0]).get(fact[2]).size() 
							> cardinalityRelation.second.intValue();
				} else {
					return false;
				}								
			} else if (cardinalityRelation.first.equals(hasNumberOfValuesGreaterThanInvBS)) {
				if (get(object2relation2subject, fact[0]).containsKey(fact[2])) {
					return get(object2relation2subject, fact[0]).get(fact[2]).size() 
							> cardinalityRelation.second.intValue();
				} else { 
					return false;
				}
			} else if (cardinalityRelation.first.equals(hasNumberOfValuesSmallerThanBS)) {
				if (get(subject2relation2object, fact[0]).containsKey(fact[2])) {
					return get(subject2relation2object, fact[0]).get(fact[2]).size() 
								< cardinalityRelation.second.intValue();
				} else {
					return true;
				}
			} else if (cardinalityRelation.first.equals(hasNumberOfValuesSmallerThanInvBS)) {
				if (get(object2relation2subject, fact[0]).containsKey(fact[2])) {
					return get(object2relation2subject, fact[0]).get(fact[2]).size() 
							< cardinalityRelation.second.intValue();
				} else {
					return true;
				}
			}
		}
		return (get(subject2relation2object, fact[0], fact[1])
				.contains(fact[2]));
	}

	private static final String cardinalityRelationsRegex = "(" + 
			hasNumberOfValuesEquals + "|" + hasNumberOfValuesGreaterThan + "|" + hasNumberOfValuesSmallerThan + "|" +
			hasNumberOfValuesEqualsInv + "|" + hasNumberOfValuesGreaterThanInv + "|" + hasNumberOfValuesSmallerThanInv + 
			")([0-9]+)";
	
	private static final Pattern cardinalityRelationsRegexPattern = 
			Pattern.compile(cardinalityRelationsRegex);

	/**
	 * Determines whether the relation has 
	 * @param byteString
	 * @return
	 */
	public static Pair<ByteString, Integer> parseCardinalityRelation(ByteString relation) {
		Matcher m = cardinalityRelationsRegexPattern.matcher(relation);
		if (m.matches()) {
			Pair<ByteString, Integer> pair = new Pair<>(ByteString.of(m.group(1)), 
					Integer.parseInt(m.group(2)));
			if (pair.first.equals(hasNumberOfValuesSmallerThanBS) || 
					pair.first.equals(hasNumberOfValuesSmallerThanInvBS)) {
				if (pair.second.intValue() == 0) throw new IllegalArgumentException(relation + " is undefined.");
			}
			return pair;
		} else {
			return null;
		}
	}

	/**
	 * Returns the results of a triple query pattern with two variables as a map
	 * of first value to set of second values.
	 */
	public Map<ByteString, IntHashMap<ByteString>> resultsTwoVariables(
			CharSequence var1, CharSequence var2, CharSequence[] triple) {
		if (varpos(var1, triple) == -1 || varpos(var2, triple) == -1
				|| var1.equals(var2) || numVariables(triple) != 2)
			throw new IllegalArgumentException(
					"Triple should contain the two variables " + var1 + ", "
							+ var2 + ": " + toString(triple));
		return (resultsTwoVariables(compress(var1), compress(var2),
				triple(triple)));
	}
	
	/**
	 * Returns the results of a triple query pattern with two variables as a map
	 * of first value to set of second values.
	 */
	public Map<ByteString, IntHashMap<ByteString>> resultsTwoVariables(
			int pos1, int pos2, CharSequence[] triple) {
		if (!isVariable(triple[pos1]) || !isVariable(triple[pos2])
				|| numVariables(triple) != 2 || pos1 == pos2)
			throw new IllegalArgumentException(
					"Triple should contain 2 variables, one at " + pos1
							+ " and one at " + pos2 + ": " + toString(triple));
		return (resultsTwoVariables(pos1, pos2, triple(triple)));
	}	
	
	/**
	 * Returns the results of a triple query pattern with two variables as a map
	 * of first value to set of second values
	 */
	public Map<ByteString, IntHashMap<ByteString>> resultsTwoVariables(
			ByteString var1, ByteString var2, ByteString[] triple) {
		int varPos1 = varpos(var1, triple);
		int varPos2 = varpos(var2, triple);
		return resultsTwoVariables(varPos1, varPos2, triple);
	}
	

	/**
	 * Returns the results of a triple query pattern with two variables as a map
	 * of first value to set of second values
	 */
	public Map<ByteString, IntHashMap<ByteString>> resultsTwoVariables(
			int pos1, int pos2, ByteString[] triple) {
		if (triple[1].equals(TRANSITIVETYPEbs)) {
			Map<ByteString, IntHashMap<ByteString>> result = new LinkedHashMap<>();
			switch(pos1) {
			case 0:
				/*
				 * Return a map from all entities to all super-classes
				 */
				for (ByteString entity : get(relation2subject2object, Schema.typeRelationBS).keySet()) {
					result.put(entity, Schema.getAllTypesForEntity(this, entity));
				}
				return result;
			case 2:
				/*
				 * Return a map from all types to all entities of sub-classes
				 */
				Set<ByteString> allTypes = (Schema.isTaxonomyMaterialized()) ? Schema.getAllDefinedTypes() : get(relation2object2subject, Schema.typeRelationBS).keySet();
				for (ByteString type : allTypes) {
					result.put(type, resultsOneVariable(triple(ByteString.of("?s"), TRANSITIVETYPEbs, type)));
				}
				return result;
			case 1:
			default:
				throw new IllegalArgumentException("The argument at position " + pos1 
						+ " should be a variable");
			}
		}
		if (triple[1].equals(DIFFERENTFROMbs))
			throw new IllegalArgumentException(
					"Cannot query with differentFrom: " + toString(triple));
		if (triple[1].equals(EQUALSbs)) {
			Map<ByteString, IntHashMap<ByteString>> result = new HashMap<>();
			for (ByteString entity : subject2object2relation.keySet()) {
				IntHashMap<ByteString> innerResult = new IntHashMap<>();
				innerResult.add(entity);
				result.put(entity, innerResult);
			}
			return (result);
		}
		if (triple[1].equals(EXISTSbs) || triple[1].equals(EXISTSINVbs)) {
			Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> map = 
					triple[1].equals(EXISTSbs) ? relation2subject2object
							: relation2object2subject;
			Map<ByteString, IntHashMap<ByteString>> result = new HashMap<>();
			for (ByteString relation : map.keySet()) {
				IntHashMap<ByteString> innerResult = new IntHashMap<>();
				innerResult.addAll(map.get(relation).keySet());
				result.put(relation, innerResult);
			}
			return (result);
		}
		
		if (triple[1].equals(NOTEXISTSbs) || triple[1].equals(NOTEXISTSINVbs)) {
			Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> map = 
					triple[1].equals(NOTEXISTSbs) ? relation2subject2object
							: relation2object2subject;
			Map<ByteString, IntHashMap<ByteString>> result = new HashMap<>();
			for (ByteString relation : map.keySet()) {
				Set<ByteString> uMap = triple[1].equals(NOTEXISTSbs) ? 
						new LinkedHashSet<>(subjectSize) : new LinkedHashSet<>(objectSize);				
				uMap.removeAll(map.get(relation).keySet());	
				result.put(relation, new IntHashMap<>(uMap));
			}	
			return result;
		}
		
		switch (pos1) {
		case 0:
			switch (pos2) {
			case 1:
				return (get(object2subject2relation, triple[2]));
			case 2:
				return (get(relation2subject2object, triple[1]));
			}
			break;
		case 1:
			switch (pos2) {
			case 0:
				return get(object2relation2subject, triple[2]);
			case 2:
				return get(subject2relation2object, triple[0]);
			}
			break;
		case 2:
			switch (pos2) {
			case 0:
				return get(relation2object2subject, triple[1]);
			case 1:
				return get(subject2object2relation, triple[0]);
			}
			break;
		}
		throw new IllegalArgumentException(
				"Invalid combination of variables in " + toString(triple)
						+ " pos1 = " + pos1 + " pos2=" + pos2);
	}
	
	/**
	 * Returns the results of a triple query pattern with three variables as
	 * a nested map, firstValue : {secondValue : thirdValue}.
	 * @param var1
	 * @param var2
	 * @param var3
	 * @param triple
	 * @return
	 */
	public Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> resultsThreeVariables(
			ByteString var1, ByteString var2, ByteString var3,
			ByteString[] triple) {
		int varPos1 = varpos(var1, triple);
		int varPos2 = varpos(var2, triple);
		int varPos3 = varpos(var3, triple);
		
		return resultsThreeVariables(varPos1, varPos2, varPos3, triple);
	}

	/**
	 * Returns the results of a triple query pattern with three variables as
	 * a nested map, firstValue : {secondValue : thirdValue}.
	 * @param varPos1 Position of first variable in the triple pattern
	 * @param varPos2 Position of the second variable in the triple pattern
	 * @param varPos3 Position of the third variable in the triple pattern
	 * @param triple
	 * @return
	 */
	private Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> resultsThreeVariables(
			int varPos1, int varPos2, int varPos3, ByteString[] triple) {
		switch (varPos1) {
		case 0 :
			switch (varPos2) {
			case 1 :
				if (varPos3 == 2)
					return subject2relation2object;
				else
					throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
							+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);
			case 2 :
				if (varPos3 == 1)
					return subject2object2relation;
				else
					throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
							+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);
			default:
				throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
						+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);
			}
		case 1 :
			switch (varPos2) {
			case 0 :
				if (varPos3 == 2)
					return relation2subject2object;
				else 
					throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
								+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);
			case 2 :
				if (varPos3 == 0) 
					return relation2object2subject;
				else
					throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
							+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);					
			default:
				throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
						+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);
			}
		case 2 :
			switch (varPos2) {
			case 0 :
				if (varPos3 == 1)
					return object2subject2relation;
				else
					throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
							+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);
			case 1 :
				if (varPos3 == 0)
					return object2relation2subject;
				else
					throw new IllegalArgumentException("Invalid combination of variables in " + toString(triple)
							+ " pos1 = " + varPos1 + " pos2=" + varPos2 + " pos3=" + varPos3);
			}
		}
		
		return null;
	}

	/** 
	 * Returns the number of distinct results of the triple pattern query 
	 * with 1 variable.
	 * @throws OperationNotSupportedException 
	 **/
	public long countOneVariable(ByteString... triple)  {
		if (triple[1].equals(DIFFERENTFROMbs))
			return (Long.MAX_VALUE);
		if (triple[1].equals(EQUALSbs))
			return (1);
		if (triple[1].equals(EXISTSbs)) {
			if (isVariable(triple[2]))
				return get(relation2subject2object, triple[0]).size();
			else 
				return get(subject2relation2object, triple[2]).size();
		}
		if (triple[1].equals(EXISTSINVbs)) {
			if (isVariable(triple[2]))
				return get(relation2object2subject, triple[0]).size();
			else
				return get(object2relation2subject, triple[2]).size();
		}
		if (triple[1].equals(NOTEXISTSbs)) {
			if (isVariable(triple[2]))
				return subjectSize.size() - get(relation2subject2object, triple[0]).size();
			else
				return relationSize.size() - get(subject2relation2object, triple[2]).size();
		}
		if (triple[1].equals(NOTEXISTSINVbs)) {
			if (isVariable(triple[2]))
				return objectSize.size() - get(relation2object2subject, triple[0]).size();
			else
				return relationSize.size() - get(object2relation2subject, triple[2]).size();
		}
		
		Pair<ByteString, Integer> compositeRelation = parseCardinalityRelation(triple[1]);				
		if (compositeRelation != null) {			
			if (compositeRelation.first.equals(hasNumberOfValuesEqualsBS)
					|| compositeRelation.first.equals(hasNumberOfValuesEqualsInv)) {
				
				Map<ByteString, IntHashMap<ByteString>> map = 
						compositeRelation.first.equals(hasNumberOfValuesEqualsBS) ?
						get(this.relation2subject2object, triple[2]) :
						get(this.relation2object2subject, triple[2]);				
				long count = 0;
				if (compositeRelation.second.intValue() > 0) {
					for (ByteString s : map.keySet()) {
						if (map.get(s).size() == compositeRelation.second.intValue())
							++count; 
					}
				} else {
					IntHashMap<ByteString> set =
					compositeRelation.first.equals(hasNumberOfValuesEqualsBS) ? 
							subjectSize : objectSize;
					for (ByteString s : set) {
						if (!map.containsKey(s)) {
							++count;
						}
					}
				}
				return count;
			} else if (compositeRelation.first.equals(hasNumberOfValuesGreaterThanBS)
					|| compositeRelation.first.equals(hasNumberOfValuesGreaterThanInvBS)) {
				// If it is 0, just return the size of the map
				Map<ByteString, IntHashMap<ByteString>> map = 
						compositeRelation.first.equals(hasNumberOfValuesGreaterThanBS) ?
						get(this.relation2subject2object, triple[2]) :
						get(this.relation2object2subject, triple[2]);
				if (compositeRelation.second.intValue() == 0) {
					return map.size();
				} else {
					long count = 0;
					for (ByteString s : map.keySet()) {
						if (map.containsKey(s)) {
							if (map.get(s).size() > compositeRelation.second.intValue())
								++count; 
						}
					}
					return count;	
				}
			} else {
				Map<ByteString, IntHashMap<ByteString>> map = 
						compositeRelation.first.equals(hasNumberOfValuesSmallerThanBS) ?
						get(this.relation2subject2object, triple[2]) :
						get(this.relation2object2subject, triple[2]);
				long count = 0;
				IntHashMap<ByteString> set =
					compositeRelation.first.equals(hasNumberOfValuesSmallerThanBS) ? 
							subjectSize : objectSize;
				for (ByteString s : set) {					
					if (map.containsKey(s)) {						
						if (compositeRelation.second.intValue() == 1)
							continue;
						
						if (map.get(s).size() < compositeRelation.second.intValue())
							++count;
					} else {
						++count;
					}
				}
				return count;
			}
		}
			
		return (resultsOneVariable(triple).size());
	}

	/** 
	 * Returns the number of distinct results of the triple pattern query 
	 * with 2 variables. 
	 **/
	protected long countTwoVariables(ByteString... triple) {
		if (triple[1].equals(TRANSITIVETYPEbs)) {
			Map<ByteString, IntHashMap<ByteString>> resultTwoVars = 
					resultsTwoVariables(0, 2, triple);
			long count = 0;
			for (ByteString subject : resultTwoVars.keySet()) {
				count += resultTwoVars.get(subject).size();
			}
			return count;
		}
		if (triple[1].equals(DIFFERENTFROMbs))
			return (Long.MAX_VALUE);
		if (triple[1].equals(EQUALSbs))
			return (subject2relation2object.size());
		if (triple[1].equals(EXISTSbs)) {
			long count = 0;
			for (ByteString relation : relationSize) {
				count += get(relation2subject2object, relation).size();
			}
			return count;
		}
		if (triple[1].equals(EXISTSINVbs)) {
			long count = 0;
			for (ByteString relation : relationSize) {
				count += get(relation2object2subject, relation).size();
			}
			return count;
		}
		if (triple[1].equals(NOTEXISTSbs) || triple[1].equals(NOTEXISTSINVbs)) {			
			Map<ByteString, IntHashMap<ByteString>> resultTwoVars = 
					resultsTwoVariables(triple[0], triple[2], triple);
			long count = 0;
			for (ByteString relation : resultTwoVars.keySet()) {
				count += resultTwoVars.get(relation).size();
			}
			return count;
		}
		
		if (!isVariable(triple[0]))
			return (long) (subjectSize.get(triple[0], 0));
		if (!isVariable(triple[1])) {
			return (long) (relationSize.get(triple[1], 0));
		}
		return (long) (objectSize.get(triple[2], 0));
	}

	/** 
	 * Returns number of variable occurrences in a triple. Variables
	 * start with "?".
	 **/
	public static int numVariables(CharSequence... fact) {
		int counter = 0;
		for (int i = 0; i < fact.length; i++)
			if (isVariable(fact[i]))
				counter++;
		return (counter);
	}
	
	/**
	 * Determines whether a sequence of triples contains at least one variable
	 * @param query
	 * @return
	 */
	public static boolean containsVariables(List<ByteString[]> query) {
		// TODO Auto-generated method stub
		for (ByteString[] triple : query) {
			if (numVariables(triple) > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * It returns the number of instances (bindings) that satisfy this 
	 * triple pattern. 
	 * @param triple A triple pattern containing both constants and variables (no restrictions,
	 * it can contain only constants).
	 **/
	public long count(CharSequence... triple) {
		return (count(triple(triple)));
	}

	/** returns number of instances of this triple */
	protected long count(ByteString... triple) {
		switch (numVariables(triple)) {
		case 0:
			return (contains(triple) ? 1 : 0);
		case 1:
			return (countOneVariable(triple));
		case 2:
			return (countTwoVariables(triple));
		case 3:
			if (triple[1] == DIFFERENTFROMbs)
				return (Integer.MAX_VALUE);
			return (size());
		}
		return (-1);
	}

	// ---------------------------------------------------------------------------
	// Existence
	// ---------------------------------------------------------------------------

	/** 
	 * Remove a triple from a list of triples.
	 * @param pos Index in the list of the triple to be removed.
	 * @param triples Target list
	 **/
	protected static List<ByteString[]> remove(int pos, List<ByteString[]> triples) {
		if (pos == 0)
			return (triples.subList(1, triples.size()));
		if (pos == triples.size() - 1)
			return (triples.subList(0, triples.size() - 1));
		List<ByteString[]> result = new ArrayList<>(triples);
		result.remove(pos);
		return (result);
	}

	
	public static AtomicLong STAT_NUMBER_OF_CALL_TO_MRT = new AtomicLong();
	/**
	 * It returns the index of the most restrictive triple, -1 if most restrictive has count 0.
	 * The most restrictive triple is the one that contains the smallest number of satisfying
	 * instantiations.
	 **/
	protected int mostRestrictiveTriple(List<ByteString[]> triples) {
		STAT_NUMBER_OF_CALL_TO_MRT.incrementAndGet();
		int bestPos = -1;
		long count = Long.MAX_VALUE;
		for (int i = 0; i < triples.size(); i++) {
			long myCount = isSpecialAtom(triples.get(i)) ? Long.MAX_VALUE - 1 :
				count(triples.get(i));
			if (myCount >= count)
				continue;
			if (myCount == 0)
				return (-1);
			bestPos = i;
			count = myCount;
		}
		return (bestPos);
	}

	/**
	 * Returns true if the atom includes any of the special non-materialized relations.
	 * This types of relations are normally computed in the KB.
	 * @param byteStrings
	 * @return
	 */
	private boolean isSpecialAtom(ByteString[] atom) {
		return specialRelations.contains(atom[1]) ||
				parseCardinalityRelation(atom[1]) != null;
	}

	/**
	 * It returns the index of the most restrictive triple among those that contain the given variable, 
	 * -1 if most restrictive has count 0. The most restrictive triple is the one that contains the smallest 
	 * number of satisfying instantiations.
	 * @param triples
	 * @param variable Only triples containing this variable are considered.
	 **/
	protected int mostRestrictiveTriple(List<ByteString[]> triples,
			ByteString variable) {
		int bestPos = -1;
		long count = Long.MAX_VALUE;
		for (int i = 0; i < triples.size(); i++) {
			if (varpos(variable, triples.get(i)) != -1) {
				long myCount = count(triples.get(i));
				if (myCount >= count)
					continue;
				if (myCount == 0)
					return (-1);
				bestPos = i;
				count = myCount;
			}
		}
		return (bestPos);
	}

	/**
	 * It returns the index of the most restrictive triple among those that contain the given variables, 
	 * -1 if most restrictive has count 0. The most restrictive triple is the one that contains the smallest 
	 * number of satisfying instantiations.
	 * @param triples
	 * @param var1 
	 * @param var2 
	 **/
	protected int mostRestrictiveTriple(List<ByteString[]> triples,
			ByteString var1, ByteString var2) {
		int bestPos = -1;
		long count = Long.MAX_VALUE;
		for (int i = 0; i < triples.size(); i++) {
			ByteString[] triple = triples.get(i);
			if (contains(var1, triple) || contains(var2, triple)) {
				long myCount = count(triple);
				if (myCount >= count)
					continue;
				if (myCount == 0)
					return (-1);
				bestPos = i;
				count = myCount;
			}
		}
		return (bestPos);
	}

	/**
	 * Returns TRUE if the triple pattern contains the given variable.
	 * @param var
	 * @param triple
	 * @return
	 */
	private boolean contains(ByteString var, ByteString[] triple) {
		return triple[0].equals(var) || triple[1].equals(var)
				|| triple[2].equals(var);
	}

	/** 
	 * Returns the position of a variable in a triple.
	 * @param var
	 * @param triple
	 **/
	public static int varpos(ByteString var, ByteString[] triple) {
		for (int i = 0; i < triple.length; i++) {
			if (var.equals(triple[i]))
				return (i);
		}
		return (-1);
	}

	/** 
	 * Returns the position of a variable in a triple.
	 * @param var
	 * @param triple
	 **/
	public static int varpos(CharSequence var, CharSequence[] triple) {
		for (int i = 0; i < triple.length; i++) {
			if (var.equals(triple[i]))
				return (i);
		}
		return (-1);
	}

	/**
	 * Returns the position of the first variable in the pattern
	 * @param fact
	 * @return
	 */
	public static int firstVariablePos(ByteString... fact) {
		for (int i = 0; i < fact.length; i++)
			if (isVariable(fact[i]))
				return (i);
		return (-1);
	}

	/**
	 * Returns the position of the second variable in the pattern
	 * @param fact
	 * @return
	 */
	public static int secondVariablePos(ByteString... fact) {
		for (int i = firstVariablePos(fact) + 1; i < fact.length; i++)
			if (isVariable(fact[i]))
				return (i);
		return (-1);
	}

	/**
	 * It returns TRUE if there exists one instantiation that satisfies
	 * the query
	 * @param triples
	 * @return
	 */
	public boolean exists(List<CharSequence[]> triples) {
		return (existsBS1(triples(triples)));
	}

	/**
	 * It returns TRUE if there exists one instantiation that satisfies
	 * the query
	 * @param triples
	 * @return
	 */
	public boolean existsBS1(List<ByteString[]> triples) {
		if (triples.isEmpty())
			return (false);
		if (triples.size() == 1)
			return (count(triples.get(0)) != 0);
		int bestPos = mostRestrictiveTriple(triples);
		if (bestPos == -1)
			return (false);
		ByteString[] best = triples.get(bestPos);

		switch (numVariables(best)) {
		case 0:
			if (!contains(best))
				return (false);
			return (existsBS1(remove(bestPos, triples)));
		case 1:
			int firstVarIdx = firstVariablePos(best);
			if (firstVarIdx == -1) {
				System.out.println("[DEBUG] Problem with query "
						+ KB.toString(triples));
			}
			try (Instantiator insty = new Instantiator(
					remove(bestPos, triples), best[firstVarIdx])) {
				for (ByteString inst : resultsOneVariable(best)) {
					if (existsBS1(insty.instantiate(inst)))
						return (true);
				}
			}
			return (false);
		case 2:
			int firstVar = firstVariablePos(best);
			int secondVar = secondVariablePos(best);
			Map<ByteString, IntHashMap<ByteString>> instantiations = resultsTwoVariables(
					firstVar, secondVar, best);
			List<ByteString[]> otherTriples = remove(bestPos, triples);
			try (Instantiator insty1 = new Instantiator(otherTriples,
					best[firstVar]);
					Instantiator insty2 = new Instantiator(otherTriples,
							best[secondVar])) {
				for (ByteString val1 : instantiations.keySet()) {
					insty1.instantiate(val1);
					for (ByteString val2 : instantiations.get(val1)) {
						if (existsBS1(insty2.instantiate(val2)))
							return (true);
					}
				}
			}
			return (false);
		case 3:
		default:
			return (size() != 0);
		}
	}

	// ---------------------------------------------------------------------------
	// Count Distinct
	// ---------------------------------------------------------------------------

	/** 
	 * It returns the number of instantiations of variable that fulfill a certain 
	 * list of triple patterns.
	 * @param variable Projection variable
	 * @param query The list of triple patterns 
	 **/
	public long countDistinct(CharSequence variable, List<CharSequence[]> query) {
		return (countDistinct(compress(variable), triples(query)));
	}

	/** returns the number of instances that fulfill a certain condition */
	public long countDistinct(ByteString variable, List<ByteString[]> query) {
                return (long) (selectDistinct(variable, query).size());
	}

	// ---------------------------------------------------------------------------
	// Selection
	// ---------------------------------------------------------------------------

	/** returns the instances that fulfill a certain condition */
	public Set<ByteString> selectDistinct(CharSequence variable,
			List<CharSequence[]> query) {
		return (selectDistinct(compress(variable), triples(query)));
	}

	/** returns the instances that fulfill a certain condition */
	public Set<ByteString> selectDistinct(ByteString variable,
			List<ByteString[]> query) {
		// Only one triple
		if (query.size() == 1) {
			ByteString[] triple = query.get(0);
			switch (numVariables(triple)) {
			case 0:
				return (Collections.emptySet());
			case 1:
				return (resultsOneVariable(triple));
			case 2:
				int firstVar = firstVariablePos(triple);
				int secondVar = secondVariablePos(triple);
				if (firstVar == -1 || secondVar == -1) {
					System.out.println("[DEBUG] Problem with query "
							+ KB.toString(query));
				}
				if (triple[firstVar].equals(variable))
					return (resultsTwoVariables(firstVar, secondVar, triple)
							.keySet());
				else
					return (resultsTwoVariables(secondVar, firstVar, triple)
							.keySet());
			default:
				switch (Arrays.asList(query.get(0)).indexOf(variable)) {
				case 0:
					return (subjectSize);
				case 1:
					return (relationSize);
				case 2:
					return (objectSize);
				}
			}
			throw new RuntimeException("Very weird: SELECT " + variable
					+ " WHERE " + toString(query.get(0)));
		}

		int bestPos = mostRestrictiveTriple(query);
		IntHashMap<ByteString> result = new IntHashMap<>();
		if (bestPos == -1)
			return (result);
		ByteString[] best = query.get(bestPos);

		// If the variable is in the most restrictive triple
		if (Arrays.asList(best).indexOf(variable) != -1) {
			switch (numVariables(best)) {
			case 1 :
				try (Instantiator insty = new Instantiator(remove(bestPos,
						query), variable)) {
					for (ByteString inst : resultsOneVariable(best)) {
						if (existsBS1(insty.instantiate(inst)))
							result.add(inst);
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Map<ByteString, IntHashMap<ByteString>> instantiations = best[firstVar]
						.equals(variable) ? resultsTwoVariables(firstVar,
						secondVar, best) : resultsTwoVariables(secondVar,
						firstVar, best);
				try (Instantiator insty = new Instantiator(query, variable)) {
					for (ByteString val : instantiations.keySet()) {
						if (existsBS1(insty.instantiate(val)))
							result.add(val);
					}
				}
				break;
			case 3:
			default:
				try (Instantiator insty = new Instantiator(remove(bestPos, query), variable)) {
					int varPos = varpos(variable, best);
					ByteString var1, var2, var3;
					switch (varPos) {
					case 0 :
						var1 = best[0];
						var2 = best[1];
						var3 = best[2];
						break;
					case 1 :
						var1 = best[1];
						var2 = best[0];
						var3 = best[2];
						break;
					case 2 : default :
						var1 = best[2];
						var2 = best[0];
						var3 = best[1];
						break;							
					}

					for (ByteString inst : resultsThreeVariables(var1, var2, var3, best).keySet()) {
						if (existsBS1(insty.instantiate(inst)))
							result.add(inst);
					}
				}
				break;
			}
			return (result);
		}

		// If the variable is not in the most restrictive triple...
		List<ByteString[]> others = remove(bestPos, query);
		switch (numVariables(best)) {
		case 0:
			return (selectDistinct(variable, others));
		case 1:
			ByteString var = best[firstVariablePos(best)];
			try (Instantiator insty = new Instantiator(others, var)) {
				for (ByteString inst : resultsOneVariable(best)) {
					result.addAll(selectDistinct(variable,
							insty.instantiate(inst)));
				}
			}
			break;
		case 2:
			int firstVar = firstVariablePos(best);
			int secondVar = secondVariablePos(best);
			Map<ByteString, IntHashMap<ByteString>> instantiations = resultsTwoVariables(
					firstVar, secondVar, best);
			try (Instantiator insty1 = new Instantiator(others, best[firstVar]);
					Instantiator insty2 = new Instantiator(others,
							best[secondVar])) {
				for (ByteString val1 : instantiations.keySet()) {
					insty1.instantiate(val1);
					for (ByteString val2 : instantiations.get(val1)) {
						result.addAll(selectDistinct(variable,
								insty2.instantiate(val2)));
					}
				}
			}
			break;
		case 3:
		default:
			firstVar = firstVariablePos(best);
			secondVar = secondVariablePos(best);
			Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> map = 
					resultsThreeVariables(best[0], best[1], best[2], best);
			try (Instantiator insty1 = new Instantiator(others, best[0]);
					Instantiator insty2 = new Instantiator(others, best[1]);
						Instantiator insty3 = new Instantiator(others, best[2])) {
				for (ByteString val1 : map.keySet()) {
					insty1.instantiate(val1);
					instantiations = map.get(val1);
					for (ByteString val2 : instantiations.keySet()) {
						insty2.instantiate(val2);
						IntHashMap<ByteString> instantiations2 = instantiations.get(val2);
						for (ByteString val3 : instantiations2) {
							result.addAll(selectDistinct(variable, insty3.instantiate(val3)));
						}
					}
				}
			}
			break;

		}
		return (result);
	}
	
	// ---------------------------------------------------------------------------
	// Select distinct, two variables
	// ---------------------------------------------------------------------------

	/** Returns all (distinct) pairs of values that make the query true */
	public Map<ByteString, IntHashMap<ByteString>> selectDistinct(
			CharSequence var1, CharSequence var2, List<? extends CharSequence[]> query) {
		return (selectDistinct(compress(var1), compress(var2), triples(query)));
	}

	/** Returns all (distinct) pairs of values that make the query true */
	public Map<ByteString, IntHashMap<ByteString>> selectDistinct(
			ByteString var1, ByteString var2, List<ByteString[]> query) {
		if (query.isEmpty())
			return (Collections.emptyMap());
		if (query.size() == 1) {
			return (resultsTwoVariables(var1, var2, query.get(0)));
		}
		Map<ByteString, IntHashMap<ByteString>> result = new HashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (ByteString val1 : selectDistinct(var1, query)) {
				Set<ByteString> val2s = selectDistinct(var2,
						insty1.instantiate(val1));
				IntHashMap<ByteString> ihm = val2s instanceof IntHashMap<?> ? (IntHashMap<ByteString>) val2s
						: new IntHashMap<>(val2s);
				if (!val2s.isEmpty())
					result.put(val1, ihm);
			}
		}
		return (result);
	}
	
	// ---------------------------------------------------------------------------
	// Select distinct, more than 2 variables
	// ---------------------------------------------------------------------------

	/** Return all triplets of values that make the query true **/
	public Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> selectDistinct(
			CharSequence var1, CharSequence var2, CharSequence var3,
			List<? extends CharSequence[]> query) {
		return selectDistinct(compress(var1), compress(var2), compress(var3), triples(query));
	}
	
	public Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> selectDistinct(
			ByteString var1, ByteString var2, ByteString var3,
			List<ByteString[]> query) {
		if (query.isEmpty()) {
			return Collections.emptyMap();
		}
		
		if (query.size() == 1) {
			ByteString[] first = query.get(0);
			int numVariables = numVariables(first);
			if (numVariables == 3) {
				return resultsThreeVariables(var1, var2, var3, first);
			} else {
				throw new UnsupportedOperationException("Selection over"
						+ " variables not occuring in the query is not supported");
			}
		}
		
		Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> result = 
				new HashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (ByteString val1 : selectDistinct(var1, query)) {
				insty1.instantiate(val1);
				Map<ByteString, IntHashMap<ByteString>> tail = selectDistinct(var2, var3, query);
				if (!tail.isEmpty()) {
					result.put(val1, tail);
				}
			}
		}
		return result;
	}
	/**
	 * Turn a result map of 2 levels into a map of 3 levels.
	 */
	private Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> fixResultMap(
			Map<ByteString, IntHashMap<ByteString>> resultsTwoVars, int fixLevel, ByteString constant) {
		Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> extendedMap = new
				HashMap<ByteString, Map<ByteString, IntHashMap<ByteString>>>();
		switch (fixLevel) {
		case 1:
			extendedMap.put(constant, resultsTwoVars);
			break;
		case 2 :
			for (ByteString val : resultsTwoVars.keySet()) {
				Map<ByteString, IntHashMap<ByteString>> newMap = 
						new HashMap<ByteString, IntHashMap<ByteString>>();
				newMap.put(constant, resultsTwoVars.get(val));
				extendedMap.put(val, newMap);
			}	
		case 3 : default:
			IntHashMap<ByteString> newMap = new IntHashMap<ByteString>();
			newMap.add(constant);
			for (ByteString val1 : resultsTwoVars.keySet()) {
				Map<ByteString, IntHashMap<ByteString>> intermediateMap = 
						new HashMap<ByteString, IntHashMap<ByteString>>();
				for (ByteString val2 : resultsTwoVars.get(val1)) {
					intermediateMap.put(val2, newMap);
				}
				extendedMap.put(val1, intermediateMap);
			}
			break;
		}
		return extendedMap;
	}

	public Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> selectDistinct(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4,
			List<? extends CharSequence[]> query) {
		return selectDistinct(compress(var1), compress(var2), compress(var3), compress(var4), triples(query));
	}
	
	public Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> selectDistinct(
			ByteString var1, ByteString var2, ByteString var3, ByteString var4,
			List<ByteString[]> query) {
		if (query.size() < 2) {
			throw new IllegalArgumentException("The query must have at least 2 atoms");
		}
		
		Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> result = 
				new HashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (ByteString val1 : selectDistinct(var1, query)) {
				insty1.instantiate(val1);
				Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> tail = 
						selectDistinct(var2, var3, var4, query);
				if (!tail.isEmpty()) {
					result.put(val1, tail);
				}
			}
		}
		
		return result;
	}
	
	public Map<ByteString, Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>>> selectDistinct(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4, CharSequence var5,
			List<? extends CharSequence[]> query) {
		return selectDistinct(compress(var1), compress(var2), compress(var3), compress(var4), compress(var5), triples(query));
	}
	
	public Map<ByteString, Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>>> selectDistinct(
			ByteString var1, ByteString var2, ByteString var3, ByteString var4, ByteString var5,
			List<ByteString[]> query) {
		if (query.size() < 2) {
			throw new IllegalArgumentException("The query must have at least 2 atoms");
		}
		
		Map<ByteString, Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>>> result = 
				new HashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (ByteString val1 : selectDistinct(var1, query)) {
				insty1.instantiate(val1);
				Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> tail = 
						selectDistinct(var2, var3, var4, var5, query);
				if (!tail.isEmpty()) {
					result.put(val1, tail);
				}
			}
		}
		
		return result;
	}

	// ---------------------------------------------------------------------------
	// Count single projection bindings
	// ---------------------------------------------------------------------------

	/**
	 * Maps each value of the variable to the number of distinct values of the
	 * projection variable
	 */
	public IntHashMap<ByteString> frequentBindingsOf(CharSequence variable,
			CharSequence projectionVariable, List<CharSequence[]> query) {
		return (frequentBindingsOf(compress(variable),
				compress(projectionVariable), triples(query)));
	}

	/**
	 * For each instantiation of variable, it returns the number of different
	 * instances of projectionVariable that satisfy the query.
	 * 
	 * @return IntHashMap A map of the form {string : frequency}
	 **/
	public IntHashMap<ByteString> frequentBindingsOf(ByteString variable,
			ByteString projectionVariable, List<ByteString[]> query) {
		// If only one triple
		if (query.size() == 1) {
			ByteString[] triple = query.get(0);
			int varPos = varpos(variable, triple);
			int projPos = varpos(projectionVariable, triple);
			if (varPos == -1 || projPos == -1)
				throw new IllegalArgumentException(
						"Query should contain at least two variables: "
								+ toString(triple));
			if (numVariables(triple) == 2)
				return (new IntHashMap<ByteString>(resultsTwoVariables(varPos,
						projPos, triple)));
			// Three variables (only supported if varpos==2 and projPos==0)
			if (projPos != 0)
				throw new UnsupportedOperationException(
						"frequentBindingsOf on most general triple is only possible "
						+ "with projection variable in position 1: "
								+ toString(query));

			// Two variables
			IntHashMap<ByteString> res = new IntHashMap<>();
			if (varPos == projPos) {
				try (Instantiator insty = new Instantiator(query,
						triple[projPos])) {
					for (ByteString inst : resultsOneVariable(triple)) {
						res.add(selectDistinct(variable,
								insty.instantiate(inst)));
					}
				}
				return res;
			}

			for (ByteString predicate : relationSize.keys()) {
				triple[1] = predicate;
				res.add(predicate, resultsTwoVariables(0, 2, triple).size());
			}
			triple[1] = variable;
			return (res);
		}

		// Find most restrictive triple
		int bestPos = mostRestrictiveTriple(query, projectionVariable, variable);
		IntHashMap<ByteString> result = new IntHashMap<>();
		if (bestPos == -1)
			return (result);
		ByteString[] best = query.get(bestPos);
		int varPos = varpos(variable, best);
		int projPos = varpos(projectionVariable, best);
		List<ByteString[]> other = remove(bestPos, query);

		// If the variable and the projection variable are in the most
		// restrictive triple
		if (varPos != -1 && projPos != -1) {
			switch (numVariables(best)) {
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Map<ByteString, IntHashMap<ByteString>> instantiations = best[firstVar]
						.equals(variable) ? resultsTwoVariables(firstVar,
						secondVar, best) : resultsTwoVariables(secondVar,
						firstVar, best);
				try (Instantiator insty1 = new Instantiator(other, variable);
						Instantiator insty2 = new Instantiator(other,
								projectionVariable)) {
					for (ByteString val1 : instantiations.keySet()) {
						insty1.instantiate(val1);
						for (ByteString val2 : instantiations.get(val1)) {
							if (existsBS1(insty2.instantiate(val2)))
								result.increase(val1);
						}
					}
				}
				break;
			case 3:
			default:
				throw new UnsupportedOperationException(
						"3 variables in the variable triple are not yet supported: FREQBINDINGS "
								+ variable + " WHERE " + toString(query));
			}
			return (result);
		}

		// If the variable is in the most restrictive triple
		if (varPos != -1) {
			switch (numVariables(best)) {
			case 1:
				try (Instantiator insty = new Instantiator(other, variable)) {
					for (ByteString inst : resultsOneVariable(best)) {
						result.add(
								inst,
								selectDistinct(projectionVariable,
										insty.instantiate(inst)).size());
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Map<ByteString, IntHashMap<ByteString>> instantiations = best[firstVar]
						.equals(variable) ? resultsTwoVariables(firstVar,
						secondVar, best) : resultsTwoVariables(secondVar,
						firstVar, best);
				try (Instantiator insty1 = new Instantiator(query, variable)) {
					for (ByteString val1 : instantiations.keySet()) {
						result.add(
								val1,
								selectDistinct(projectionVariable,
										insty1.instantiate(val1)).size());
					}
				}
				break;
			case 3:
			default:
				throw new UnsupportedOperationException(
						"3 variables in the variable triple are not yet supported: FREQBINDINGS "
								+ variable + " WHERE " + toString(query));
			}
			return (result);
		}

		// Default case
		if (projPos != -1) {
			switch (numVariables(best)) {
			case 1:
				try (Instantiator insty = new Instantiator(other,
						projectionVariable)) {
					for (ByteString inst : resultsOneVariable(best)) {
						result.add(selectDistinct(variable,
								insty.instantiate(inst)));
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Map<ByteString, IntHashMap<ByteString>> instantiations = best[firstVar]
						.equals(projectionVariable) ? resultsTwoVariables(
						firstVar, secondVar, best) : resultsTwoVariables(
						secondVar, firstVar, best);
				try (Instantiator insty1 = new Instantiator(query,
						projectionVariable)) {
					for (ByteString val1 : instantiations.keySet()) {
						result.add(selectDistinct(variable,
								insty1.instantiate(val1)));
					}
				}
				break;
			case 3:
			default:
				throw new UnsupportedOperationException(
						"3 variables in the projection triple are not yet supported: FREQBINDINGS "
								+ variable + " WHERE " + toString(query));
			}
			return (result);
		}

		return result;
	}

	// ---------------------------------------------------------------------------
	// Count Projection Bindings
	// ---------------------------------------------------------------------------

	/**
	 * Counts, for each binding of the variable at position pos, the number of
	 * instantiations of the triple
	 */
	protected IntHashMap<ByteString> countBindings(int pos,
			ByteString... triple) {
		switch (numVariables(triple)) {
		case 1:
			return (new IntHashMap<ByteString>(resultsOneVariable(triple)));
		case 2:
			int pos2 = -1;
			switch (pos) {
			case 0: pos2 = (isVariable(triple[2])) ? 2 : 1; break; // We want the most frequent subjects
			case 1: pos2 = (isVariable(triple[2])) ? 2 : 0; break; // We want the most frequent predicates
			case 2: pos2 = (isVariable(triple[1])) ? 1 : 0; break; // we want the most frequent objects
			}
			return (new IntHashMap<ByteString>(resultsTwoVariables(pos, pos2, triple)));
		case 3:
			return (pos == 0 ? subjectSize : pos == 1 ? relationSize
					: objectSize);
		default:
			throw new InvalidParameterException(
					"Triple should contain at least 1 variable: "
							+ toString(triple));
		}
	}

	/**
	 * Counts for each binding of the variable at pos how many instances of the
	 * projection triple exist in the query
	 */
	protected IntHashMap<ByteString> countProjectionBindings(int pos,
			ByteString[] projectionTriple, List<ByteString[]> otherTriples) {
		if (!isVariable(projectionTriple[pos]))
			throw new IllegalArgumentException("Position " + pos + " in "
					+ toString(projectionTriple) + " must be a variable");
		IntHashMap<ByteString> result = new IntHashMap<>();
		switch (numVariables(projectionTriple)) {
		case 1:
			try (Instantiator insty = new Instantiator(otherTriples,
					projectionTriple[pos])) {
				for (ByteString inst : resultsOneVariable(projectionTriple)) {
					if (existsBS1(insty.instantiate(inst)))
						result.increase(inst);
				}
			}
			break;
		case 2:
			int firstVar = firstVariablePos(projectionTriple);
			int secondVar = secondVariablePos(projectionTriple);
			Map<ByteString, IntHashMap<ByteString>> instantiations = resultsTwoVariables(
					firstVar, secondVar, projectionTriple);
			try (Instantiator insty1 = new Instantiator(otherTriples,
					projectionTriple[firstVar]);
					Instantiator insty2 = new Instantiator(otherTriples,
							projectionTriple[secondVar])) {
				for (ByteString val1 : instantiations.keySet()) {
					insty1.instantiate(val1);
					for (ByteString val2 : instantiations.get(val1)) {
						if (existsBS1(insty2.instantiate(val2)))
							result.increase(firstVar == pos ? val1 : val2);
					}
				}
			}
			break;
		case 3:
		default:
			throw new UnsupportedOperationException(
					"3 variables in the projection triple are not yet supported: "
							+ toString(projectionTriple) + ", "
							+ toString(otherTriples));
		}
		return (result);
	}

	/**
	 * For each instantiation of variable, it returns the number of instances of
	 * the projectionTriple satisfy the query. The projection triple can have
	 * either one or two variables.
	 * 
	 * @return IntHashMap A map of the form {string : frequency}
	 **/
	public IntHashMap<ByteString> countProjectionBindings(
			ByteString[] projectionTriple, List<ByteString[]> otherTriples,
			ByteString variable) {
		int pos = Arrays.asList(projectionTriple).indexOf(variable);

		// If the other triples are empty, count all bindings
		if (otherTriples.isEmpty()) {
			return (countBindings(pos, projectionTriple));
		}

		// If the variable appears in the projection triple,
		// use the other method
		if (pos != -1) {
			return (countProjectionBindings(pos, projectionTriple, otherTriples));
		}

		// Now let's iterate through all instantiations of the projectionTriple
		List<ByteString[]> wholeQuery = new ArrayList<ByteString[]>();
		wholeQuery.add(projectionTriple);
		wholeQuery.addAll(otherTriples);

		ByteString instVar = null;
		int posRestrictive = mostRestrictiveTriple(wholeQuery);
		ByteString[] mostRestrictive = posRestrictive != -1 ? wholeQuery
				.get(posRestrictive) : projectionTriple;
		IntHashMap<ByteString> result = new IntHashMap<>();
		int posInCommon = (mostRestrictive != projectionTriple) ? firstVariableInCommon(
				mostRestrictive, projectionTriple) : -1;
		int nHeadVars = numVariables(projectionTriple);

		// Avoid ground facts in the projection triple
		if (mostRestrictive == projectionTriple || posInCommon == -1
				|| nHeadVars == 1) {
			switch (numVariables(projectionTriple)) {
			case 1:
				instVar = projectionTriple[firstVariablePos(projectionTriple)];
				try (Instantiator insty = new Instantiator(otherTriples,
						instVar)) {
					for (ByteString inst : resultsOneVariable(projectionTriple)) {
						result.add(selectDistinct(variable,
								insty.instantiate(inst)));
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(projectionTriple);
				int secondVar = secondVariablePos(projectionTriple);
				Map<ByteString, IntHashMap<ByteString>> instantiations = resultsTwoVariables(
						firstVar, secondVar, projectionTriple);
				try (Instantiator insty1 = new Instantiator(otherTriples,
						projectionTriple[firstVar]);
						Instantiator insty2 = new Instantiator(otherTriples,
								projectionTriple[secondVar])) {
					for (ByteString val1 : instantiations.keySet()) {
						insty1.instantiate(val1);
						for (ByteString val2 : instantiations.get(val1)) {
							result.add(selectDistinct(variable,
									insty2.instantiate(val2)));
						}
					}
				}
				break;
			case 3:
			default:
				throw new UnsupportedOperationException(
						"3 variables in the projection triple are not yet supported: "
								+ toString(projectionTriple) + ", "
								+ toString(otherTriples));
			}
		} else {
			List<ByteString[]> otherTriples2 = new ArrayList<ByteString[]>(
					wholeQuery);
			List<ByteString[]> projectionTripleList = new ArrayList<ByteString[]>(
					1);
			projectionTripleList.add(projectionTriple);
			otherTriples2.remove(projectionTriple);
			// Iterate over the most restrictive triple
			switch (numVariables(mostRestrictive)) {
			case 1:
				// Go for an improved plan, but remove the bound triple
				otherTriples2.remove(mostRestrictive);
				instVar = mostRestrictive[firstVariablePos(mostRestrictive)];
				try (Instantiator insty1 = new Instantiator(otherTriples2,
						instVar);
						Instantiator insty2 = new Instantiator(
								projectionTripleList, instVar)) {
					for (ByteString inst : resultsOneVariable(mostRestrictive)) {
						result.add(countProjectionBindings(
								insty2.instantiate(inst).get(0),
								insty1.instantiate(inst), variable));
					}
				}
				break;
			case 2:
				int projectionPosition = KB.varpos(
						mostRestrictive[posInCommon], projectionTriple);
				// If the projection triple has two variables, bind the common
				// variable without problems
				if (nHeadVars == 2) {
					try (Instantiator insty1 = new Instantiator(otherTriples2,
							mostRestrictive[posInCommon]);
							Instantiator insty3 = new Instantiator(
									projectionTripleList,
									projectionTriple[projectionPosition])) {
						IntHashMap<ByteString> instantiations = countBindings(
								posInCommon, mostRestrictive);
						for (ByteString b1 : instantiations) {
							result.add(countProjectionBindings(insty3
									.instantiate(b1).get(0), insty1
									.instantiate(b1), variable));
						}
					}
				} else if (nHeadVars == 1) {
					instVar = projectionTriple[firstVariablePos(projectionTriple)];
					try (Instantiator insty = new Instantiator(otherTriples,
							instVar)) {
						for (ByteString inst : resultsOneVariable(projectionTriple)) {
							result.add(selectDistinct(variable,
									insty.instantiate(inst)));
						}
					}
				}
				break;
			case 3:
			default:
				throw new UnsupportedOperationException(
						"3 variables in the most restrictive triple are not yet supported: "
								+ toString(mostRestrictive) + ", "
								+ toString(wholeQuery));
			}
		}

		return (result);
	}

	/**
	 * Returns the in the first atom, of the first variable that is found on the
	 * second atom.
	 * @param t1
	 * @param t2
	 * @return
	 */
	public int firstVariableInCommon(ByteString[] t1, ByteString[] t2) {
		for (int i = 0; i < t1.length; ++i) {
			if (KB.isVariable(t1[i]) && varpos(t1[i], t2) != -1)
				return i;
		}

		return -1;
	}

	/**
	 * Return the number of common variables between 2 atoms.
	 * @param a
	 * @param b
	 * @return
	 */
	public int numVarsInCommon(ByteString[] a, ByteString[] b) {
		int count = 0;
		for (int i = 0; i < a.length; ++i) {
			if (KB.isVariable(a[i]) && varpos(a[i], b) != -1)
				++count;
		}

		return count;
	}

	/**
	 * Counts, for each binding of the variable the number of instantiations of
	 * the projection triple
	 */
	public IntHashMap<ByteString> countProjectionBindings(
			CharSequence[] projectionTriple, List<CharSequence[]> query,
			CharSequence variable) {
		ByteString[] projection = triple(projectionTriple);
		List<ByteString[]> otherTriples = new ArrayList<>();
		for (CharSequence[] t : query) {
			ByteString[] triple = triple(t);
			if (!Arrays.equals(triple, projection))
				otherTriples.add(triple);
		}
		return (countProjectionBindings(projection, otherTriples,
				compress(variable)));
	}

	// ---------------------------------------------------------------------------
	// Count Projection
	// ---------------------------------------------------------------------------

	/**
	 * Counts the number of instances of the projection triple that exist in
	 * joins with the query
	 */
	public long countProjection(CharSequence[] projectionTriple,
			List<CharSequence[]> query) {
		ByteString[] projection = triple(projectionTriple);
		// Create "otherTriples"
		List<ByteString[]> otherTriples = new ArrayList<>();
		for (CharSequence[] t : query) {
			ByteString[] triple = triple(t);
			if (!Arrays.equals(triple, projection))
				otherTriples.add(triple);
		}
		return (countProjection(projection, otherTriples));
	}

	/**
	 * Counts the number of instances of the projection triple that exist in
	 * joins with the other triples
	 */
	public long countProjection(ByteString[] projectionTriple,
			List<ByteString[]> otherTriples) {
		if (otherTriples.isEmpty())
			return (count(projectionTriple));
		switch (numVariables(projectionTriple)) {
		case 0:
			return (count(projectionTriple));
		case 1:
			long counter = 0;
			ByteString variable = projectionTriple[firstVariablePos(projectionTriple)];
			try (Instantiator insty = new Instantiator(otherTriples, variable)) {
				for (ByteString inst : resultsOneVariable(projectionTriple)) {
					if (existsBS1(insty.instantiate(inst)))
						counter++;
				}
			}
			return (counter);
		case 2:
			counter = 0;
			int firstVar = firstVariablePos(projectionTriple);
			int secondVar = secondVariablePos(projectionTriple);
			Map<ByteString, IntHashMap<ByteString>> instantiations = resultsTwoVariables(
					firstVar, secondVar, projectionTriple);
			try (Instantiator insty1 = new Instantiator(otherTriples,
					projectionTriple[firstVar])) {
				for (ByteString val1 : instantiations.keySet()) {
					try (Instantiator insty2 = new Instantiator(
							insty1.instantiate(val1),
							projectionTriple[secondVar])) {
						for (ByteString val2 : instantiations.get(val1)) {
							if (existsBS1(insty2.instantiate(val2)))
								counter++;
						}
					}
				}
			}
			return (counter);
		case 3:
		default:
			throw new UnsupportedOperationException(
					"3 variables in the projection triple are not yet supported: "
							+ toString(projectionTriple) + ", "
							+ toString(otherTriples));
		}
	}

	// ---------------------------------------------------------------------------
	// Counting pairs
	// ---------------------------------------------------------------------------

	/** returns the number of distinct pairs (var1,var2) for the query */
	public long countPairs(CharSequence var1, CharSequence var2,
			List<ByteString[]> query) {
		return (countDistinctPairs(compress(var1), compress(var2), triples(query)));
	}

	/**
	 * Identifies queries containing the pattern: select ?a ?b where r(?a, ?c)
	 * r(?b, ?c) ... select ?a ?b where r(?c, ?a) r(?c, ?b) ...
	 * 
	 * @param query
	 * @return
	 */
	public int[] identifyHardQueryTypeI(List<ByteString[]> query) {
		if (query.size() < 2)
			return null;

		int lastIdx = query.size() - 1;
		for (int idx1 = 0; idx1 < lastIdx; ++idx1) {
			for (int idx2 = idx1 + 1; idx2 < query.size(); ++idx2) {
				ByteString[] t1, t2;
				t1 = query.get(idx1);
				t2 = query.get(idx2);

				// Not the same relation
				if (!t1[1].equals(t2[1]) || numVariables(t1) != 2
						|| numVariables(t2) != 2)
					return null;

				if (!t1[0].equals(t2[0]) && t1[2].equals(t2[2])) {
					return new int[] { 2, 0, idx1, idx2 };
				} else if (t1[0].equals(t2[0]) && !t1[2].equals(t2[2])) {
					return new int[] { 0, 2, idx1, idx2 };
				}
			}
		}
		return null;
	}

	/**
	 * Identifies queries containing the pattern: select ?a ?b where r(?a, ?c)
	 * r'(?b, ?c) ... select ?a ?b where r(?c, ?a) r'(?c, ?b) ...
	 * 
	 * @param query
	 * @return
	 */
	public int[] identifyHardQueryTypeII(List<ByteString[]> query) {
		if (query.size() < 2)
			return null;

		int lastIdx = query.size() - 1;
		for (int idx1 = 0; idx1 < lastIdx; ++idx1) {
			for (int idx2 = idx1 + 1; idx2 < query.size(); ++idx2) {
				ByteString[] t1, t2;
				t1 = query.get(idx1);
				t2 = query.get(idx2);

				// Not the same relation
				if (numVariables(t1) != 2 || numVariables(t2) != 2)
					continue;

				if (!t1[0].equals(t2[0]) && t1[2].equals(t2[2])) {
					return new int[] { 2, 0, idx1, idx2 };
				} else if (t1[0].equals(t2[0]) && !t1[2].equals(t2[2])) {
					return new int[] { 0, 2, idx1, idx2 };
				}
			}
		}

		return null;
	}

	public int[] identifyHardQueryTypeIII(List<ByteString[]> query) {
		if (query.size() < 2)
			return null;

		int lastIdx = query.size() - 1;
		for (int idx1 = 0; idx1 < lastIdx; ++idx1) {
			for (int idx2 = idx1 + 1; idx2 < query.size(); ++idx2) {
				ByteString[] t1, t2;
				t1 = query.get(idx1);
				t2 = query.get(idx2);

				// Not the same relation
				if (numVariables(t1) != 2 || numVariables(t2) != 2)
					continue;

				// Look for the common variable
				int varpos1 = KB.varpos(t1[0], t2);
				int varpos2 = KB.varpos(t1[2], t2);
				if ((varpos1 != -1 && varpos2 != -1)
						|| (varpos1 == -1 && varpos2 == -1))
					continue;

				if (varpos1 != -1) {
					return new int[] { varpos1, 0, idx1, idx2 };
				} else {
					return new int[] { varpos2, 2, idx1, idx2 };
				}
			}
		}

		return null;
	}

	public long countPairs(ByteString var1, ByteString var2,
			List<ByteString[]> query, int[] queryInfo) {

		long result = 0;
		// Approximate count
		ByteString joinVariable = query.get(queryInfo[2])[queryInfo[0]];
		ByteString targetVariable = query.get(queryInfo[3])[queryInfo[1]];
		ByteString targetRelation = query.get(queryInfo[2])[1];

		// Heuristic
		if (relationSize.get(targetRelation) < 50000)
			return countDistinctPairs(var1, var2, query);

		long duplicatesEstimate, duplicatesCard;
		double duplicatesFactor;

		List<ByteString[]> subquery = new ArrayList<ByteString[]>(query);
		subquery.remove(queryInfo[2]); // Remove one of the hard queries
		duplicatesCard = countDistinct(targetVariable, subquery);

		if (queryInfo[0] == 2) {
			duplicatesFactor = (1.0 / functionality(targetRelation)) - 1.0;
		} else {
			duplicatesFactor = (1.0 / inverseFunctionality(targetRelation)) - 1.0;
		}
		duplicatesEstimate = (int) Math.ceil(duplicatesCard * duplicatesFactor);

		try (Instantiator insty1 = new Instantiator(subquery, joinVariable)) {
			for (ByteString value : selectDistinct(joinVariable, subquery)) {
				result += (long) Math
						.ceil(Math.pow(
								countDistinct(targetVariable,
										insty1.instantiate(value)), 2));
			}
		}

		result -= duplicatesEstimate;
		return result;
	}

	public long countPairs(ByteString var1, ByteString var2,
			List<ByteString[]> query, int[] queryInfo,
			ByteString[] existentialTriple, int nonExistentialPosition) {
		long result = 0;
		long duplicatesEstimate, duplicatesCard;
		double duplicatesFactor;
		// Approximate count
		ByteString joinVariable = query.get(queryInfo[2])[queryInfo[0]];
		ByteString targetVariable = null;
		ByteString targetRelation = query.get(queryInfo[2])[1];
		List<ByteString[]> subquery = new ArrayList<ByteString[]>(query);

		// Heuristic
		if (relationSize.get(targetRelation) < 50000) {
			subquery.add(existentialTriple);
			result = countDistinctPairs(var1, var2, subquery);
			return result;
		}

		if (varpos(existentialTriple[nonExistentialPosition],
				query.get(queryInfo[2])) == -1) {
			subquery.remove(queryInfo[2]);
			targetVariable = query.get(queryInfo[3])[queryInfo[1]];
		} else {
			subquery.remove(queryInfo[3]);
			targetVariable = query.get(queryInfo[2])[queryInfo[1]];
		}

		subquery.add(existentialTriple);
		duplicatesCard = countDistinct(targetVariable, subquery);
		if (queryInfo[0] == 2) {
			duplicatesFactor = (1.0 / functionality(targetRelation)) - 1.0;
		} else {
			duplicatesFactor = (1.0 / inverseFunctionality(targetRelation)) - 1.0;
		}
		duplicatesEstimate = (int) Math.ceil(duplicatesCard * duplicatesFactor);

		try (Instantiator insty1 = new Instantiator(subquery, joinVariable)) {
			for (ByteString value : selectDistinct(joinVariable, subquery)) {
				result += (long) countDistinct(targetVariable,
						insty1.instantiate(value));
			}
		}

		result -= duplicatesEstimate;

		return result;
	}

    /**
     * returns the number of distinct pairs (var1,var2) for the query
     */
    public long countDistinctPairs(ByteString var1, ByteString var2,
            List<ByteString[]> query) {

        // Go for the standard plan
        long result = 0;

        try (Instantiator insty1 = new Instantiator(query, var1)) {
            Set<ByteString> bindings = selectDistinct(var1, query);
            for (ByteString val1 : bindings) {
                result += countDistinct(var2, insty1.instantiate(val1));
            }
        }

        return (result);
    }

	/** Can instantiate a variable in a query with a value */
	public static class Instantiator implements Closeable {
		List<ByteString[]> query;

		int[] positions;

		ByteString variable;
		
		private int atomSize;

		public Instantiator(List<ByteString[]> q, ByteString var, int atomSize) {
			this.atomSize = atomSize;
			positions = new int[q.size() * atomSize];
			int numPos = 0;
			query = q;
			variable = var;
			for (int i = 0; i < query.size(); i++) {
				for (int j = 0; j < query.get(i).length; j++) {
					if (query.get(i)[j].equals(variable))
						positions[numPos++] = i * atomSize + j;
				}
			}

			if (numPos < positions.length)
				positions[numPos] = -1;
		}
		
		public Instantiator(List<ByteString[]> q, ByteString var) {
			this(q, var, 3);
		}

		public List<ByteString[]> instantiate(ByteString value) {
			for (int i = 0; i < positions.length; i++) {
				if (positions[i] == -1)
					break;
				query.get(positions[i] / atomSize)[positions[i] % atomSize] = value;
			}
			return (query);
		}

		@Override
		public void close() {
			for (int i = 0; i < positions.length; i++) {
				if (positions[i] == -1)
					break;
				query.get(positions[i] / atomSize)[positions[i] % atomSize] = variable;
			}
		}
	}

	// ---------------------------------------------------------------------------
	// Creating Triples
	// ---------------------------------------------------------------------------

	/** ToString for a triple */
	public static <T> String toString(T[] s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length; i++)
			b.append(s[i]).append(" ");
		return (b.toString());
	}

	/** ToString for a query */
	public static String toString(List<ByteString[]> s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.size(); i++)
			b.append(toString(s.get(i))).append(" ");
		return (b.toString());
	}

	/** Compresses a string to an internal string */
	public static ByteString compress(CharSequence s) {
		if (s instanceof ByteString)
			return ((ByteString) s);
		String str = s.toString();
		int pos = str.indexOf("\"^^");
		if (pos != -1)
			str = str.substring(0, pos + 1);
		return (ByteString.of(str));
	}

	/** Makes a list of triples */
	public static List<ByteString[]> triples(ByteString[]... triples) {
		return (Arrays.asList(triples));
	}

	/** makes triples */
	@SuppressWarnings("unchecked")
	public static List<ByteString[]> triples(
			List<? extends CharSequence[]> triples) {
		if (iscompressed(triples))
			return ((List<ByteString[]>) triples);
		List<ByteString[]> t = new ArrayList<>();
		for (CharSequence[] c : triples)
			t.add(triple(c));
		return (t);
	}
	
	public static ByteString[] triple2Array(
			Triple<ByteString, ByteString, ByteString> t) {
		return new ByteString[] { t.first, t.second, t.third };
	}
	
	public static Triple<ByteString, ByteString, ByteString> array2Triple(
			ByteString[] triple) {
		return new Triple<ByteString, ByteString, ByteString>(
				triple[0], triple[1], triple[2]);
	}

	/** TRUE if this query is compressed */
	public static boolean iscompressed(List<? extends CharSequence[]> triples) {
		for (int i = 0; i < triples.size(); i++) {
			CharSequence[] t = triples.get(i);
			if (!(t instanceof ByteString[]))
				return (false);
		}
		return true;
	}

	/** Makes a triple */
	public static ByteString[] triple(ByteString... triple) {
		return (triple);
	}

	/** Makes a triple */
	public static ByteString[] triple(CharSequence... triple) {
		ByteString[] result = new ByteString[triple.length];
		for (int i = 0; i < triple.length; i++)
			result[i] = compress(triple[i]);
		return (result);
	}

	/** Pattern of a triple */
	public static final Pattern triplePattern = Pattern
			.compile("(\\w+)\\((\\??\\w+)\\s*,\\s*(\\??\\w+)\\)");

	/** Pattern of a triple */
	public static final Pattern amieTriplePattern = Pattern
			.compile("(\\??\\w+|<[-_\\w\\p{L}/:–'.\\(\\),]+>)\\s+(<?[-_\\w:\\.]+>?)\\s+(\"?[-_\\w\\s,'.–:]+\"?(@\\w+)?|\\??\\w+|<?[-_\\w\\p{L}/:–'.\\(\\)\\\"\\^,]+>?)");


	/** 
	 * Parses a triple of the form r(x,y) and turns into a triple
	 * of the form [x, r, y]
	 * @param s
	 * @return
	 **/
	public static ByteString[] triple(String s) {
		Matcher m = triplePattern.matcher(s);
		if (m.find())
			return (triple(m.group(2).trim(), m.group(1).trim(), m.group(3).trim()));
		m = amieTriplePattern.matcher(s);
		if (!m.find())
			return (triple(m.group(1).trim(), m.group(2).trim(), m.group(3).trim()));
		return (null);
	}

	/** 
	 * It parses a Datalog query with atoms of the form r(x,y) and turns into a list of
	 * AMIE triples of the form [x, r, y].
	 * @param s
	 * @return
	 **/
	public static ArrayList<ByteString[]> triples(String s) {
		Matcher m = triplePattern.matcher(s);
		ArrayList<ByteString[]> result = new ArrayList<>();
		while (m.find())
			result.add(triple(m.group(2).trim(), m.group(1).trim(), m.group(3).trim()));
		if (result.isEmpty()) {
			m = amieTriplePattern.matcher(s);
			while (m.find())
				result.add(triple(m.group(1).trim(), m.group(2).trim(), m.group(3).trim()));
		}
		return (result);
	}

	/**
	 * Parses a rule of the form triple &amp; triple &amp; ... =&gt; triple or triple :-
	 * triple &amp; triple &amp; ...
	 * @return A pair where the first element is the list of body atoms (left-hand side 
	 * of the rule) and the second element is triple pattern, the head of the rule.
	 */
	public static Pair<List<ByteString[]>, ByteString[]> rule(String s) {
		List<ByteString[]> triples = triples(s);
		if (triples.isEmpty())
			return null;
		if (s.contains(":-"))
			return (new Pair<>(triples.subList(1, triples.size()),
					triples.get(0)));
		if (s.contains("=>"))
			return (new Pair<>(triples.subList(0, triples.size() - 1),
					triples.get(triples.size() - 1)));
		return (null);
	}
	
	/**
	 * It returns all the bindings of the projection variable that match
	 * the antecedent but not the head.
	 * @param projectionVariable
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Set<ByteString> difference(CharSequence projectionVariable,
			List<? extends CharSequence[]> antecedent, CharSequence[] head) {
		List<CharSequence[]> headList = new ArrayList<>();
		headList.add(head);
		return difference(compress(projectionVariable), triples(antecedent), 
				triples(headList));
	}
	
	/**
	 * It returns all the bindings of the projection variable that match
	 * the antecedent but not the succedent.
	 * @param projectionVariable
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Set<ByteString> difference(ByteString projectionVariable,
			List<ByteString[]> antecedent, List<ByteString[]> head) {
		// TODO Auto-generated method stub
		Set<ByteString> bodyBindings = new HashSet<ByteString>(selectDistinct(
				projectionVariable, antecedent));
		Set<ByteString> headBindings = selectDistinct(projectionVariable, head);

		bodyBindings.removeAll(headBindings);
		return bodyBindings;
	}
	
	
	// ---------------------------------------------------------------------------
	// Difference with 2 variables
	// ---------------------------------------------------------------------------
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the atom 'head'.
	 * @param var1
	 * @param var2
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, IntHashMap<ByteString>> difference(CharSequence var1,
			CharSequence var2, List<? extends CharSequence[]> antecedent, CharSequence[] head) {
		return difference(compress(var1), compress(var2), triples(antecedent), triple(head));
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the atom 'head'.
	 * @param var1
	 * @param var2
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, IntHashMap<ByteString>> difference(ByteString var1,
			ByteString var2, List<ByteString[]> antecedent,
			ByteString[] head) {
		// Look for all bindings for the variables that appear on the antecedent
		// but not in the head
		List<ByteString[]> headList = new ArrayList<ByteString[]>(1);
		headList.add(head);
		return difference(var1, var2, antecedent, headList);
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the atom 'head'.
	 * @param var1
	 * @param var2
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, IntHashMap<ByteString>> differenceNoVarsInCommon(CharSequence var1,
			CharSequence var2, List<? extends CharSequence[]> antecedent,
			CharSequence[] head) {
		return differenceNoVarsInCommon(compress(var1), compress(var2), triples(antecedent), triple(head));
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the atom 'head'. Special case of the difference where the head atom does 
	 * not contain the projection variables.
	 * @param var1
	 * @param var2
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, IntHashMap<ByteString>> differenceNoVarsInCommon(ByteString var1,
			ByteString var2, List<ByteString[]> antecedent,
			ByteString[] head) {
		// Look for all bindings for the variables that appear on the antecedent
		// but not in the head
		List<ByteString[]> headList = new ArrayList<ByteString[]>(1);
		headList.add(head);
		List<ByteString[]> wholeQuery = new ArrayList<ByteString[]>(antecedent);
		wholeQuery.add(head);
		Map<ByteString, IntHashMap<ByteString>> results = new HashMap<ByteString, IntHashMap<ByteString>>();
		
		Map<ByteString, IntHashMap<ByteString>> antecedentBindings = 
				selectDistinct(var1, var2, antecedent);
		try(Instantiator insty1 = new Instantiator(wholeQuery, var1);
				Instantiator insty2 = new Instantiator(wholeQuery, var2)) {
			for (ByteString val1 : antecedentBindings.keySet()) {
				insty1.instantiate(val1);
				IntHashMap<ByteString> nestedValues = new IntHashMap<ByteString>();
				for(ByteString val2 : antecedentBindings.get(val1)) {
					insty2.instantiate(val2);
					if (!existsBS1(wholeQuery)) {
						nestedValues.add(val2);
					}
				}
				if (!nestedValues.isEmpty()) {
					results.put(val1, nestedValues);
				}
			}
		}
		
		return results;
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the second.
	 * @param var1
	 * @param var2
	 * @param antecedent
	 * @param headList
	 * @return
	 */
	public Map<ByteString, IntHashMap<ByteString>> difference(ByteString var1,
			ByteString var2, List<ByteString[]> antecedent,
			List<ByteString[]> headList) {
		Map<ByteString, IntHashMap<ByteString>> bodyBindings = selectDistinct(
				var1, var2, antecedent);
		Map<ByteString, IntHashMap<ByteString>> headBindings = selectDistinct(
				var1, var2, headList);
		Map<ByteString, IntHashMap<ByteString>> result = new HashMap<ByteString, IntHashMap<ByteString>>();

		Set<ByteString> keySet = bodyBindings.keySet();
		for (ByteString key : keySet) {
			if (!headBindings.containsKey(key)) {
				result.put(key, bodyBindings.get(key));
			} else {
				IntHashMap<ByteString> partialResult = new IntHashMap<ByteString>();
				for (ByteString value : bodyBindings.get(key)) {
					if (!headBindings.get(key).contains(value)) {
						partialResult.add(value);
					}
				}
				if (!partialResult.isEmpty())
					result.put(key, partialResult);
			}

		}

		return result;
	}
	
	// ---------------------------------------------------------------------------
	// Difference with 3 variables
	// ---------------------------------------------------------------------------
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the atom 'head'.
	 * @param var1
	 * @param var2
	 * @param var3
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> difference(
			CharSequence var1, CharSequence var2, CharSequence var3,
			List<? extends CharSequence[]> antecedent, CharSequence[] head) {
		return difference(compress(var1), compress(var2), compress(var3), 
				triples(antecedent), triple(head));
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the atom 'head'
	 * @param var1
	 * @param var2
	 * @param var3
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> difference(
			ByteString var1, ByteString var2, ByteString var3,
			List<ByteString[]> antecedent, ByteString[] head) {
		Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> results = null;
		
		List<ByteString[]> headList = new ArrayList<>(1);
		headList.add(head);
		
		Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> bodyBindings = null;
		Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> headBindings = null;
		
		if (numVariables(head) == 3) {
			results = new HashMap<ByteString, Map<ByteString,IntHashMap<ByteString>>>();
			bodyBindings = selectDistinct(var1, var2, var3, antecedent);
			headBindings = selectDistinct(var1, var2, var3, headList);
		
			for (ByteString val1 : bodyBindings.keySet()) {
				if (headBindings.containsKey(val1)) {
					// Check at the next level
					Map<ByteString, IntHashMap<ByteString>> tailBody = 
							bodyBindings.get(val1);
					Map<ByteString, IntHashMap<ByteString>> tailHead = 
							headBindings.get(val1);
					for (ByteString val2 : tailBody.keySet()) {
						if (tailHead.containsKey(val2)) {
							IntHashMap<ByteString> tailBody1 = tailBody.get(val2);
							IntHashMap<ByteString> headBody1 = tailHead.get(val2);
							for (ByteString val3 : tailBody1) {
								if (!headBody1.contains(val3)) {
									Map<ByteString, IntHashMap<ByteString>> secondLevel = 
											results.get(val1);
									if (secondLevel == null) {
										secondLevel = new HashMap<ByteString, IntHashMap<ByteString>>();
										results.put(val1, secondLevel);
									}
									
									IntHashMap<ByteString> thirdLevel = 
											secondLevel.get(val2);
									if (thirdLevel == null) {
										thirdLevel = new IntHashMap<ByteString>();
										secondLevel.put(val2, thirdLevel);
									}
									
									thirdLevel.add(val3);								
								}
							}
						} else {
							Map<ByteString, IntHashMap<ByteString>> secondLevel = 
									results.get(val1);
							if (secondLevel == null) {
								secondLevel = new HashMap<ByteString, IntHashMap<ByteString>>();
								results.put(val1, secondLevel);
							}
							secondLevel.put(val2, tailBody.get(val2));
						}
					}
				} else {
					// Add all the stuff associated to this subject
					results.put(val1, bodyBindings.get(val1));
				}
			}
		} else {
			Map<ByteString, IntHashMap<ByteString>> tmpResult = null;
			int fixLevel = -1;
			if (varpos(var1, head) == -1) {				
				tmpResult = difference(var2, var3, antecedent, head);
				fixLevel = 1;
			} else if (varpos(var2, head) == -1) {
				tmpResult = difference(var1, var3, antecedent, head);
				fixLevel = 2;
			} else if (varpos(var3, head) == -1) {
				tmpResult = difference(var1, var2, antecedent, head);
				fixLevel = 3;
			}
			
			ByteString constant = null;
			for (ByteString t : head) {
				if (!isVariable(t)) {
					constant = t;
					break;
				}
			}
			
			results = fixResultMap(tmpResult, fixLevel, constant);
		}
		
		return results;
	}
	
	
	// ---------------------------------------------------------------------------
	// Difference with 4 variables
	// ---------------------------------------------------------------------------
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the atom 'head'
	 * @param var1
	 * @param var2
	 * @param var3
	 * @param var4
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> difference(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4,
			List<? extends CharSequence[]> antecedent, CharSequence[] head) {
		return difference(compress(var1), compress(var2), compress(var3), compress(var4),
				triples(antecedent), triple(head));
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the second.
	 * @param var1
	 * @param var2
	 * @param antecedent
	 * @param head
	 * @param swap
	 * @return
	 */
	public Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> difference(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4,
			List<? extends CharSequence[]> antecedent, CharSequence[] head, boolean swap) {
		return difference(compress(var1), compress(var2), compress(var3), compress(var4),
				triples(antecedent), triple(head), swap);
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the second.
	 * @param var1
	 * @param var2
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> difference(
			ByteString var1, ByteString var2, ByteString var3, ByteString var4,
			List<ByteString[]> antecedent, ByteString[] head) {
		Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> results = new
				HashMap<ByteString, Map<ByteString, Map<ByteString,IntHashMap<ByteString>>>>();
		
		int headNumVars = numVariables(head);
		if (headNumVars == 3) {
			try (Instantiator insty = new Instantiator(antecedent, var1)) {
				for (ByteString val1 : selectDistinct(var1, antecedent)) {
					insty.instantiate(val1);
					Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> diff = 
							difference(var2, var3, var4, antecedent, head);
					if (!diff.isEmpty()) {
						results.put(val1, diff);
					}
				}
			}
		} else if (headNumVars == 2) {
			try (Instantiator insty1 = new Instantiator(antecedent, var1)) {
				try (Instantiator insty2 = new Instantiator(antecedent, var2)) {
					Map<ByteString, IntHashMap<ByteString>> resultsTwoVars =
							selectDistinct(var1, var2, antecedent);
					for (ByteString val1 : resultsTwoVars.keySet()) {
						insty1.instantiate(val1);
						Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> level1 =
								new HashMap<ByteString, Map<ByteString,IntHashMap<ByteString>>>();
						for (ByteString val2 : resultsTwoVars.get(val1)) {
							insty2.instantiate(val2);
							Map<ByteString, IntHashMap<ByteString>> diff = 
									difference(var3, var4, antecedent, head);
							if (!diff.isEmpty()) {
								level1.put(val2, diff);
							}
						}
						if (!level1.isEmpty()) {
							results.put(val1, level1);
						}
					}
				}
			}

		}
		
		return results;
	}
	

	/**
	 * It performs set difference for the case where the head contains 2 out of the 4 variables
	 * defined in the body. 
	 * @param var1 First variable, not occurring in the head
	 * @param var2 Second variable, not occuring in the head
	 * @param var3 First Variable occurring in both body and head
	 * @param var4 Second Variable occuring in both body and head
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> differenceNotVarsInCommon(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4,
			List<? extends CharSequence[]> antecedent, CharSequence[] head) {
		return differenceNotVarsInCommon(compress(var1), compress(var2), 
				compress(var3), compress(var4), triples(antecedent), triple(head));
	}
	
	/**
	 * It performs set difference for the case where the head contains 2 out of the 4 variables
	 * defined in the body. 
	 * @param var1 First variable, not occurring in the head
	 * @param var2 Second variable, not occurring in the head
	 * @param var3 First Variable occurring in both body and head
	 * @param var4 Second Variable occurring in both body and head
	 * @param antecedent
	 * @param head
	 * @return
	 */
	public Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> differenceNotVarsInCommon(
			ByteString var1, ByteString var2, ByteString var3, ByteString var4,
			List<ByteString[]> antecedent, ByteString[] head) {
		int headNumVars = numVariables(head);
		Map<ByteString, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>>> results = 
				new HashMap<ByteString, Map<ByteString,Map<ByteString,IntHashMap<ByteString>>>>();
		List<ByteString[]> wholeQuery = new ArrayList<ByteString[]>(antecedent);
		wholeQuery.add(head);
		
		if (headNumVars == 3) {
			try (Instantiator insty = new Instantiator(wholeQuery, var1)) {
				for (ByteString val1 : selectDistinct(var1, antecedent)) {
					insty.instantiate(val1);
					if (!existsBS1(wholeQuery)) {
						Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> diff = 
								selectDistinct(var2, var3, var4, antecedent);
						results.put(val1, diff);
					}
				}
			}
		} else if (headNumVars == 2) {
			try (Instantiator insty1 = new Instantiator(wholeQuery, var1)) {
				try (Instantiator insty2 = new Instantiator(wholeQuery, var2)) {
					Map<ByteString, IntHashMap<ByteString>> resultsTwoVars = selectDistinct(var1, var2, antecedent);
					for (ByteString val1 : resultsTwoVars.keySet()) {
						insty1.instantiate(val1);
						Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> level1 =
								new HashMap<ByteString, Map<ByteString,IntHashMap<ByteString>>>();
						for (ByteString val2 : resultsTwoVars.get(val1)) {
							insty2.instantiate(val2);
							if (!existsBS1(wholeQuery)) {
								Map<ByteString, IntHashMap<ByteString>> diff = 
										selectDistinct(var3, var4, antecedent);
								level1.put(val2, diff);
							}
						}
						if (!level1.isEmpty()) {
							results.put(val1, level1);
						}
					}
				}
			}

		}
		
		return results;
	}

	/**
	 * Counts the number of bindings in the given nested map.
	 * @param bindings
	 * @return
	 */
	public static long aggregate(
			Map<ByteString, IntHashMap<ByteString>> bindings) {
		long result = 0;

		for (ByteString binding : bindings.keySet()) {
			result += bindings.get(binding).size();
		}

		return result;
	}


	/**
	 * Get a collection with all the relations of the KB.
	 * @return
	 */
	public Collection<ByteString> getRelations() {
		return relationSize.decreasingKeys();
	}
	
	/**
	 * It returns all the entities that occur as subjects or objects
	 * in the KB.
	 * @param kb
	 * @return
	 */
	public Collection<ByteString> getAllEntities() {
		Collection<ByteString> result = new ArrayList<>();
		result.addAll(subjectSize);
		result.addAll(objectSize);
		return result;
	}
	
	/**
	 * It returns all the entities and the number of facts where they occur.
	 * @param kb
	 * @return
	 */
	public IntHashMap<ByteString> getEntitiesOccurrences() {
		IntHashMap<ByteString> result = new IntHashMap<>();
		result.addAll(subjectSize);
		result.addAll(objectSize);
		return result;
	}
	

	/**
	 * It returns the number of facts for each of the entities in the
	 * collection provided as argument.
	 * @param kb
	 * @return
	 */
	public IntHashMap<ByteString> getEntitiesOccurrences(Collection<ByteString> entities) {
		IntHashMap<ByteString> result = new IntHashMap<>();
		for (ByteString entity : entities) {
			if (subjectSize.contains(entity)) result.add(entity, subjectSize.get(entity));
			if (objectSize.contains(entity)) result.add(entity, objectSize.get(entity));
		}
		return result;
	}
	
	
	/**
	 * Return all the relations (and their sizes) that are bigger than the given
	 * threshold.
	 * @param threshold
	 * @return
	 */
	public IntHashMap<ByteString> getRelationsBiggerOrEqualThan(int threshold) {	
		IntHashMap<ByteString> relationsBiggerThan = new IntHashMap<ByteString>();
		for (ByteString relation : relation2subject2object.keySet()) {
			int size = 0;		
			Map<ByteString, IntHashMap<ByteString>> tail = 
					relation2subject2object.get(relation);
			for (ByteString subject : tail.keySet()) {
				size += tail.get(subject).size();
				if (size >= threshold) {
					relationsBiggerThan.put(relation, size);
				}
			}
		}
		
		return relationsBiggerThan;
	}
	
	/**
	 * Get a list of the relations of the KB.
	 * @return
	 */
	public List<ByteString> getRelationsList() {
		return relationSize.decreasingKeys();
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		int maxCount = 30;
		for (ByteString v1 : subject2relation2object.keySet()) {
			Map<ByteString, IntHashMap<ByteString>> tail = subject2relation2object.get(v1);
			for (ByteString v2 : tail.keySet()) {
				for (ByteString v3 : tail.get(v2)) {
					strBuilder.append(v1);
					strBuilder.append(delimiter);
					strBuilder.append(v2);
					strBuilder.append(delimiter);					
					strBuilder.append(v3);					
					strBuilder.append("\n");
					if (maxCount >= 30)
						break;
					++maxCount;
				}
			}
		}
		
		return strBuilder.toString();
	}
	
	// ---------------------------------------------------------------------------
	// Cardinality functions
	// ---------------------------------------------------------------------------

	/**
	 * Given a relation returns the maximal number of object values such that
	 * the right cumulative distribution of values is higher than threshold. For example given
	 * the histogram {1: 3, 2: 4, 3: 5} and the right cumulative distribution 
	 * {0: 12, 1: 9, 2: 5} for the number of nationalities of people
	 * maximalRightCumulativeCardinality(<isCitizenOf>, 5, 3) would return 2 as this is the 
	 * maximal entry in the cumulative distribution that is above the provided threshold (10)
	 * and that is smaller than 3, the given limit.
	 * @param relation
	 * @param threshold
	 * @param limit
	 * @return
	 */
	public int maximalRightCumulativeCardinality(ByteString relation, long threshold, int limit) {
		Map<ByteString, IntHashMap<ByteString>> map = get(relation2subject2object, relation);
		return maximalRightCumulativeCardinality(relation, threshold, map, limit);
	}
	
	/**
	 * Given a relation returns the maximal number of subject values such that
	 * the right cumulative distribution of values is higher than threshold. For example given
	 * the histogram {1: 3, 2: 4, 3: 5} and the right cumulative distribution 
	 * {0: 12, 1: 9, 2: 5} for the number of parents of people
	 * maximalCardinality(<hasChild>, 5, 3) would return 2 as this is the 
	 * maximal entry in the cumulative distribution that is above the provided threshold (10)
	 * and smaller than the given limit 3.
	 * @param relation
	 * @param threshold
	 * @param limit
	 * @return
	 */
	public int maximalRightCumulativeCardinalityInv(ByteString relation, long threshold, int limit) {
		Map<ByteString, IntHashMap<ByteString>> map = 
				get(relation2object2subject, relation);
		return maximalRightCumulativeCardinality(relation, threshold, map, limit);
	}
	
	private int maximalRightCumulativeCardinality(ByteString relation, long threshold, 
			Map<ByteString, IntHashMap<ByteString>> map, int iMaxThreshold) {
		IntHashMap<Integer> histogram = buildCumulativeHistogram(map);
		List<Integer> keys = histogram.decreasingKeys();
		Collections.sort(keys);
		int maxThreshold = histogram.get(iMaxThreshold);
		for (int keyidx = iMaxThreshold + 1; keyidx < keys.size(); ++keyidx) {
			int val = keys.get(keyidx);
			int iValue = histogram.get(val);
			if (iValue < maxThreshold && iValue >= threshold) {
				return val;
			}
		}
		
		return -1;
	}
	
	/**
	 * It returns a histogram, where the keys are cardinalities and the values are the number of
	 * instances in the map that have equal or fewer values than the key of the map. The entry [2: 20] in the 
	 * resulting map means that there are 20 keys in the map that have at most 2 values associated to them.
	 * @param map
	 * @return
	 */
	private IntHashMap<Integer> buildCumulativeHistogram(Map<ByteString, IntHashMap<ByteString>> map) {
		IntHashMap<Integer> histogram = new IntHashMap<>();
		for (ByteString subject : map.keySet()) {
			for (int i = 0; i < map.get(subject).size(); ++i) {
				histogram.increase(i);	
			}
		}
		return histogram;
	}
	
	/**
	 * It returns a histogram for the number of values associated to each instance in the map.
	 * For example the entry [2: 20] in the map means that there are 20 keys that have 2 values
	 * in the input map.
	 * @param map
	 * @return
	 */
	public IntHashMap<Integer> buildHistogram(Map<ByteString, IntHashMap<ByteString>> map) {
		IntHashMap<Integer> histogram = new IntHashMap<>();
		for (ByteString subject : map.keySet()) {
			histogram.increase(map.get(subject).size());
		}
		return histogram;		
	}
	
	/**
	 * Returns the maximal number of values an entity can have for the given relation.
	 * @param relation
	 * @param threshold
	 * @return
	 */
	public int maximalCardinality(ByteString relation) {
		Map<ByteString, IntHashMap<ByteString>> map = 
				get(relation2subject2object, relation);
		return maximalCardinality(relation, map);
	}
	
	/**
	 * Returns the maximal number of values smaller than limit than 
	 * an entity can have for the given relation.
	 * @param relation
	 * @param threshold
	 * @return
	 */
	public int maximalCardinality(ByteString relation, int limit) {
		Map<ByteString, IntHashMap<ByteString>> map = 
				get(relation2subject2object, relation);
		return maximalCardinality(relation, map, limit);
	}
	
	private int maximalCardinality(ByteString relation, 
			Map<ByteString, IntHashMap<ByteString>> map, int limit) {
		IntHashMap<Integer> histogram = buildHistogram(map);
		List<Integer> keys = histogram.decreasingKeys();
		Collections.sort(keys);
		Object[] keysArray = keys.toArray();
		int idx = Arrays.binarySearch(keysArray, limit);
		int val = limit;
		if (idx == -1) return val;
		if (idx < -1) idx = -idx - 2;
		while (val >= limit && 
				idx >= 0) {
			val = ((Integer)keysArray[idx]).intValue();
			--idx;
		} 
		return val;
	}

	public int maximalCardinalityInv(ByteString relation) {
		Map<ByteString, IntHashMap<ByteString>> map = 
				get(relation2object2subject, relation);
		return maximalCardinality(relation, map);
	}
	
	public int maximalCardinalityInv(ByteString relation, int limit) {
		Map<ByteString, IntHashMap<ByteString>> map = 
				get(relation2object2subject, relation);
		return maximalCardinality(relation, map, limit);
	}
	
	private int maximalCardinality(ByteString relation, Map<ByteString, IntHashMap<ByteString>> map) {
		IntHashMap<Integer> histogram = buildHistogram(map);
		List<Integer> keys = histogram.decreasingKeys();
		Collections.sort(keys);
		Collections.reverse(keys);
		return keys.get(0);
	}
	
	// ---------------------------------------------------------------------------
	// Utilities
	// ---------------------------------------------------------------------------
	
	/**
	 * Returns a new KB containing the triples that are present in 
	 * the KBs.
	 * @param otherKb
	 * @return
	 */
	public KB intersect(KB otherKb) {
		ByteString[] triple = new ByteString[3];
		KB result = new KB();
		for (ByteString subject : subject2relation2object.keySet()) {
			triple[0] = subject;
			Map<ByteString, IntHashMap<ByteString>> tail = subject2relation2object.get(subject);			
			for (ByteString predicate : tail.keySet()) {
				triple[1] = predicate;
				for (ByteString object : tail.get(predicate)) {
					triple[2] = object;
					if (otherKb.contains(triple)) {
						result.add(triple);
					}
				}
			}
		}
		return result;
	}
	
	/** Deletion **/
	
	/*** Delete a triple ***/
	public boolean delete(CharSequence subject, CharSequence predicate, CharSequence object) {
		return delete(compress(subject), compress(predicate), compress(object));
	}
	
	/**
	 * Removes the given triple from the in-memory KB.
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 */
	public boolean delete(ByteString subject, ByteString predicate, ByteString object) {
		if (contains(subject, predicate, object)) {
			subjectSize.decrease(subject);
			relationSize.decrease(predicate);
			objectSize.decrease(object);
			removeFromIndex(subject, predicate, object, subject2relation2object);
			removeFromIndex(subject, object, predicate, subject2object2relation);
			removeFromIndex(predicate, subject, object, relation2subject2object);
			removeFromIndex(predicate, object, subject, relation2object2subject);
			removeFromIndex(object, subject, predicate, object2subject2relation);
			removeFromIndex(object, predicate, subject, object2relation2subject);
			--size;
			return true;
		}
		
		return false;
	}
	
	/** Remove a triple from an index **/
	protected void removeFromIndex(
			ByteString s1,
			ByteString s2,
			ByteString s3,
			Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> index) {
		Map<ByteString, IntHashMap<ByteString>> imap2 = index.get(s1);
		IntHashMap<ByteString> imap3 = imap2.get(s2);
		if (imap3 == null) {
			System.out.println("Problem for prediction " + s1 + " " + s2 + " " + s3);
		}
		imap3.remove(s3);
		if (imap3.isEmpty()) {
			imap2.remove(s2);
			if (imap2.isEmpty()) {
				index.remove(s1);
			}
		}
	}
	
	/**
	 * It outputs statistical information about the KB.
	 * @param detailRelations If true, print also information about the relations
	 */
	public void summarize(boolean detailRelations) {
		System.out.println("Number of subjects: " + size(Column.Subject));
		System.out.println("Number of relations: " + size(Column.Relation));
		System.out.println("Number of objects: " + size(Column.Object));
		System.out.println("Number of facts: " + size());
		
		if (detailRelations) {
			System.out.println("Relation\tTriples\tFunctionality"
					+ "\tInverse functionality\tVariance\tInverse Variance"
					+ "\tNumber of subjects\tNumber of objects");
			for(ByteString relation: relationSize.keys()){
				System.out.println(relation + "\t" + relationSize.get(relation) + 
						"\t" + functionality(relation) + 
						"\t" + inverseFunctionality(relation) + 
						"\t" + variance(relation) +
						"\t" + inverseVariance(relation) +
						"\t" + relation2subject2object.get(relation).size() +
						"\t" + relation2object2subject.get(relation).size());
			}
		}
	}
	
	/**
	 * It outputs information about the distribution of the number of properties per
	 * instance in relations.
	 * @param useSignatureTypes
	 */
	public void summarizeDistributions(boolean useSignatureTypes) {
		summarize(true);
		System.out.println();
		List<ByteString> ommittedRelations = Arrays.asList(ByteString.of("rdf:type"),
				ByteString.of("rdfs:domain"), ByteString.of("rdfs:range"));
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("sample.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (ByteString relation : relationSize.keys()) {
			if (ommittedRelations.contains(relation))
				continue;
			
			System.out.println(relation);
			IntHashMap<Integer> distribution = new IntHashMap<>();
			Map<ByteString, IntHashMap<ByteString>> theMap = null;
			boolean isFunctional = isFunctional(relation);
			if (isFunctional) {
				theMap = relation2subject2object.get(relation);
			} else {
				theMap = relation2object2subject.get(relation);
			}
			
			if (useSignatureTypes) {				
				Set<ByteString> instances = null;
				if (isFunctional) {
					instances = Schema.getDomainSet(this, relation);
					// We still intersect them with the entities we have in the KB
					//instances.retainAll(subjectSize);
				} else {
					// We still intersect them with the entities we have in the KB					
					instances = Schema.getRangeSet(this, relation);
					//instances.retainAll(objectSize);
				}
				// Now count the instances not occurring in the relation.
				for (ByteString instance : instances) {
					if (!theMap.containsKey(instance)) {
						distribution.increase(0);
						if (Math.random() >= 0.9) {
							if (isFunctional)
								writer.write(instance + "\t" + relation + "\t" + "NULL\n");
							else
								writer.write("NULL" + "\t" + relation + "\t" + instance + "\n");
						}
					}
				}				
			}
			for (ByteString argument : theMap.keySet()) {
				distribution.increase(theMap.get(argument).size());
			}
			U.printHistogramAndCumulativeDistribution(distribution);
			System.out.println();
		}
		
		writer.close();
	}
	
	public void dump() {
		dump(System.out);
	}
	
	
	/**
	 * Dump the contents of the in-memory KB in TSV format into a PrintStream
	 * @param out
	 */
	public void dump(PrintStream out) {
		for (ByteString subject : subject2relation2object.keySet()) {
			Map<ByteString, IntHashMap<ByteString>> iMap = subject2relation2object.get(subject);
			for (ByteString relation : iMap.keySet()) {
				for (ByteString object : iMap.get(relation)) {
					out.println(subject + delimiter + relation + delimiter + object);
				}
			}
		}
	}
	
	/**
	 * Returns true if the KB contains at least one fact with this relation
	 * @param relation
	 * @return
	 */
	public boolean containsRelation(ByteString relation) {
		return relationSize.contains(relation);
	}

	/**
	 * Outputs statistics about the types (classes) present in the KB.
	 */
	public void summarizeTypes() {
		if (relation2object2subject.containsKey(Schema.typeRelationBS)) {
			Map<ByteString, IntHashMap<ByteString>> map = 
					relation2object2subject.get(Schema.typeRelationBS);
			for (ByteString type : map.keySet()) {
				System.out.println(type + "\t" + map.get(type).size());
			}
		}
	}
	
	public void loadDiff(File file, KB oldKB, ByteString... exceptions)
			throws IOException {
		long size = size();
		List<ByteString> exceptionsList = Arrays.asList(exceptions);
		if (file.isDirectory()) 
			throw new UnsupportedOperationException("Expected kb file, not a directory");
		for (String line : new FileLines(file, "UTF-8", null)) {
			if (line.endsWith("."))
				line = Char17.cutLast(line);
			String[] split = line.split(delimiter);
			if (split.length == 3) {
				if (exceptionsList.contains(ByteString.of(split[1].trim())) 
						|| !oldKB.contains(split[0].trim(), split[1].trim(), split[2].trim()))
					add(split[0].trim(), split[1].trim(), split[2].trim());
			} else if (split.length == 4) {
				if (exceptionsList.contains(ByteString.of(split[2].trim())) 
						|| !oldKB.contains(split[1].trim(), split[2].trim(), split[3].trim()))
					add(split[1].trim(), split[2].trim(), split[3].trim());
			}
		}
		Announce.message("Diff KB Loaded", (size() - size), "facts");
	}


	/**
	 * Returns a new KB containing the entities that are not present in 
	 * the KBs.
	 * @param otherKb
	 * @return
	 */
	public KB newEntitiesKB(Set<ByteString> oldEntities) {
		Set<ByteString> newEntities = relation2subject2object.get(Schema.typeRelationBS).keySet();
		ByteString[] triple = new ByteString[3];
		KB result = new KB();
		for (ByteString subject : subject2relation2object.keySet()) {
			triple[0] = subject;
			Map<ByteString, IntHashMap<ByteString>> tail = subject2relation2object.get(subject);			
			for (ByteString predicate : tail.keySet()) {
				triple[1] = predicate;
				for (ByteString object : tail.get(predicate)) {
					triple[2] = object;
					if ((newEntities.contains(subject) && !oldEntities.contains(subject)) || (newEntities.contains(object) && !oldEntities.contains(object))) {
						result.add(triple);
					}
				}
			}
		}
		return result;
	}
        
        public Set<ByteString> getRelationSet() {
            return new HashSet<>(relationSize);
        }
        
        public Set<ByteString> getClassSet() {
            return null;
        }
	
	public static void main(String[] args) {
		System.out.println(Arrays.binarySearch(new int[]{3, 4}, 1));
		
	}
}