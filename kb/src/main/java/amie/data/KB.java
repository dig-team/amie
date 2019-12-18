package amie.data;

import static amie.data.U.decrease;
import static amie.data.U.decreasingKeys;
import static amie.data.U.increase;
import amie.data.starpattern.SignedPredicate;
import amie.data.tuple.IntArrays;
import amie.data.tuple.IntPair;
import amie.data.tuple.IntTriple;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.OperationNotSupportedException;

import javatools.administrative.Announce;
import javatools.datatypes.ByteString;

import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;
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
    
        protected static Lock mappingLock = new ReentrantLock();
        protected static ArrayList<ByteString> idToEntity = new ArrayList(Arrays.asList(ByteString.of("null")));
        protected static ArrayList<ByteString> compositeEntity = new ArrayList();
        
        protected static Object2IntMap<ByteString> entityToId = new Object2IntOpenHashMap<ByteString>();
        protected static Object2IntMap<ByteString> compositeToId = new Object2IntOpenHashMap<ByteString>();
        
        /** Variable sign (as defined in SPARQL) **/
	public static final char VariableSign = '?';
        
        private static final String VariableRegex = Pattern.quote(Character.toString(VariableSign)) 
                + "(_)?([a-z])([0-9])?([0-9])?";
        
        private static final Pattern VariablePattern = Pattern.compile(VariableRegex);
        
        public static boolean isComposite(int id) {
            return id <= -2048;
        }
        
        public static int mapComposite(CharSequence cs) {
            ByteString b = _compress(cs);
            int r;
            mappingLock.lock();
            if (entityToId.containsKey(b)) {
                throw new IllegalStateException(cs.toString() + " is used as usual and composite entity");
            }
            if (compositeToId.containsKey(b)) {
                r = compositeToId.getInt(b);
            } else {
                compositeEntity.add(b);
                compositeToId.put(b, -2047-compositeEntity.size());
                r = -2047-compositeEntity.size();
            }
            mappingLock.unlock();
            return r;
        }
        
        public static final String hasNumberOfValuesEquals = "hasNumberOfValuesEquals";
	
	public static final String hasNumberOfValuesEqualsInv = "hasNumberOfValuesEqualsInv";
	
	public static final int hasNumberOfValuesEqualsBS = KB.mapComposite(hasNumberOfValuesEquals);
	
	public static final int hasNumberOfValuesEqualsInvBS = KB.mapComposite(hasNumberOfValuesEqualsInv);
	
	public static final String hasNumberOfValuesGreaterThan = "hasNumberOfValuesGreaterThan";
	
	public static final String hasNumberOfValuesGreaterThanInv = "hasNumberOfValuesGreaterThanInv";
	
	public static final int hasNumberOfValuesGreaterThanBS = KB.mapComposite(hasNumberOfValuesGreaterThan);
	
	public static final int hasNumberOfValuesGreaterThanInvBS = KB.mapComposite(hasNumberOfValuesGreaterThanInv);
	
	public static final String hasNumberOfValuesSmallerThan = "hasNumberOfValuesSmallerThan";
			
	public static final String hasNumberOfValuesSmallerThanInv = "hasNumberOfValuesSmallerThanInv";		
	
	public static final int hasNumberOfValuesSmallerThanBS = KB.mapComposite(hasNumberOfValuesSmallerThan);
	
	public static final int hasNumberOfValuesSmallerThanInvBS = KB.mapComposite(hasNumberOfValuesSmallerThanInv);		
	
        public static final IntList cardinalityRelations = IntArrays.asList(hasNumberOfValuesEqualsBS,
                hasNumberOfValuesEqualsInvBS, hasNumberOfValuesGreaterThanBS, hasNumberOfValuesGreaterThanInvBS,
                hasNumberOfValuesSmallerThanBS, hasNumberOfValuesSmallerThanInvBS);
                
        private static final String cardinalityRelationsRegex = "(" + 
			hasNumberOfValuesEquals + "|" + hasNumberOfValuesGreaterThan + "|" + hasNumberOfValuesSmallerThan + "|" +
			hasNumberOfValuesEqualsInv + "|" + hasNumberOfValuesGreaterThanInv + "|" + hasNumberOfValuesSmallerThanInv + 
			")([0-9]+)";
	
	private static final Pattern cardinalityRelationsRegexPattern = 
			Pattern.compile(cardinalityRelationsRegex);
        
        public static final int COMPOSE_SIZE = 15;
        
        public static int compose(int id, int n) {
            return -(-id + (n << COMPOSE_SIZE));
        }
        
        public static IntPair uncompose(int c) {
            if (!isComposite(c)) { return null; }
            return new IntPair(-((-c) % ((1 << COMPOSE_SIZE))), (-c) >> COMPOSE_SIZE);
        }
        
        public static int mapComposite(CharSequence cs, int n) {
            return compose(mapComposite(cs), n);
        }
        
        private static int parseCardinality(CharSequence cs) {
            Matcher m = cardinalityRelationsRegexPattern.matcher(cs);
            if (m.matches()) {
                return mapComposite(m.group(1), Integer.parseInt(m.group(2)));
            }
            return 0;
        }
	/**
	 * Determines whether the relation has 
	 * @param byteString
	 * @return
	 */
	public static IntPair parseCardinalityRelation(int composite) {
            IntPair r = uncompose(composite);
            if (r == null) { return null; }
            if (!cardinalityRelations.contains(r.first)) { return null; }
            return r;
	}
        
        public static String unmapComposite(int composite) {
            IntPair p = uncompose(composite);
            return compositeEntity.get(-p.first-2048).toString() + Integer.toString(p.second);
        }
        
	/** TRUE if the int is a SPARQL variable */
	public static boolean isVariable(CharSequence s) {
		return (s.length() > 0 && s.charAt(0) == VariableSign);
	}
        
        public static boolean isVariable(int s) {
            return (s < 0 && s > -2048);
        }
	
        public static boolean isOpenableVariable(CharSequence s) {
            return (s.length() > 1 && s.charAt(0) == VariableSign && s.charAt(1) != '_');
        }
        
        public static boolean isOpenableVariable(int s) {
            return (s < 0 && s > -1024);
        }
        
        /**
         * Map a variable of the form [a-z][0-9]{1-2} to an integer between 1 and 1023.
         * @param letter
         * @param num
         * @return 
         */
        private static int mapVariable(char letter, int num1, int num2) {
            int r = letter - 96;
            if (num1 < 0) {
                return r;
            } else {
                if (num2 < 0) {
                    return ((r-1) * 10) + num1 + 27;
                } else {
                    return Math.min(((r-1) * 100) + num1*10 + num2 + 287, 1023);
                }
            }
        }
        
        public static int parseVariable(CharSequence cs) {
            Matcher m = VariablePattern.matcher(cs);
            if (m.matches()) {
                if (m.group(1) == null) {
                    return -mapVariable(m.group(2).charAt(0), (m.group(3) == null) ? -1 : Integer.parseInt(m.group(3)),
                            (m.group(4) == null) ? -1 : Integer.parseInt(m.group(4)));
                } else {
                    return -1023-mapVariable(m.group(2).charAt(0), (m.group(3).isEmpty()) ? -1 : Integer.parseInt(m.group(3)),
                            (m.group(4) == null) ? -1 : Integer.parseInt(m.group(4)));
                }
            }
            throw new IllegalArgumentException("Variable " + cs.toString() + " DO NOT MATCH \"\\?(_?)[a-z][0-9]{1,2}\"");
        }
        
        private static void unmapVariable(int pos, StringBuilder sb) {
            int mod = 1;
            int r = 0;
            if (pos <= 26) {
                sb.append((char) (pos + 96));
            } else if (pos <= 286) { 
                pos -= 27; // ?a0 <-> -27
                sb.append((char) (pos / 10 + 97));
                sb.append(pos % 10);
            } else {
                pos -= 287; // ?a00 <-> -287
                sb.append((char) (pos / 100 + 97));
                sb.append(pos % 100);
            }
        }
        
        public static String unparseVariable(int v) {
            StringBuilder sb = new StringBuilder();
            sb.append(VariableSign);
            if (!isOpenableVariable(v)) {
                sb.append('_');
                v += 1023;
            }
            unmapVariable(-v, sb);
            return sb.toString();
        }
        
        public static String unmap(int e) {
            if (isComposite(e)) {
                return unmapComposite(e);
            }
            if (isVariable(e)) {
                return unparseVariable(e);
            }
            if (e < idToEntity.size()) {
                return idToEntity.get(e).toString();
            }
            
            throw new IllegalArgumentException("Cannot unmap invalid id: " + e + " (/" + idToEntity.size() + ")");
        }
        
        public static int map(CharSequence cs) {
            if (isVariable(cs)) {
                return parseVariable(cs);
            }
            
            int r = 0;
            if ((r = parseCardinality(cs)) != 0) {
                return r;
            }
            
            ByteString b = _compress(cs);
            mappingLock.lock();
            if (compositeToId.containsKey(b)) {
                r = mapComposite(b);
            } else if (entityToId.containsKey(b)) {
                r = entityToId.getInt(b);
            } else {
                r = idToEntity.size();
                idToEntity.add(b);
                entityToId.put(b, r);
            }
            mappingLock.unlock();
            return r;
        }

	// ---------------------------------------------------------------------------
	// Indexes
	// ---------------------------------------------------------------------------

	/** Index */
	protected final Int2ObjectMap<Int2ObjectMap<IntSet>> subject2relation2object = new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();

	/** Index */
	public final Int2ObjectMap<Int2ObjectMap<IntSet>> relation2object2subject = new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();

	/** Index */
	protected final Int2ObjectMap<Int2ObjectMap<IntSet>> object2subject2relation = new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();

	/** Index */
	public final Int2ObjectMap<Int2ObjectMap<IntSet>> relation2subject2object = new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();

	/** Index */
	protected final Int2ObjectMap<Int2ObjectMap<IntSet>> object2relation2subject = new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();

	/** Index */
	protected final Int2ObjectMap<Int2ObjectMap<IntSet>> subject2object2relation = new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();

	/** Number of facts per subject */
	protected final Int2IntMap subjectSize = new Int2IntOpenHashMap();

	/** Number of facts per object */
	protected final Int2IntMap objectSize = new Int2IntOpenHashMap();

	/** Number of facts per relation */
	public final Int2IntMap relationSize = new Int2IntOpenHashMap();

	// ---------------------------------------------------------------------------
	// Statistics
	// ---------------------------------------------------------------------------
	
	/**
	 * Subject-subject overlaps
	 */
	public final Int2ObjectMap<Int2IntMap> subject2subjectOverlap = new Int2ObjectOpenHashMap<Int2IntMap>();

	/**
	 * Subject-object overlaps
	 */
	public final Int2ObjectMap<Int2IntMap> subject2objectOverlap = new Int2ObjectOpenHashMap<Int2IntMap>();

	/**
	 * Object-object overlaps
	 */
	public final Int2ObjectMap<Int2IntMap> object2objectOverlap = new Int2ObjectOpenHashMap<Int2IntMap>();

	/** Number of facts */
	protected long size;
	
	// ---------------------------------------------------------------------------
	// Constants
	// ---------------------------------------------------------------------------

	/** X transitiveType T predicate **/
    public static final String TRANSITIVETYPEstr = "transitiveType";
	
	public static final int TRANSITIVETYPEbs = KB.map(TRANSITIVETYPEstr);
	
	/** (X differentFrom Y Z ...) predicate */
	public static final String DIFFERENTFROMstr = "differentFrom";

	/** (X differentFrom Y Z ...) predicate */
	public static final int DIFFERENTFROMbs = KB.map(DIFFERENTFROMstr);

	/** (X equals Y Z ...) predicate */
	public static final String EQUALSstr = "equals";

	/** (X equals Y Z ...) predicate */
	public static final int EQUALSbs = KB.map(EQUALSstr);
	
	/** r(X, y') exists for some y', predicate */
	public static final String EXISTSstr = "exists";
	
	/** r(X, y') exists for some y', predicate */
	public static final int EXISTSbs = KB.map(EXISTSstr);
	
	/** r(y', X) exists for some y', predicate */
	public static final String EXISTSINVstr = "existsInv";
	
	/** r(y', X) exists for some y', predicate */
	public static final int EXISTSINVbs = KB.map(EXISTSINVstr);	
	
	/** r(X, y') does NOT exists for some y', predicate */
	public static final String NOTEXISTSstr = "~exists";
	
	/** r(X, y') does NOT exists for some y', predicate */
	public static final int NOTEXISTSbs = KB.map(NOTEXISTSstr);
	
	/** r(y', X) does NOT exists for some y', predicate */
	public static final String NOTEXISTSINVstr = "~existsInv";
	
	/** r(y', X) does NOT exists for some y', predicate */
	public static final int NOTEXISTSINVbs = KB.map(NOTEXISTSINVstr);
	
	public static final IntList specialRelations = IntArrays.asList(TRANSITIVETYPEbs, DIFFERENTFROMbs, 
			EQUALSbs, EXISTSbs, EXISTSINVbs, NOTEXISTSbs, NOTEXISTSINVbs);

	/** Identifiers for the overlap maps */
	public static final int SUBJECT2SUBJECT = 0;

	public static final int SUBJECT2OBJECT = 2;

	public static final int OBJECT2OBJECT = 4;

	public enum Column { Subject, Relation, Object };
	
// ---------------------------------------------------------------------------
	// Loading
	// ---------------------------------------------------------------------------

	protected String delimiter = "\t";
	
	public void setDelimiter(String newDelimiter) {
		delimiter = newDelimiter;
	}
        
        protected boolean optimConnectedComponent = true;
        protected boolean optimExistentialDetection = true;
        
        public void setOptimConnectedComponent(boolean value) {
            this.optimConnectedComponent = value;
        }
        
        public void setOptimExistentialDetection(boolean value) {
            this.optimExistentialDetection = value;
        }

	public KB() {}

	/** Methods to add single facts to the KB **/
	protected boolean add(int subject, int relation,
			int object,
			Int2ObjectMap<Int2ObjectMap<IntSet>> map) {
		synchronized (map) {
			Int2ObjectMap<IntSet> relation2object = map
					.get(subject);
			if (relation2object == null)
				map.put(subject,
						relation2object = new Int2ObjectOpenHashMap<IntSet>());
			IntSet objects = relation2object.get(relation);
			if (objects == null)
				relation2object.put(relation,
						objects = new IntOpenHashSet());
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
	public boolean add(int... fact) {
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
	protected boolean add(int subject, int relation, int object) {
		if (!add(subject, relation, object, subject2relation2object))
			return (false);
		add(relation, object, subject, relation2object2subject);
		add(object, subject, relation, object2subject2relation);
		add(relation, subject, object, relation2subject2object);
		add(object, relation, subject, object2relation2subject);
		add(subject, object, relation, subject2object2relation);
		synchronized (subjectSize) {
			increase(subjectSize, subject);
		}
		synchronized (relationSize) {
			increase(relationSize, relation);
		}
		synchronized (objectSize) {
			increase(objectSize, object);
		}

		synchronized (subject2subjectOverlap) {
			Int2IntMap overlaps = subject2subjectOverlap
					.get(relation);
			if (overlaps == null) {
				subject2subjectOverlap.put(relation,
						new Int2IntOpenHashMap());
			}
		}

		synchronized (subject2objectOverlap) {
			Int2IntMap overlaps = subject2objectOverlap
					.get(relation);
			if (overlaps == null) {
				subject2objectOverlap.put(relation,
						new Int2IntOpenHashMap());
			}
		}

		synchronized (object2objectOverlap) {
			Int2IntMap overlaps = object2objectOverlap
					.get(relation);
			if (overlaps == null) {
				object2objectOverlap.put(relation,
						new Int2IntOpenHashMap());
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
		for (int subject: otherKb.subject2relation2object.keySet()) {
			Int2ObjectMap<IntSet> subjectMap = 
					otherKb.subject2relation2object.get(subject);
			for (int relation : subjectMap.keySet()) {
				for (int object : subjectMap.get(relation)) {
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
		IntSet entities = new IntOpenHashSet(subjectSize.keySet());
		entities.addAll(objectSize.keySet());
		return entities.size();
	}

	/**
	 * It clears the overlap tables and rebuilds them. Recommended when new facts has been added
	 * to the KB after the initial loading.
	 */
	public void rebuildOverlapTables() {
		resetOverlapTables(); 
		buildOverlapTables();
	}
        
        public void rebuildOverlapTables(int nThread) {
		resetOverlapTables(); 
		buildOverlapTables(nThread);
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
	private void resetMap(Int2ObjectMap<Int2IntMap> map) {
		for (int key : map.keySet()) {
			map.put(key, new Int2IntOpenHashMap());
		}
	}

	/**
	 * It builds the overlap tables for relations. They contain the number of subjects and
	 * objects in common between pairs of relations. They can be used for join cardinality estimation.
	 */
	public void buildOverlapTables() {
		for (int r1 : relationSize.keySet()) {
			IntSet subjects1 = relation2subject2object.get(r1)
					.keySet();
			IntSet objects1 = relation2object2subject.get(r1)
					.keySet();
			for (int r2 : relationSize.keySet()) {
				IntSet subjects2 = relation2subject2object.get(r2)
						.keySet();
				IntSet objects2 = relation2object2subject.get(r2)
						.keySet();

				if (r1 != r2) {
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

				if (r1 != r2) {
					int oooverlap = computeOverlap(objects1, objects2);
					object2objectOverlap.get(r1).put(r2, oooverlap);
					object2objectOverlap.get(r2).put(r1, oooverlap);
				} else {
					object2objectOverlap.get(r1).put(r1, objects2.size());
				}
			}
		}
	}
        
        public void buildOverlapTables(int nThread) {
            try {
                OverlapTableComputation.compute(this, nThread);
            } catch (InterruptedException ex) {
                Logger.getLogger(KB.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    protected static class OverlapTableComputation extends Thread {
        
        public static LinkedList<Pair<SignedPredicate, SignedPredicate>> initQueue(KB db, int nThread) {
            LinkedList<Pair<SignedPredicate, SignedPredicate>> queue = new LinkedList<>();
            SignedPredicate sp1, sp1i, sp2, sp2i;
            for (int r1 : db.relationSize.keySet()) {
                sp1 = new SignedPredicate(r1, true);
                sp1i = new SignedPredicate(r1, false);
                db.subject2subjectOverlap.put(r1, new Int2IntOpenHashMap());
                db.subject2objectOverlap.put(r1, new Int2IntOpenHashMap());
                db.object2objectOverlap.put(r1, new Int2IntOpenHashMap());
                
                for (int r2 : db.relationSize.keySet()) {
                
                    if (r1 < r2) { 
                        continue;
                    }
                    
                    sp2 = new SignedPredicate(r2, true);
                    sp2i = new SignedPredicate(r2, false);
                    queue.add(new Pair<>(sp1, sp2));
                    queue.add(new Pair<>(sp1i, sp2i));
                    queue.add(new Pair<>(sp1, sp2i));
                    queue.add(new Pair<>(sp1i, sp2));
                }
            }
            
            for (int i = 0; i < nThread; i++) {
                queue.add(new Pair<>(null, null));
            }
            return queue;
        }
        
        public static void compute(KB db, int nThread) throws InterruptedException {
            Thread[] threadList = new Thread[nThread];
            LinkedList<Pair<SignedPredicate, SignedPredicate>> queue = initQueue(db, nThread);
            
            for (int i = 0; i < nThread; i++) {
                threadList[i] = (new OverlapTableComputation(db, queue));
            }
            
            for (int i = 0; i < nThread; i++) {
                threadList[i].start();
            }
            
            for (int i = 0; i < nThread; i++) {
                threadList[i].join();
            }
        }
        
        public static void set(KB db, SignedPredicate sp1, SignedPredicate sp2, int overlap) {
            Int2IntMap e;
            if (sp1.subject && sp2.subject) {
                e = db.subject2subjectOverlap.get(sp1.predicate);
                synchronized(e) {
                    e.put(sp2.predicate, overlap);
                }
                if (sp1.predicate != sp2.predicate) {
                    e = db.subject2subjectOverlap.get(sp2.predicate);
                    synchronized(e) {
                        e.put(sp1.predicate, overlap);
                    }
                }
            } else if (!sp1.subject && !sp2.subject) {
                e = db.object2objectOverlap.get(sp1.predicate);
                synchronized(e) {
                    e.put(sp2.predicate, overlap);
                }
                if (sp1.predicate != sp2.predicate) {
                    e = db.object2objectOverlap.get(sp2.predicate);
                    synchronized(e) {
                        e.put(sp1.predicate, overlap);
                    }
                }
            } else if (sp1.subject) {
                e = db.subject2objectOverlap.get(sp1.predicate);
                synchronized(e) {
                    e.put(sp2.predicate, overlap);
                }
            } else { // if (sp2.subject)
                e = db.subject2objectOverlap.get(sp2.predicate);
                synchronized(e) {
                    e.put(sp1.predicate, overlap);
                }
            }
        }
        
        final LinkedList<Pair<SignedPredicate, SignedPredicate>> queue;
        KB db;
        
        public OverlapTableComputation(KB db, LinkedList<Pair<SignedPredicate, SignedPredicate>> queue) {
            this.queue = queue;
            this.db = db;
        }
        
        public void run() {
            Pair<SignedPredicate, SignedPredicate> q;
            int overlap;
            IntHashMap e;
            while(true) {
                synchronized(queue) {
                    q = queue.pollFirst();
                }
                if (q == null || q.first == null) {
                    break;
                }
                
                if (q.first.equals(q.second)) {
                    overlap = db.getMap(q.first).keySet().size();
                } else {
                    overlap = (int) SetU.countIntersection(db.getMap(q.first).keySet(), db.getMap(q.second).keySet());
                }
                set(db, q.first, q.second, overlap);
            }
        }
    }

	/**
	 * Calculates the number of elements in the intersection of two sets of ByteStrings.
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static int computeOverlap(IntSet s1, IntSet s2) {
		int overlap = 0;
		for (int r : s1) {
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
	public double functionality(int relation) {
		if (relation == EQUALSbs) {
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
	public double inverseFunctionality(int relation) {
		if (relation == EQUALSbs) {
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
	public double colFunctionality(int relation, Column col) {
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
	public boolean isFunctional(int relation) {
		return functionality(relation) >= inverseFunctionality(relation);
	}
	
	/**
	 * It returns the functionality or the inverse functionality of a relation.
	 * @param relation
	 * @param inversed If true, the method returns the inverse functionality, otherwise
	 * it returns the standard functionality.
	 * @return
	 */
	public double functionality(int relation, boolean inversed) {
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
	public double inverseFunctionality(int relation, boolean inversed) {
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
	public double inverseVariance(int relation) {
		// First calculate the average
		double avg = 1.0 / inverseFunctionality(relation);
		// Now compute the formula
		double sum = 0.0;
		Int2ObjectMap<IntSet> targetMap = 
				relation2object2subject.get(relation);
		for (int object : targetMap.keySet()) {
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
	public double variance(int relation) {
		// First calculate the average
		double avg = 1.0 / functionality(relation);
		// Now compute the formula
		double sum = 0.0;
		Int2ObjectMap<IntSet> targetMap = 
				relation2subject2object.get(relation);
		for (int subject : targetMap.keySet()) {
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
	public int overlap(int relation1, int relation2, int overlap) {
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
	public int relationSize(int relation) {
		return relationSize.get(relation);
	}

	/**
	 * It returns the number of distinct instance of one of the arguments (columns)
	 * of a relation.
	 * @param relation
	 * @param column. Subject or Object
	 * @return
	 */
	public int relationColumnSize(int relation, Column column) {
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
	public static boolean differentFrom(int... triple) {
		if (triple[1] != DIFFERENTFROMbs)
			throw new IllegalArgumentException(
					"DifferentFrom can only be called with a differentFrom predicate: "
							+ toString(triple));
		for (int i = 2; i < triple.length; i++) {
			if (triple[0] == triple[i])
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
	public static boolean equalTo(int... triple) {
		if (triple[1] != EQUALSbs)
			throw new IllegalArgumentException(
					"equalTo can only be called with a equals predicate: "
							+ toString(triple));
		for (int i = 2; i < triple.length; i++) {
			if (triple[0] != triple[i])
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
	protected IntSet get(
			Int2ObjectMap<Int2ObjectMap<IntSet>> map,
			int key1, int key2) {
		Int2ObjectMap<IntSet> m = map.get(key1);
		if (m == null)
			return (new IntOpenHashSet());
		IntSet r = m.get(key2);
		if (r == null)
			return (new IntOpenHashSet());
		return (r);
	}
	
	/**
	 * It returns the second and third level values of a map given the keys for the first
	 * level.  
	 * @param key
	 * @param map A 3-level map 
	 * @return
	 * */
	protected Int2ObjectMap<IntSet> get(
			Int2ObjectMap<Int2ObjectMap<IntSet>> map,
			int key) {
		Int2ObjectMap<IntSet> m = map.get(key);
		if (m == null)
			return (Int2ObjectMaps.emptyMap());
		else
			return (m);
	}

	/**
	 * Returns the results of the triple pattern query, if it contains exactly 1
	 * variable
	 */
	public IntSet resultsOneVariable(CharSequence... triple) {
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
	public IntSet resultsOneVariable(int... triple) {
		if (triple[1] == TRANSITIVETYPEbs) {
			if (isVariable(triple[0])) {
				/*
				 * Return all the entities in subclasses of triple[2]
				 */
				IntSet result = new IntOpenHashSet();
				for (int subtype : Schema.getAllSubTypes(this, triple[2])) {
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
		if (triple[1] == DIFFERENTFROMbs)
			throw new IllegalArgumentException("Cannot query differentFrom: "
					+ toString(triple));
		if (triple[1] == EQUALSbs) {
			IntSet result = new IntOpenHashSet();
			if (isVariable(triple[0]))
				result.add(triple[2]);
			else
				result.add(triple[0]);
			return (result);
		}		
		if (triple[1] == EXISTSbs) {
			if (isVariable(triple[0])) 
				return (new IntOpenHashSet(get(subject2relation2object, triple[2]).keySet()));
			else 
				return (new IntOpenHashSet(get(relation2subject2object, triple[0]).keySet()));
		}		
		if (triple[1] == EXISTSINVbs) {
			if (isVariable(triple[0])) 
				return (new IntOpenHashSet(get(object2relation2subject, triple[2]).keySet()));
			else 
				return (new IntOpenHashSet(get(relation2object2subject, triple[0]).keySet()));
		}
		
		if (triple[1] == NOTEXISTSbs) {
			IntSet values = new IntOpenHashSet();
			if (isVariable(triple[0])) {
				values.addAll(relationSize.keySet());
				values.removeAll(get(subject2relation2object, triple[2]).keySet());
			} else {
				values.addAll(subjectSize.keySet());
				values.removeAll(get(relation2subject2object, triple[0]).keySet());
			}			
			return new IntOpenHashSet(values);
		}
		
		if (triple[1] == NOTEXISTSINVbs) {
			IntSet values = new IntOpenHashSet();
			if (isVariable(triple[0])) {
				values.addAll(relationSize.keySet());
				values.removeAll(get(object2relation2subject, triple[2]).keySet());
			} else { 
				values.addAll(objectSize.keySet());
				values.removeAll(get(relation2object2subject, triple[0]).keySet());
			}
			return new IntOpenHashSet(values);
		}
		
		if (isComposite(triple[1])) {
                    IntPair cardinalityRelation = uncompose(triple[1]);
			if (isVariable(triple[2])) {
				throw new UnsupportedOperationException("The relation " + triple[1] 
						+ " does not support variables in the object position");
			}
			IntSet results = new IntOpenHashSet();
			if (cardinalityRelation.first == hasNumberOfValuesEqualsBS
					|| cardinalityRelation.first == hasNumberOfValuesEqualsInvBS) {
				Int2ObjectMap<IntSet> map =(
						cardinalityRelation.first == hasNumberOfValuesEqualsBS)?
								get(relation2subject2object, triple[2]) :
								get(relation2object2subject, triple[2]);			
				if (cardinalityRelation.second > 0) {
					for (int entity : map.keySet()) {
						if (map.get(entity).size() == cardinalityRelation.second)
							results.add(entity);
					}
				} else {
					IntSet set =(cardinalityRelation.first == hasNumberOfValuesEqualsBS)?
							subjectSize.keySet() : objectSize.keySet();
					for (int entity : set) {
						if (!map.containsKey(entity)) {
							results.add(entity);
						}
					}
				}
			} else if ((cardinalityRelation.first == hasNumberOfValuesGreaterThanBS)
					||(cardinalityRelation.first == hasNumberOfValuesGreaterThanInvBS)) {
				Int2ObjectMap<IntSet> map = 
						cardinalityRelation.first == (hasNumberOfValuesGreaterThanBS) ?
								get(relation2subject2object, triple[2]) :
								get(relation2object2subject, triple[2]);	
				
				if (cardinalityRelation.second == 0) {
					return new IntOpenHashSet(map.keySet());
				}
					
				for (int entity : map.keySet()) {
					if (map.get(entity).size() > cardinalityRelation.second)
						results.add(entity);
				}
			} else {
				IntSet set =(cardinalityRelation.first == hasNumberOfValuesSmallerThanBS)?
						subjectSize.keySet() : objectSize.keySet();
				Int2ObjectMap<IntSet> map =(
						cardinalityRelation.first == hasNumberOfValuesSmallerThanBS)?
								get(relation2subject2object, triple[2]) :
								get(relation2object2subject, triple[2]);	
				for (int entity : set) {
					if (map.containsKey(entity)) {
						if (cardinalityRelation.second == 1)
							continue;
						
						if (map.get(entity).size() < cardinalityRelation.second)
							results.add(entity);	
					} else {
						results.add(entity);
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
	protected boolean contains(int... fact) {
		if (fact[1] == TRANSITIVETYPEbs) {
			for (int type : get(this.subject2relation2object, fact[0], Schema.typeRelationBS)) {
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
		if (isComposite(fact[1])) {
                    IntPair cardinalityRelation = uncompose(fact[1]);
			if (cardinalityRelation.first == hasNumberOfValuesEqualsBS) {
				if (get(subject2relation2object, fact[0]).containsKey(fact[2])) {
					return get(subject2relation2object, fact[0]).get(fact[2]).size() 
							== cardinalityRelation.second;
				} else {
					return cardinalityRelation.second == 0;
				}
			} else if (cardinalityRelation.first == hasNumberOfValuesEqualsInvBS) {
				if (get(object2relation2subject, fact[0]).containsKey(fact[2])) {
					return get(object2relation2subject, fact[0]).get(fact[2]).size() 
					== cardinalityRelation.second;
				} else {
					return cardinalityRelation.second == 0;
				}
			} else if (cardinalityRelation.first == hasNumberOfValuesGreaterThanBS) {
				if (get(subject2relation2object, fact[0]).containsKey(fact[2])) {
					return get(subject2relation2object, fact[0]).get(fact[2]).size() 
							> cardinalityRelation.second;
				} else {
					return false;
				}								
			} else if (cardinalityRelation.first == hasNumberOfValuesGreaterThanInvBS) {
				if (get(object2relation2subject, fact[0]).containsKey(fact[2])) {
					return get(object2relation2subject, fact[0]).get(fact[2]).size() 
							> cardinalityRelation.second;
				} else { 
					return false;
				}
			} else if (cardinalityRelation.first == hasNumberOfValuesSmallerThanBS) {
				if (get(subject2relation2object, fact[0]).containsKey(fact[2])) {
					return get(subject2relation2object, fact[0]).get(fact[2]).size() 
								< cardinalityRelation.second;
				} else {
					return true;
				}
			} else if (cardinalityRelation.first == hasNumberOfValuesSmallerThanInvBS) {
				if (get(object2relation2subject, fact[0]).containsKey(fact[2])) {
					return get(object2relation2subject, fact[0]).get(fact[2]).size() 
							< cardinalityRelation.second;
				} else {
					return true;
				}
			}
		}
		return (get(subject2relation2object, fact[0], fact[1])
				.contains(fact[2]));
	}

	/**
	 * Returns the results of a triple query pattern with two variables as a map
	 * of first value to set of second values.
	 */
	public Int2ObjectMap<IntSet> resultsTwoVariables(
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
	public Int2ObjectMap<IntSet> resultsTwoVariablesByPos(
			int pos1, int pos2, CharSequence[] triple) {
		if (!isVariable(triple[pos1]) || !isVariable(triple[pos2])
				|| numVariables(triple) != 2 || pos1 == pos2)
			throw new IllegalArgumentException(
					"Triple should contain 2 variables, one at " + pos1
							+ " and one at " + pos2 + ": " + toString(triple));
		return (resultsTwoVariablesByPos(pos1, pos2, triple(triple)));
	}	
	
	/**
	 * Returns the results of a triple query pattern with two variables as a map
	 * of first value to set of second values
	 */
	public Int2ObjectMap<IntSet> resultsTwoVariables(
			int var1, int var2, int[] triple) {
		int varPos1 = varpos(var1, triple);
		int varPos2 = varpos(var2, triple);
		return resultsTwoVariablesByPos(varPos1, varPos2, triple);
	}
	

	/**
	 * Returns the results of a triple query pattern with two variables as a map
	 * of first value to set of second values
	 */
	public Int2ObjectMap<IntSet> resultsTwoVariablesByPos(
			int pos1, int pos2, int[] triple) {
		if (triple[1] == TRANSITIVETYPEbs) {
			Int2ObjectMap<IntSet> result = new Int2ObjectOpenHashMap<>();
			switch(pos1) {
			case 0:
				/*
				 * Return a map from all entities to all super-classes
				 */
				for (int entity : get(relation2subject2object, Schema.typeRelationBS).keySet()) {
					result.put(entity, Schema.getAllTypesForEntity(this, entity));
				}
				return result;
			case 2:
				/*
				 * Return a map from all types to all entities of sub-classes
				 */
				IntSet allTypes = (Schema.isTaxonomyMaterialized()) ? Schema.getAllDefinedTypes() : get(relation2object2subject, Schema.typeRelationBS).keySet();
				for (int type : allTypes) {
					result.put(type, resultsOneVariable(triple(KB.map("?s"), TRANSITIVETYPEbs, type)));
				}
				return result;
			case 1:
			default:
				throw new IllegalArgumentException("The argument at position " + pos1 
						+ " should be a variable");
			}
		}
		if (triple[1] == DIFFERENTFROMbs)
			throw new IllegalArgumentException(
					"Cannot query with differentFrom: " + toString(triple));
		if (triple[1] == EQUALSbs) {
			Int2ObjectMap<IntSet> result = new Int2ObjectOpenHashMap<>();
			for (int entity : subject2object2relation.keySet()) {
				IntSet innerResult = new IntOpenHashSet();
				innerResult.add(entity);
				result.put(entity, innerResult);
			}
			return (result);
		}
		if ((triple[1] == EXISTSbs)||(triple[1] == EXISTSINVbs)) {
			Int2ObjectMap<Int2ObjectMap<IntSet>> map =(
					triple[1] == EXISTSbs)? relation2subject2object
							: relation2object2subject;
			Int2ObjectMap<IntSet> result = new Int2ObjectOpenHashMap<>();
			for (int relation : map.keySet()) {
				IntSet innerResult = new IntOpenHashSet();
				innerResult.addAll(map.get(relation).keySet());
				result.put(relation, innerResult);
			}
			return (result);
		}
		
		if ((triple[1] == NOTEXISTSbs)||(triple[1] == NOTEXISTSINVbs)) {
			Int2ObjectMap<Int2ObjectMap<IntSet>> map =(
					triple[1] == NOTEXISTSbs)? relation2subject2object
							: relation2object2subject;
			Int2ObjectMap<IntSet> result = new Int2ObjectOpenHashMap<>();
			for (int relation : map.keySet()) {
				IntSet uMap =(triple[1] == NOTEXISTSbs)? 
						new IntOpenHashSet(subjectSize.keySet()) : new IntOpenHashSet(objectSize.keySet());				
				uMap.removeAll(map.get(relation).keySet());	
				result.put(relation, new IntOpenHashSet(uMap));
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
	public Int2ObjectMap<Int2ObjectMap<IntSet>> resultsThreeVariables(
			int var1, int var2, int var3,
			int[] triple) {
		int varPos1 = varpos(var1, triple);
		int varPos2 = varpos(var2, triple);
		int varPos3 = varpos(var3, triple);
		
		return resultsThreeVariablesByPos(varPos1, varPos2, varPos3, triple);
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
	private Int2ObjectMap<Int2ObjectMap<IntSet>> resultsThreeVariablesByPos(
			int varPos1, int varPos2, int varPos3, int[] triple) {
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
	public long countOneVariable(int... triple)  {
		if (triple[1] == DIFFERENTFROMbs)
			return (Long.MAX_VALUE);
		if (triple[1] == EQUALSbs)
			return (1);
		if (triple[1] == EXISTSbs) {
			if (isVariable(triple[2]))
				return get(relation2subject2object, triple[0]).size();
			else 
				return get(subject2relation2object, triple[2]).size();
		}
		if (triple[1] == EXISTSINVbs) {
			if (isVariable(triple[2]))
				return get(relation2object2subject, triple[0]).size();
			else
				return get(object2relation2subject, triple[2]).size();
		}
		if (triple[1] == NOTEXISTSbs) {
			if (isVariable(triple[2]))
				return subjectSize.size() - get(relation2subject2object, triple[0]).size();
			else
				return relationSize.size() - get(subject2relation2object, triple[2]).size();
		}
		if (triple[1] == NOTEXISTSINVbs) {
			if (isVariable(triple[2]))
				return objectSize.size() - get(relation2object2subject, triple[0]).size();
			else
				return relationSize.size() - get(object2relation2subject, triple[2]).size();
		}
					
		if (isComposite(triple[1])) {
                    IntPair compositeRelation = uncompose(triple[1]);
			if (compositeRelation.first == (hasNumberOfValuesEqualsBS)
					|| compositeRelation.first == (hasNumberOfValuesEqualsInvBS)) {
				
				Int2ObjectMap<IntSet> map = 
						compositeRelation.first == (hasNumberOfValuesEqualsBS) ?
						get(this.relation2subject2object, triple[2]) :
						get(this.relation2object2subject, triple[2]);				
				long count = 0;
				if (compositeRelation.second > 0) {
					for (int s : map.keySet()) {
						if (map.get(s).size() == compositeRelation.second)
							++count; 
					}
				} else {
					IntSet set =
					compositeRelation.first == (hasNumberOfValuesEqualsBS) ? 
							subjectSize.keySet() : objectSize.keySet();
					for (int s : set) {
						if (!map.containsKey(s)) {
							++count;
						}
					}
				}
				return count;
			} else if (compositeRelation.first == (hasNumberOfValuesGreaterThanBS)
					|| compositeRelation.first == (hasNumberOfValuesGreaterThanInvBS)) {
				// If it is 0, just return the size of the map
				Int2ObjectMap<IntSet> map = 
						compositeRelation.first == (hasNumberOfValuesGreaterThanBS) ?
						get(this.relation2subject2object, triple[2]) :
						get(this.relation2object2subject, triple[2]);
				if (compositeRelation.second == 0) {
					return map.size();
				} else {
					long count = 0;
					for (int s : map.keySet()) {
						if (map.containsKey(s)) {
							if (map.get(s).size() > compositeRelation.second)
								++count; 
						}
					}
					return count;	
				}
			} else {
				Int2ObjectMap<IntSet> map = 
						compositeRelation.first == (hasNumberOfValuesSmallerThanBS) ?
						get(this.relation2subject2object, triple[2]) :
						get(this.relation2object2subject, triple[2]);
				long count = 0;
				IntSet set =
					compositeRelation.first == (hasNumberOfValuesSmallerThanBS) ? 
							subjectSize.keySet() : objectSize.keySet();
				for (int s : set) {					
					if (map.containsKey(s)) {						
						if (compositeRelation.second == 1)
							continue;
						
						if (map.get(s).size() < compositeRelation.second)
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
	protected long countTwoVariables(int... triple) {
		if (triple[1] == TRANSITIVETYPEbs) {
			Int2ObjectMap<IntSet> resultTwoVars = 
					resultsTwoVariablesByPos(0, 2, triple);
			long count = 0;
			for (int subject : resultTwoVars.keySet()) {
				count += resultTwoVars.get(subject).size();
			}
			return count;
		}
		if (triple[1] == DIFFERENTFROMbs)
			return (Long.MAX_VALUE);
		if (triple[1] == EQUALSbs)
			return (subject2relation2object.size());
		if (triple[1] == EXISTSbs) {
			long count = 0;
			for (int relation : relationSize.keySet()) {
				count += get(relation2subject2object, relation).size();
			}
			return count;
		}
		if (triple[1] == EXISTSINVbs) {
			long count = 0;
			for (int relation : relationSize.keySet()) {
				count += get(relation2object2subject, relation).size();
			}
			return count;
		}
		if ((triple[1] == NOTEXISTSbs)||(triple[1] == NOTEXISTSINVbs)) {			
			Int2ObjectMap<IntSet> resultTwoVars = 
					resultsTwoVariables(triple[0], triple[2], triple);
			long count = 0;
			for (int relation : resultTwoVars.keySet()) {
				count += resultTwoVars.get(relation).size();
			}
			return count;
		}
		
		if (!isVariable(triple[0]))
			return (long) (subjectSize.getOrDefault(triple[0], 0));
		if (!isVariable(triple[1])) {
			return (long) (relationSize.getOrDefault(triple[1], 0));
		}
		return (long) (objectSize.getOrDefault(triple[2], 0));
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
        
        public static int numVariables(int... fact) {
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
	public static boolean containsVariables(List<int[]> query) {
		// TODO Auto-generated method stub
		for (int[] triple : query) {
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
	public long count(int... triple) {
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
	protected static List<int[]> remove(int pos, List<int[]> triples) {
		if (pos == 0)
			return (triples.subList(1, triples.size()));
		if (pos == triples.size() - 1)
			return (triples.subList(0, triples.size() - 1));
		List<int[]> result = new ArrayList<>(triples);
		result.remove(pos);
		return (result);
	}

	
	public static AtomicLong STAT_NUMBER_OF_CALL_TO_MRT = new AtomicLong();
	/**
	 * It returns the index of the most restrictive triple, -1 if most restrictive has count 0.
	 * The most restrictive triple is the one that contains the smallest number of satisfying
	 * instantiations.
	 **/
	protected int mostRestrictiveTriple(List<int[]> triples) {
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
	private boolean isSpecialAtom(int[] atom) {
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
	protected int mostRestrictiveTriple(List<int[]> triples,
			int variable) {
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
	protected int mostRestrictiveTriple(List<int[]> triples,
			int var1, int var2) {
		int bestPos = -1;
		long count = Long.MAX_VALUE;
		for (int i = 0; i < triples.size(); i++) {
			int[] triple = triples.get(i);
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
	private boolean contains(int var, int[] triple) {
		return(triple[0] == var || triple[1] == var
				|| triple[2] == var);
	}
        
        private boolean contains(int var, List<int[]> triples) {
		for (int[] triple : triples) {
                    if (contains(var, triple)) return true;
                }
                return false;
	}

	/** 
	 * Returns the position of a variable in a triple.
	 * @param var
	 * @param triple
	 **/
	public static int varpos(int var, int[] triple) {
		for (int i = 0; i < triple.length; i++) {
			if (var == triple[i])
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
	public static int firstVariablePos(int... fact) {
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
	public static int secondVariablePos(int... fact) {
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
	public boolean existsBS1(List<int[]> triples) {
		if (triples.isEmpty())
			return (false);
		if (triples.size() == 1)
			return (count(triples.get(0)) != 0);
		int bestPos = mostRestrictiveTriple(triples);
		if (bestPos == -1)
			return (false);
                List<int[]> otherTriples;
		int[] best = triples.get(bestPos);

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
                        otherTriples = remove(bestPos, triples);
                        if (optimExistentialDetection && !contains(best[firstVarIdx], otherTriples)) {
                            //if (otherTriples.isEmpty()) return (true);
                            return (existsBS1(otherTriples));
                        }
			try (Instantiator insty = new Instantiator(
					otherTriples, best[firstVarIdx])) {
				for (int inst : resultsOneVariable(best)) {
					if (existsBS1(insty.instantiate(inst)))
						return (true);
				}
			}
			return (false);
		case 2:
			int firstVar = firstVariablePos(best);
			int secondVar = secondVariablePos(best);
			otherTriples = remove(bestPos, triples);
                        Int2ObjectMap<IntSet> instantiations;
                        if (!optimExistentialDetection
                                || (contains(best[firstVar], otherTriples) && contains(best[secondVar], otherTriples))) {
                            instantiations = resultsTwoVariablesByPos(firstVar, secondVar, best);
                            try (Instantiator insty1 = new Instantiator(otherTriples,
                                            best[firstVar]);
                                            Instantiator insty2 = new Instantiator(otherTriples,
                                                            best[secondVar])) {
				for (int val1 : instantiations.keySet()) {
                                    insty1.instantiate(val1);
                                    for (int val2 : instantiations.get(val1)) {
					if (existsBS1(insty2.instantiate(val2)))
                                            return (true);
                                    }
				}
                            }
                        } else {
                            int nonExistentialVariablePos = (contains(best[firstVar], otherTriples)) ? firstVar : secondVar;
                            int existentialVariablePos = (contains(best[firstVar], otherTriples)) ? secondVar : firstVar;
                            instantiations = resultsTwoVariablesByPos(nonExistentialVariablePos, existentialVariablePos, best);
                            try (Instantiator insty1 = new Instantiator(otherTriples, best[nonExistentialVariablePos])) {
                                for (int val1 : instantiations.keySet()) {
                                    if (existsBS1(insty1.instantiate(val1)))
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
	public long countDistinct(int variable, List<int[]> query) {
                return (long) (selectDistinct(variable, query).size());
	}

	// ---------------------------------------------------------------------------
	// Selection
	// ---------------------------------------------------------------------------

	/** returns the instances that fulfill a certain condition */
	public IntSet selectDistinct(CharSequence variable,
			List<CharSequence[]> query) {
		return (selectDistinct(compress(variable), triples(query)));
	}

	/** returns the instances that fulfill a certain condition */
	public IntSet selectDistinct(int variable,
			List<int[]> query) {
		// Only one triple
		if (query.size() == 1) {
			int[] triple = query.get(0);
			switch (numVariables(triple)) {
			case 0:
				return (IntSets.EMPTY_SET);
			case 1:
				return (resultsOneVariable(triple));
			case 2:
				int firstVar = firstVariablePos(triple);
				int secondVar = secondVariablePos(triple);
				if (firstVar == -1 || secondVar == -1) {
					System.out.println("[DEBUG] Problem with query "
							+ KB.toString(query));
				}
				if (triple[firstVar] == variable)
					return (resultsTwoVariablesByPos(firstVar, secondVar, triple)
							.keySet());
				else
					return (resultsTwoVariablesByPos(secondVar, firstVar, triple)
							.keySet());
			default:
				switch (varpos(variable, query.get(0))) {
				case 0:
					return (subjectSize.keySet());
				case 1:
					return (relationSize.keySet());
				case 2:
					return (objectSize.keySet());
				}
			}
			throw new RuntimeException("Very weird: SELECT " + variable
					+ " WHERE " + toString(query.get(0)));
		}

		int bestPos = mostRestrictiveTriple(query);
		IntSet result = new IntOpenHashSet();
		if (bestPos == -1)
			return (result);
		int[] best = query.get(bestPos);

		// If the variable is in the most restrictive triple
		if (varpos(variable, best) != -1) {
			switch (numVariables(best)) {
			case 1 :
				try (Instantiator insty = new Instantiator(remove(bestPos,
						query), variable)) {
					for (int inst : resultsOneVariable(best)) {
						if (existsBS1(insty.instantiate(inst)))
							result.add(inst);
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Int2ObjectMap<IntSet> instantiations =(best[firstVar]
						 == variable)? resultsTwoVariablesByPos(firstVar,
						secondVar, best) : resultsTwoVariablesByPos(secondVar,
						firstVar, best);
                                int otherVariable =(best[firstVar] == variable)? secondVar : firstVar;
                                List<int[]> otherTriples = remove(bestPos, query);
				try (Instantiator insty = new Instantiator(
                                        (contains(best[otherVariable], otherTriples)) ? query : otherTriples, variable)) {
					for (int val : instantiations.keySet()) {
						if (existsBS1(insty.instantiate(val)))
							result.add(val);
					}
				}
				break;
			case 3:
			default:
				try (Instantiator insty = new Instantiator(remove(bestPos, query), variable)) {
					int varPos = varpos(variable, best);
					int var1, var2, var3;
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

					for (int inst : resultsThreeVariables(var1, var2, var3, best).keySet()) {
						if (existsBS1(insty.instantiate(inst)))
							result.add(inst);
					}
				}
				break;
			}
			return (result);
		}

		// If the variable is not in the most restrictive triple...
                Int2ObjectMap<IntSet> instantiations;
                List<int[]> others = remove(bestPos, query);
		switch (numVariables(best)) {
		case 0:
			return (selectDistinct(variable, others));
		case 1:
			int var = best[firstVariablePos(best)];
                        if (optimExistentialDetection && !contains(var, others)) {
                            // Can be used for 4+ atoms rules.
                            return (selectDistinct(variable, others));
                        }
			try (Instantiator insty = new Instantiator(others, var)) {
				for (int inst : resultsOneVariable(best)) {
					result.addAll(selectDistinct(variable,
							insty.instantiate(inst)));
				}
			}
			break;
		case 2:
                        int firstVar = firstVariablePos(best);
                        int secondVar = secondVariablePos(best);
                        if (!optimExistentialDetection // Always execute if the optim is deactivated
                                || (contains(best[firstVar], others) && contains(best[secondVar], others))) {
                            instantiations = resultsTwoVariablesByPos(firstVar, secondVar, best);
                            try (Instantiator insty1 = new Instantiator(others, best[firstVar]);
                                    Instantiator insty2 = new Instantiator(others,
                                         best[secondVar])) {
                                for (int val1 : instantiations.keySet()) {
                                    insty1.instantiate(val1);
                                    for (int val2 : instantiations.get(val1)) {
                                        result.addAll(selectDistinct(variable,
                                                insty2.instantiate(val2)));
                                    }
                                }
                            }
			} else {
                            int nonExistentialVariablePos = (contains(best[firstVar], others)) ? firstVar : secondVar;
                            int existentialVariablePos = (contains(best[firstVar], others)) ? secondVar : firstVar;
                            instantiations = resultsTwoVariablesByPos(nonExistentialVariablePos, existentialVariablePos, best);
                            try (Instantiator insty1 = new Instantiator(others, best[nonExistentialVariablePos])) {
                                for (int val1 : instantiations.keySet()) {
                                    result.addAll(selectDistinct(variable,
					insty1.instantiate(val1)));
                                }
                            }
                        }
			break;
		case 3:
		default:
			firstVar = firstVariablePos(best);
			secondVar = secondVariablePos(best);
			Int2ObjectMap<Int2ObjectMap<IntSet>> map = 
					resultsThreeVariables(best[0], best[1], best[2], best);
			try (Instantiator insty1 = new Instantiator(others, best[0]);
					Instantiator insty2 = new Instantiator(others, best[1]);
						Instantiator insty3 = new Instantiator(others, best[2])) {
				for (int val1 : map.keySet()) {
					insty1.instantiate(val1);
					instantiations = map.get(val1);
					for (int val2 : instantiations.keySet()) {
						insty2.instantiate(val2);
						IntSet instantiations2 = instantiations.get(val2);
						for (int val3 : instantiations2) {
							result.addAll(selectDistinct(variable, insty3.instantiate(val3)));
						}
					}
				}
			}
			break;

		}
		return (result);
	}
        
    /** 
     * returns the instances that fulfill a certain condition 
     *
     * @param query: may be modified in place. Return to a consistent state when 
     * the iterator is empty or by calling the close() method on the iterator.
     * A closed iterator can no longer be iterated upon.
     */
    public IntIterator selectDistinctIterator(IntSet result,
            int variable, List<int[]> query) {
        // Only one triple
        if (query.size() == 1) {
            int[] triple = query.get(0);
            switch (numVariables(triple)) {
                case 0:
                    return IntIterators.EMPTY_ITERATOR;
                case 1:
                    return (new SetU.addNotInIntIterator(resultsOneVariable(triple), result));
                case 2:
                    int firstVar = firstVariablePos(triple);
                    int secondVar = secondVariablePos(triple);
                    if (firstVar == -1 || secondVar == -1) {
                        System.out.println("[DEBUG] Problem with query "
                                + KB.toString(query));
                    }
                    if (triple[firstVar] == variable) {
                        return (new SetU.addNotInIntIterator(
                                resultsTwoVariablesByPos(firstVar, secondVar, triple)
                                        .keySet(), result));
                    } else {
                        return (new SetU.addNotInIntIterator(
                                resultsTwoVariablesByPos(secondVar, firstVar, triple)
                                        .keySet(), result));
                    }
                default:
                    switch (varpos(variable, query.get(0))) {
                        case 0:
                            return (new SetU.addNotInIntIterator(subjectSize.keySet(), result));
                        case 1:
                            return (new SetU.addNotInIntIterator(relationSize.keySet(), result));
                        case 2:
                            return (new SetU.addNotInIntIterator(objectSize.keySet(), result));
                    }
            }
            throw new RuntimeException("Very weird: SELECT " + variable
                    + " WHERE " + toString(query.get(0)));
        }

        int bestPos = mostRestrictiveTriple(query);
        if (bestPos == -1) {
            return IntIterators.EMPTY_ITERATOR;
        }
        int[] best = query.get(bestPos);

        // If the variable is in the most restrictive triple
        if (varpos(variable, best) != -1) {
            Instantiator insty;
            IntSet instantiations;
            switch (numVariables(best)) {
                case 1:
                    insty = new Instantiator(remove(bestPos, query), variable);
                    instantiations = resultsOneVariable(best);
                    break;
                case 2:
                    int firstVar = firstVariablePos(best);
                    int secondVar = secondVariablePos(best);
                    instantiations =(best[firstVar] == variable)? resultsTwoVariablesByPos(firstVar,
                            secondVar, best).keySet() : resultsTwoVariablesByPos(secondVar,
                                    firstVar, best).keySet();
                    int otherVariable =(best[firstVar] == variable)? secondVar : firstVar;
                    List<int[]> otherTriples = remove(bestPos, query);
                    insty = new Instantiator((contains(best[otherVariable], otherTriples)) ? query : otherTriples, variable);
                    break;
                case 3:
                default:
                    insty = new Instantiator(remove(bestPos, query), variable);
                    int varPos = varpos(variable, best);
                    int var1,
                     var2,
                     var3;
                    switch (varPos) {
                        case 0:
                            var1 = best[0];
                            var2 = best[1];
                            var3 = best[2];
                            break;
                        case 1:
                            var1 = best[1];
                            var2 = best[0];
                            var3 = best[2];
                            break;
                        case 2:
                        default:
                            var1 = best[2];
                            var2 = best[0];
                            var3 = best[1];
                            break;
                    }

                    instantiations = resultsThreeVariables(var1, var2, var3, best).keySet();
                    break;
            }
            return (new KBIteratorU.addNotInIfExistsIterator(this, insty, instantiations, result));
        }

        // If the variable is not in the most restrictive triple...
        Int2ObjectMap<IntSet> instantiations;
        List<int[]> others = remove(bestPos, query);
        switch (numVariables(best)) {
            case 0:
                return (selectDistinctIterator(result, variable, others));
            case 1:
                int var = best[firstVariablePos(best)];
                if (optimExistentialDetection && !contains(var, others)) {
                    return (selectDistinctIterator(result, variable, others));
                }
                return (new KBIteratorU.recursiveSelectForOneVarIterator(this, new Instantiator(others, var), variable, resultsOneVariable(best), result));
            case 2:
                int firstVar = firstVariablePos(best);
                int secondVar = secondVariablePos(best);
                if (!optimExistentialDetection || (contains(best[firstVar], others) && contains(best[secondVar], others))) {
                    return (new KBIteratorU.recursiveSelectForTwoVarIterator(this, 
                                new Instantiator(others, best[firstVar]), 
                                new Instantiator(others, best[secondVar]), 
                                variable, resultsTwoVariablesByPos(firstVar, secondVar, best), result));
                } else {
                    int nonExistentialVariablePos = (contains(best[firstVar], others)) ? firstVar : secondVar;
                    int existentialVariablePos = (contains(best[firstVar], others)) ? secondVar : firstVar;
                    return (new KBIteratorU.recursiveSelectForOneVarIterator(this, 
                                new Instantiator(others, best[nonExistentialVariablePos]), 
                                variable, 
                                resultsTwoVariablesByPos(nonExistentialVariablePos, existentialVariablePos, best).keySet(), 
                                result));
                }
            case 3:
            default:
                return (new KBIteratorU.recursiveSelectForThreeVarIterator(this, 
                            new Instantiator(others, best[0]), 
                            new Instantiator(others, best[1]), 
                            new Instantiator(others, best[2]), 
                            variable, 
                            resultsThreeVariables(best[0], best[1], best[2], best), 
                            result));
        }
    }
    
	// ---------------------------------------------------------------------------
	// Select distinct, two variables
	// ---------------------------------------------------------------------------

	/** Returns all (distinct) pairs of values that make the query true */
	public Int2ObjectMap<IntSet> selectDistinct(
			CharSequence var1, CharSequence var2, List<? extends CharSequence[]> query) {
		return (selectDistinct(compress(var1), compress(var2), triples(query)));
	}

	/** Returns all (distinct) pairs of values that make the query true */
	public Int2ObjectMap<IntSet> selectDistinct(
			int var1, int var2, List<int[]> query) {
		if (query.isEmpty())
			return Int2ObjectMaps.emptyMap();
		if (query.size() == 1) {
			return (resultsTwoVariables(var1, var2, query.get(0)));
		}
		Int2ObjectMap<IntSet> result = new Int2ObjectOpenHashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (int val1 : selectDistinct(var1, query)) {
				IntSet val2s = selectDistinct(var2,
						insty1.instantiate(val1));
				if (!val2s.isEmpty())
					result.put(val1, val2s);
			}
		}
		return (result);
	}
	
	// ---------------------------------------------------------------------------
	// Select distinct, more than 2 variables
	// ---------------------------------------------------------------------------

	/** Return all triplets of values that make the query true **/
	public Int2ObjectMap<Int2ObjectMap<IntSet>> selectDistinct(
			CharSequence var1, CharSequence var2, CharSequence var3,
			List<? extends CharSequence[]> query) {
		return selectDistinct(compress(var1), compress(var2), compress(var3), triples(query));
	}
	
	public Int2ObjectMap<Int2ObjectMap<IntSet>> selectDistinct(
			int var1, int var2, int var3,
			List<int[]> query) {
		if (query.isEmpty()) {
			return Int2ObjectMaps.emptyMap();
		}
		
		if (query.size() == 1) {
			int[] first = query.get(0);
			int numVariables = numVariables(first);
			if (numVariables == 3) {
				return resultsThreeVariables(var1, var2, var3, first);
			} else {
				throw new UnsupportedOperationException("Selection over"
						+ " variables not occuring in the query is not supported");
			}
		}
		
		Int2ObjectMap<Int2ObjectMap<IntSet>> result = 
				new Int2ObjectOpenHashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (int val1 : selectDistinct(var1, query)) {
				insty1.instantiate(val1);
				Int2ObjectMap<IntSet> tail = selectDistinct(var2, var3, query);
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
	private Int2ObjectMap<Int2ObjectMap<IntSet>> fixResultMap(
			Int2ObjectMap<IntSet> resultsTwoVars, int fixLevel, int constant) {
		Int2ObjectMap<Int2ObjectMap<IntSet>> extendedMap = new
				Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();
		switch (fixLevel) {
		case 1:
			extendedMap.put(constant, resultsTwoVars);
			break;
		case 2 :
			for (int val : resultsTwoVars.keySet()) {
				Int2ObjectMap<IntSet> newMap = 
						new Int2ObjectOpenHashMap<IntSet>();
				newMap.put(constant, resultsTwoVars.get(val));
				extendedMap.put(val, newMap);
			}	
		case 3 : default:
			IntSet newMap = new IntOpenHashSet();
			newMap.add(constant);
			for (int val1 : resultsTwoVars.keySet()) {
				Int2ObjectMap<IntSet> intermediateMap = 
						new Int2ObjectOpenHashMap<IntSet>();
				for (int val2 : resultsTwoVars.get(val1)) {
					intermediateMap.put(val2, newMap);
				}
				extendedMap.put(val1, intermediateMap);
			}
			break;
		}
		return extendedMap;
	}

	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> selectDistinct(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4,
			List<? extends CharSequence[]> query) {
		return selectDistinct(compress(var1), compress(var2), compress(var3), compress(var4), triples(query));
	}
	
	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> selectDistinct(
			int var1, int var2, int var3, int var4,
			List<int[]> query) {
		if (query.size() < 2) {
			throw new IllegalArgumentException("The query must have at least 2 atoms");
		}
		
		Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> result = 
				new Int2ObjectOpenHashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (int val1 : selectDistinct(var1, query)) {
				insty1.instantiate(val1);
				Int2ObjectMap<Int2ObjectMap<IntSet>> tail = 
						selectDistinct(var2, var3, var4, query);
				if (!tail.isEmpty()) {
					result.put(val1, tail);
				}
			}
		}
		
		return result;
	}
	
	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>>> selectDistinct(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4, CharSequence var5,
			List<? extends CharSequence[]> query) {
		return selectDistinct(compress(var1), compress(var2), compress(var3), compress(var4), compress(var5), triples(query));
	}
	
	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>>> selectDistinct(
			int var1, int var2, int var3, int var4, int var5,
			List<int[]> query) {
		if (query.size() < 2) {
			throw new IllegalArgumentException("The query must have at least 2 atoms");
		}
		
		Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>>> result = 
				new Int2ObjectOpenHashMap<>();
		try (Instantiator insty1 = new Instantiator(query, var1)) {
			for (int val1 : selectDistinct(var1, query)) {
				insty1.instantiate(val1);
				Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> tail = 
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
	public Int2IntMap frequentBindingsOf(CharSequence variable,
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
	public Int2IntMap frequentBindingsOf(int variable,
			int projectionVariable, List<int[]> query) {
		// If only one triple
                Int2IntMap result = new Int2IntOpenHashMap();
		if (query.size() == 1) {
			int[] triple = query.get(0);
			int varPos = varpos(variable, triple);
			int projPos = varpos(projectionVariable, triple);
			if (varPos == -1 || projPos == -1)
				throw new IllegalArgumentException(
						"Query should contain at least two variables: "
								+ toString(triple));
			if (numVariables(triple) == 2) {
                            increase(result, resultsTwoVariablesByPos(varPos,
						projPos, triple));
                            return (result);
                        }
			// Three variables (only supported if varpos==2 and projPos==0)
			if (projPos != 0)
				throw new UnsupportedOperationException(
						"frequentBindingsOf on most general triple is only possible "
						+ "with projection variable in position 1: "
								+ toString(query));

			// Two variables
			if (varPos == projPos) {
				try (Instantiator insty = new Instantiator(query,
						triple[projPos])) {
					for (int inst : resultsOneVariable(triple)) {
						increase(result, selectDistinct(variable,
								insty.instantiate(inst)));
					}
				}
				return result;
			}

			for (int predicate : relationSize.keySet()) {
				triple[1] = predicate;
				increase(result, predicate, resultsTwoVariablesByPos(varPos, 2-varPos, triple).size());
			}
			triple[1] = variable;
			return (result);
		}

		// Find most restrictive triple
		int bestPos = mostRestrictiveTriple(query, projectionVariable, variable);
		if (bestPos == -1)
			return (result);
		int[] best = query.get(bestPos);
		int varPos = varpos(variable, best);
		int projPos = varpos(projectionVariable, best);
		List<int[]> other = remove(bestPos, query);

		// If the variable and the projection variable are in the most
		// restrictive triple
		if (varPos != -1 && projPos != -1) {
			switch (numVariables(best)) {
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Int2ObjectMap<IntSet> instantiations =(best[firstVar]
						 == variable)? resultsTwoVariablesByPos(firstVar,
						secondVar, best) : resultsTwoVariablesByPos(secondVar,
						firstVar, best);
				try (Instantiator insty1 = new Instantiator(other, variable);
						Instantiator insty2 = new Instantiator(other,
								projectionVariable)) {
					for (int val1 : instantiations.keySet()) {
						insty1.instantiate(val1);
						for (int val2 : instantiations.get(val1)) {
							if (existsBS1(insty2.instantiate(val2)))
								increase(result, val1);
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
					for (int inst : resultsOneVariable(best)) {
						increase(       result,
								inst,
								selectDistinct(projectionVariable,
										insty.instantiate(inst)).size());
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Int2ObjectMap<IntSet> instantiations =(best[firstVar]
						 == variable)? resultsTwoVariablesByPos(firstVar,
						secondVar, best) : resultsTwoVariablesByPos(secondVar,
						firstVar, best);
				try (Instantiator insty1 = new Instantiator(query, variable)) {
					for (int val1 : instantiations.keySet()) {
						increase(       result,
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
					for (int inst : resultsOneVariable(best)) {
						increase(result, selectDistinct(variable,
								insty.instantiate(inst)));
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(best);
				int secondVar = secondVariablePos(best);
				Int2ObjectMap<IntSet> instantiations =(best[firstVar]
						 == projectionVariable)? resultsTwoVariablesByPos(
						firstVar, secondVar, best) : resultsTwoVariablesByPos(
						secondVar, firstVar, best);
				try (Instantiator insty1 = new Instantiator(query,
						projectionVariable)) {
					for (int val1 : instantiations.keySet()) {
						increase(result, selectDistinct(variable,
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
	protected Int2IntMap countBindings(int pos,
			int... triple) {
                Int2IntMap result = new Int2IntOpenHashMap();
		switch (numVariables(triple)) {
		case 1:
                        increase(result, resultsOneVariable(triple));
			return (result);
		case 2:
			int pos2 = -1;
			switch (pos) {
			case 0: pos2 = (isVariable(triple[2])) ? 2 : 1; break; // We want the most frequent subjects
			case 1: pos2 = (isVariable(triple[2])) ? 2 : 0; break; // We want the most frequent predicates
			case 2: pos2 = (isVariable(triple[1])) ? 1 : 0; break; // we want the most frequent objects
			}
                        increase(result, resultsTwoVariablesByPos(pos, pos2, triple));
			return (result);
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
	protected Int2IntMap countProjectionBindings(int pos,
			int[] projectionTriple, List<int[]> otherTriples) {
		if (!isVariable(projectionTriple[pos]))
			throw new IllegalArgumentException("Position " + pos + " in "
					+ toString(projectionTriple) + " must be a variable");
		Int2IntMap result = new Int2IntOpenHashMap();
		switch (numVariables(projectionTriple)) {
		case 1:
			try (Instantiator insty = new Instantiator(otherTriples,
					projectionTriple[pos])) {
				for (int inst : resultsOneVariable(projectionTriple)) {
					if (existsBS1(insty.instantiate(inst)))
						increase(result, inst);
				}
			}
			break;
		case 2:
			int firstVar = firstVariablePos(projectionTriple);
			int secondVar = secondVariablePos(projectionTriple);
			Int2ObjectMap<IntSet> instantiations = resultsTwoVariablesByPos(
					firstVar, secondVar, projectionTriple);
			try (Instantiator insty1 = new Instantiator(otherTriples,
					projectionTriple[firstVar]);
					Instantiator insty2 = new Instantiator(otherTriples,
							projectionTriple[secondVar])) {
				for (int val1 : instantiations.keySet()) {
					insty1.instantiate(val1);
					for (int val2 : instantiations.get(val1)) {
						if (existsBS1(insty2.instantiate(val2)))
							increase(result, (firstVar == pos) ? val1 : val2);
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
	public Int2IntMap countProjectionBindings(
			int[] projectionTriple, List<int[]> otherTriples,
			int variable) {
		int pos = varpos(variable, projectionTriple);

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
		List<int[]> wholeQuery = new ArrayList<int[]>();
		wholeQuery.add(projectionTriple);
		wholeQuery.addAll(otherTriples);

		int instVar = 0;
		int posRestrictive = mostRestrictiveTriple(wholeQuery);
		int[] mostRestrictive = (posRestrictive != -1) ? wholeQuery
				.get(posRestrictive) : projectionTriple;
		Int2IntMap result = new Int2IntOpenHashMap();
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
					for (int inst : resultsOneVariable(projectionTriple)) {
						increase(result, selectDistinct(variable,
								insty.instantiate(inst)));
					}
				}
				break;
			case 2:
				int firstVar = firstVariablePos(projectionTriple);
				int secondVar = secondVariablePos(projectionTriple);
				Int2ObjectMap<IntSet> instantiations = resultsTwoVariablesByPos(
						firstVar, secondVar, projectionTriple);
				try (Instantiator insty1 = new Instantiator(otherTriples,
						projectionTriple[firstVar]);
						Instantiator insty2 = new Instantiator(otherTriples,
								projectionTriple[secondVar])) {
					for (int val1 : instantiations.keySet()) {
						insty1.instantiate(val1);
						for (int val2 : instantiations.get(val1)) {
							increase(result, selectDistinct(variable,
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
			List<int[]> otherTriples2 = new ArrayList<int[]>(
					wholeQuery);
			List<int[]> projectionTripleList = new ArrayList<int[]>(
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
					for (int inst : resultsOneVariable(mostRestrictive)) {
						increase(result, countProjectionBindings(
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
						Int2IntMap instantiations = countBindings(
								posInCommon, mostRestrictive);
						for (int b1 : instantiations.keySet()) {
							increase(result, countProjectionBindings(insty3
									.instantiate(b1).get(0), insty1
									.instantiate(b1), variable));
						}
					}
				} else if (nHeadVars == 1) {
					instVar = projectionTriple[firstVariablePos(projectionTriple)];
					try (Instantiator insty = new Instantiator(otherTriples,
							instVar)) {
						for (int inst : resultsOneVariable(projectionTriple)) {
							increase(result, selectDistinct(variable,
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
	public int firstVariableInCommon(int[] t1, int[] t2) {
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
	public int numVarsInCommon(int[] a, int[] b) {
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
	public Int2IntMap countProjectionBindings(
			CharSequence[] projectionTriple, List<CharSequence[]> query,
			CharSequence variable) {
		int[] projection = triple(projectionTriple);
		List<int[]> otherTriples = new ArrayList<>();
		for (CharSequence[] t : query) {
			int[] triple = triple(t);
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
		int[] projection = triple(projectionTriple);
		// Create "otherTriples"
		List<int[]> otherTriples = new ArrayList<>();
		for (CharSequence[] t : query) {
			int[] triple = triple(t);
			if (!Arrays.equals(triple, projection))
				otherTriples.add(triple);
		}
		return (countProjection(projection, otherTriples));
	}

	/**
	 * Counts the number of instances of the projection triple that exist in
	 * joins with the other triples
	 */
	public long countProjection(int[] projectionTriple,
			List<int[]> otherTriples) {
		if (otherTriples.isEmpty())
			return (count(projectionTriple));
		switch (numVariables(projectionTriple)) {
		case 0:
			return (count(projectionTriple));
		case 1:
			long counter = 0;
			int variable = projectionTriple[firstVariablePos(projectionTriple)];
			try (Instantiator insty = new Instantiator(otherTriples, variable)) {
				for (int inst : resultsOneVariable(projectionTriple)) {
					if (existsBS1(insty.instantiate(inst)))
						counter++;
				}
			}
			return (counter);
		case 2:
			counter = 0;
			int firstVar = firstVariablePos(projectionTriple);
			int secondVar = secondVariablePos(projectionTriple);
			Int2ObjectMap<IntSet> instantiations = resultsTwoVariablesByPos(
					firstVar, secondVar, projectionTriple);
			try (Instantiator insty1 = new Instantiator(otherTriples,
					projectionTriple[firstVar])) {
				for (int val1 : instantiations.keySet()) {
					try (Instantiator insty2 = new Instantiator(
							insty1.instantiate(val1),
							projectionTriple[secondVar])) {
						for (int val2 : instantiations.get(val1)) {
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
        
        public long countPairs(CharSequence var1, CharSequence var2,
			List<CharSequence[]> query) {
		return (countDistinctPairs(compress(var1), compress(var2), triples(query)));
	}

	/**
	 * Identifies queries containing the pattern: select ?a ?b where r(?a, ?c)
	 * r(?b, ?c) ... select ?a ?b where r(?c, ?a) r(?c, ?b) ...
	 * 
	 * @param query
	 * @return
	 */
	public int[] identifyHardQueryTypeI(List<int[]> query) {
		if (query.size() < 2)
			return null;

		int lastIdx = query.size() - 1;
		for (int idx1 = 0; idx1 < lastIdx; ++idx1) {
			for (int idx2 = idx1 + 1; idx2 < query.size(); ++idx2) {
				int[] t1, t2;
				t1 = query.get(idx1);
				t2 = query.get(idx2);

				// Not the same relation
				if ((t1[1] != t2[1])|| numVariables(t1) != 2
						|| numVariables(t2) != 2)
					return null;
                                        //continue;

				if ((t1[0] != t2[0])&&(t1[2] == t2[2])) {
					return new int[] { 2, 0, idx1, idx2 };
				} else if ((t1[0] == t2[0])&&(t1[2] != t2[2])) {
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
	public int[] identifyHardQueryTypeII(List<int[]> query) {
		if (query.size() < 2)
			return null;

		int lastIdx = query.size() - 1;
		for (int idx1 = 0; idx1 < lastIdx; ++idx1) {
			for (int idx2 = idx1 + 1; idx2 < query.size(); ++idx2) {
				int[] t1, t2;
				t1 = query.get(idx1);
				t2 = query.get(idx2);

				// Not the same relation
				if (numVariables(t1) != 2 || numVariables(t2) != 2)
					continue;

				if ((t1[0] != t2[0])&&(t1[2] == t2[2])) {
					return new int[] { 2, 0, idx1, idx2 };
				} else if ((t1[0] == t2[0])&&(t1[2] != t2[2])) {
					return new int[] { 0, 2, idx1, idx2 };
				}
			}
		}

		return null;
	}

	public int[] identifyHardQueryTypeIII(List<int[]> query) {
		if (query.size() < 2)
			return null;

		int lastIdx = query.size() - 1;
		for (int idx1 = 0; idx1 < lastIdx; ++idx1) {
			for (int idx2 = idx1 + 1; idx2 < query.size(); ++idx2) {
				int[] t1, t2;
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

	public long countPairs(int var1, int var2,
			List<int[]> query, int[] queryInfo) {

		long result = 0;
		// Approximate count
		int joinVariable = query.get(queryInfo[2])[queryInfo[0]];
		int targetVariable = query.get(queryInfo[3])[queryInfo[1]];
		int targetRelation = query.get(queryInfo[2])[1];

		// Heuristic
		if (relationSize.get(targetRelation) < 50000)
			return countDistinctPairs(var1, var2, query);

		long duplicatesEstimate, duplicatesCard;
		double duplicatesFactor;

		List<int[]> subquery = new ArrayList<int[]>(query);
		subquery.remove(queryInfo[2]); // Remove one of the hard queries
		duplicatesCard = countDistinct(targetVariable, subquery);

		if (queryInfo[0] == 2) {
			duplicatesFactor = (1.0 / functionality(targetRelation)) - 1.0;
		} else {
			duplicatesFactor = (1.0 / inverseFunctionality(targetRelation)) - 1.0;
		}
		duplicatesEstimate = (int) Math.ceil(duplicatesCard * duplicatesFactor);

		try (Instantiator insty1 = new Instantiator(subquery, joinVariable)) {
			for (int value : selectDistinct(joinVariable, subquery)) {
				result += (long) Math
						.ceil(Math.pow(
								countDistinct(targetVariable,
										insty1.instantiate(value)), 2));
			}
		}

		result -= duplicatesEstimate;
		return result;
	}

	public long countPairs(int var1, int var2,
			List<int[]> query, int[] queryInfo,
			int[] existentialTriple, int nonExistentialPosition) {
		long result = 0;
		long duplicatesEstimate, duplicatesCard;
		double duplicatesFactor;
		// Approximate count
		int joinVariable = query.get(queryInfo[2])[queryInfo[0]];
		int targetVariable = 0;
		int targetRelation = query.get(queryInfo[2])[1];
		List<int[]> subquery = new ArrayList<int[]>(query);

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
			for (int value : selectDistinct(joinVariable, subquery)) {
				result += (long) countDistinct(targetVariable,
						insty1.instantiate(value));
			}
		}

		result -= duplicatesEstimate;

		return result;
	}
        
        /**
         * Rewrite the query removing atoms connected to fromVariable only via 
         * removeVariable.
         * 
         * In selectDistinctPair we first select all the possible instantiation
         * of the first variable and then the instantiations of the second.
         * During this second step we can ignore atoms connected only through 
         * the first variable.
         * 
         * A test-case exist in amie.data.KBTest
         * @param query The query to rewrite (unaltered)
         * @param fromVariable 
         * @param removeVariable
         * @return New rewritten query
         */
    public static List<int[]> connectedComponent(List<int[]> query, 
            int fromVariable, int removeVariable) {
        IntSet connectedVariables = new IntOpenHashSet();
        connectedVariables.add(fromVariable);
        boolean fixedpoint;
        do {
            fixedpoint = true;
            for (int[] atom : query) {
                for (int conpos = 0; conpos < 3; conpos++) {
                    if (connectedVariables.contains(atom[conpos])) {
                        for (int newpos = 0; newpos < 3; newpos++) {
                            if (newpos != conpos && isVariable(atom[newpos])
                                    && !connectedVariables.contains(atom[newpos])
                                    && removeVariable != atom[newpos]) {
                                connectedVariables.add(atom[newpos]);
                                fixedpoint = false;
                            }
                        }
                    }
                }
            }
        } while (!fixedpoint);
        List<int[]> result = new ArrayList<>();
        for (int[] atom : query) {
            for (int pos=0; pos < 3; pos++) {
                if (connectedVariables.contains(atom[pos])) {
                    result.add(atom);
                    break;
                }
            }
        }
        return result;
    }
    /**
     * returns the number of distinct pairs (var1,var2) for the query
     */
    public long countDistinctPairs(int var1, int var2,
            List<int[]> query) {

        // Go for the standard plan
        long result = 0;

        try (Instantiator insty1 = new Instantiator((optimConnectedComponent) ? connectedComponent(query, var2, var1) : query, var1)) {
            IntSet bindings = selectDistinct(var1, query);
            for (int val1 : bindings) {
                result += countDistinct(var2, insty1.instantiate(val1));
            }
        }

        return (result);
    }
    
    /**
     * returns the number of distinct pairs (var1,var2) for the query
     */
    public long countDistinctPairsUpTo(long upperBound, int var1, int var2,
            List<int[]> query) {

        // Go for the standard plan
        long result = 0;

        try (Instantiator insty1 = new Instantiator((optimConnectedComponent) ? connectedComponent(query, var2, var1) : query, var1)) {
            IntSet bindings = selectDistinct(var1, query);
            for (int val1 : bindings) {
                result += countDistinct(var2, insty1.instantiate(val1));
                if (result > upperBound) {
                    break;
                }
            }
        }

        return (result);
    }
    
    public long countDistinctPairsUpToWithIterator(long upperBound, int var1, 
            int var2, List<int[]> query) {

        // Go for the standard plan
        long result = 0;
        IntSet bindings, bindings2;

        try (Instantiator insty1 = new Instantiator(
                (optimConnectedComponent) ? connectedComponent(U.deepCloneInt(query), var2, var1) : U.deepCloneInt(query), var1)) {
            bindings = new IntOpenHashSet();
            for (IntIterator bindingsIt = selectDistinctIterator(bindings, var1, query); bindingsIt.hasNext(); ) {
                bindings2 = new IntOpenHashSet();
                for (IntIterator bindingsIt2 = selectDistinctIterator(bindings2, var2, 
                        insty1.instantiate(bindingsIt.nextInt())); bindingsIt2.hasNext(); ) {
                    result += 1;
                    bindingsIt2.nextInt();
                    if (result > upperBound) {
                        break;
                    }
                }
            }
        }
        return (result);
    }
    
	/** Can instantiate a variable in a query with a value */
	public static class Instantiator implements Closeable {
		List<int[]> query;

		int[] positions;

		int variable;
		
		private int atomSize;

		public Instantiator(List<int[]> q, int var, int atomSize) {
			this.atomSize = atomSize;
			positions = new int[q.size() * atomSize];
			int numPos = 0;
			query = q;
			variable = var;
			for (int i = 0; i < query.size(); i++) {
				for (int j = 0; j < query.get(i).length; j++) {
					if (query.get(i)[j] == (variable))
						positions[numPos++] = i * atomSize + j;
				}
			}

			if (numPos < positions.length)
				positions[numPos] = -1;
		}
		
		public Instantiator(List<int[]> q, int var) {
			this(q, var, 3);
		}

		public List<int[]> instantiate(int value) {
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
        
        /** ToString for a triple */
	public static String toString(int[] s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length; i++)
			b.append(unmap(s[i])).append(" ");
		return (b.toString());
	}

	/** ToString for a query */
	public static String toString(List<int[]> s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.size(); i++)
			b.append(toString(s.get(i))).append(" ");
		return (b.toString());
	}

	/** Compresses a string to an internal string */
        private static ByteString _compress(CharSequence s) {
            if (s instanceof ByteString) {
                return (ByteString) s;
            }
            String str = s.toString();
            int pos = str.indexOf("\"^^");
            if (pos != -1)
		str = str.substring(0, pos + 1);
            return ByteString.of(str);
        }
        
	public static int compress(CharSequence s) {
            return KB.map(s);
	}

	/** Makes a list of triples */
	public static List<int[]> triples(int[]... triples) {
		return (Arrays.asList(triples));
	}

	/** makes triples */
	@SuppressWarnings("unchecked")
	public static List<int[]> triples(
			List<? extends CharSequence[]> triples) {
		List<int[]> t = new ArrayList<>();
		for (CharSequence[] c : triples)
			t.add(triple(c));
		return (t);
	}
	
	public static int[] triple2Array(
			IntTriple t) {
		return new int[] { t.first, t.second, t.third };
	}
	
	public static IntTriple array2Triple(
			int[] triple) {
		return new IntTriple(
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
	public static int[] triple(int s, int p, int o) {
		return new int[]{s, p, o};
	}

	/** Makes a triple */

	public static int[] triple(CharSequence s, CharSequence p, CharSequence o) {
		return triple(compress(s), compress(p), compress(o));
	}
        
        /** Makes a triple */
	public static int[] triple(CharSequence... triple) {
		int[] result = new int[triple.length];
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
	public static int[] triple(String s) {
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
	public static ArrayList<int[]> triples(String s) {
		Matcher m = triplePattern.matcher(s);
		ArrayList<int[]> result = new ArrayList<>();
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
	public static Pair<List<int[]>, int[]> rule(String s) {
		List<int[]> triples = triples(s);
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
	public IntSet difference(CharSequence projectionVariable,
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
	public IntSet difference(int projectionVariable,
			List<int[]> antecedent, List<int[]> head) {
		// TODO Auto-generated method stub
		IntSet bodyBindings = new IntOpenHashSet(selectDistinct(
				projectionVariable, antecedent));
		IntSet headBindings = selectDistinct(projectionVariable, head);

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
	public Int2ObjectMap<IntSet> difference(CharSequence var1,
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
	public Int2ObjectMap<IntSet> difference(int var1,
			int var2, List<int[]> antecedent,
			int[] head) {
		// Look for all bindings for the variables that appear on the antecedent
		// but not in the head
		List<int[]> headList = new ArrayList<int[]>(1);
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
	public Int2ObjectMap<IntSet> differenceNoVarsInCommon(CharSequence var1,
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
	public Int2ObjectMap<IntSet> differenceNoVarsInCommon(int var1,
			int var2, List<int[]> antecedent,
			int[] head) {
		// Look for all bindings for the variables that appear on the antecedent
		// but not in the head
		List<int[]> headList = new ArrayList<int[]>(1);
		headList.add(head);
		List<int[]> wholeQuery = new ArrayList<int[]>(antecedent);
		wholeQuery.add(head);
		Int2ObjectMap<IntSet> results = new Int2ObjectOpenHashMap<IntSet>();
		
		Int2ObjectMap<IntSet> antecedentBindings = 
				selectDistinct(var1, var2, antecedent);
		try(Instantiator insty1 = new Instantiator(wholeQuery, var1);
				Instantiator insty2 = new Instantiator(wholeQuery, var2)) {
			for (int val1 : antecedentBindings.keySet()) {
				insty1.instantiate(val1);
				IntSet nestedValues = new IntOpenHashSet();
				for(int val2 : antecedentBindings.get(val1)) {
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
	public Int2ObjectMap<IntSet> difference(int var1,
			int var2, List<int[]> antecedent,
			List<int[]> headList) {
		Int2ObjectMap<IntSet> bodyBindings = selectDistinct(
				var1, var2, antecedent);
		Int2ObjectMap<IntSet> headBindings = selectDistinct(
				var1, var2, headList);
		Int2ObjectMap<IntSet> result = new Int2ObjectOpenHashMap<IntSet>();

		IntSet keySet = bodyBindings.keySet();
		for (int key : keySet) {
			if (!headBindings.containsKey(key)) {
				result.put(key, bodyBindings.get(key));
			} else {
				IntSet partialResult = new IntOpenHashSet();
				for (int value : bodyBindings.get(key)) {
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
	public Int2ObjectMap<Int2ObjectMap<IntSet>> difference(
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
	public Int2ObjectMap<Int2ObjectMap<IntSet>> difference(
			int var1, int var2, int var3,
			List<int[]> antecedent, int[] head) {
		Int2ObjectMap<Int2ObjectMap<IntSet>> results = null;
		
		List<int[]> headList = new ArrayList<>(1);
		headList.add(head);
		
		Int2ObjectMap<Int2ObjectMap<IntSet>> bodyBindings = null;
		Int2ObjectMap<Int2ObjectMap<IntSet>> headBindings = null;
		
		if (numVariables(head) == 3) {
			results = new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();
			bodyBindings = selectDistinct(var1, var2, var3, antecedent);
			headBindings = selectDistinct(var1, var2, var3, headList);
		
			for (int val1 : bodyBindings.keySet()) {
				if (headBindings.containsKey(val1)) {
					// Check at the next level
					Int2ObjectMap<IntSet> tailBody = 
							bodyBindings.get(val1);
					Int2ObjectMap<IntSet> tailHead = 
							headBindings.get(val1);
					for (int val2 : tailBody.keySet()) {
						if (tailHead.containsKey(val2)) {
							IntSet tailBody1 = tailBody.get(val2);
							IntSet headBody1 = tailHead.get(val2);
							for (int val3 : tailBody1) {
								if (!headBody1.contains(val3)) {
									Int2ObjectMap<IntSet> secondLevel = 
											results.get(val1);
									if (secondLevel == null) {
										secondLevel = new Int2ObjectOpenHashMap<IntSet>();
										results.put(val1, secondLevel);
									}
									
									IntSet thirdLevel = 
											secondLevel.get(val2);
									if (thirdLevel == null) {
										thirdLevel = new IntOpenHashSet();
										secondLevel.put(val2, thirdLevel);
									}
									
									thirdLevel.add(val3);								
								}
							}
						} else {
							Int2ObjectMap<IntSet> secondLevel = 
									results.get(val1);
							if (secondLevel == null) {
								secondLevel = new Int2ObjectOpenHashMap<IntSet>();
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
			Int2ObjectMap<IntSet> tmpResult = null;
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
			
			int constant = 0;
			for (int t : head) {
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
	 */
	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> difference(
			CharSequence var1, CharSequence var2, CharSequence var3, CharSequence var4,
			List<? extends CharSequence[]> antecedent, CharSequence[] head) {
		return difference(compress(var1), compress(var2), compress(var3), compress(var4),
				triples(antecedent), triple(head));
	}
	
	/**
	 * Bindings of the projection variables that satisfy the first list of atoms 
	 * but not the second.
	 */
	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> difference(
			int var1, int var2, int var3, int var4,
			List<int[]> antecedent, int[] head) {
		Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> results = new
				Int2ObjectOpenHashMap<Int2ObjectMap<Int2ObjectMap<IntSet>>>();
		
		int headNumVars = numVariables(head);
		if (headNumVars == 3) {
			try (Instantiator insty = new Instantiator(antecedent, var1)) {
				for (int val1 : selectDistinct(var1, antecedent)) {
					insty.instantiate(val1);
					Int2ObjectMap<Int2ObjectMap<IntSet>> diff = 
							difference(var2, var3, var4, antecedent, head);
					if (!diff.isEmpty()) {
						results.put(val1, diff);
					}
				}
			}
		} else if (headNumVars == 2) {
			try (Instantiator insty1 = new Instantiator(antecedent, var1)) {
				try (Instantiator insty2 = new Instantiator(antecedent, var2)) {
					Int2ObjectMap<IntSet> resultsTwoVars =
							selectDistinct(var1, var2, antecedent);
					for (int val1 : resultsTwoVars.keySet()) {
						insty1.instantiate(val1);
						Int2ObjectMap<Int2ObjectMap<IntSet>> level1 =
								new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();
						for (int val2 : resultsTwoVars.get(val1)) {
							insty2.instantiate(val2);
							Int2ObjectMap<IntSet> diff = 
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
	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> differenceNotVarsInCommon(
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
	public Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> differenceNotVarsInCommon(
			int var1, int var2, int var3, int var4,
			List<int[]> antecedent, int[] head) {
		int headNumVars = numVariables(head);
		Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<IntSet>>> results = 
				new Int2ObjectOpenHashMap<Int2ObjectMap<Int2ObjectMap<IntSet>>>();
		List<int[]> wholeQuery = new ArrayList<int[]>(antecedent);
		wholeQuery.add(head);
		
		if (headNumVars == 3) {
			try (Instantiator insty = new Instantiator(wholeQuery, var1)) {
				for (int val1 : selectDistinct(var1, antecedent)) {
					insty.instantiate(val1);
					if (!existsBS1(wholeQuery)) {
						Int2ObjectMap<Int2ObjectMap<IntSet>> diff = 
								selectDistinct(var2, var3, var4, antecedent);
						results.put(val1, diff);
					}
				}
			}
		} else if (headNumVars == 2) {
			try (Instantiator insty1 = new Instantiator(wholeQuery, var1)) {
				try (Instantiator insty2 = new Instantiator(wholeQuery, var2)) {
					Int2ObjectMap<IntSet> resultsTwoVars = selectDistinct(var1, var2, antecedent);
					for (int val1 : resultsTwoVars.keySet()) {
						insty1.instantiate(val1);
						Int2ObjectMap<Int2ObjectMap<IntSet>> level1 =
								new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();
						for (int val2 : resultsTwoVars.get(val1)) {
							insty2.instantiate(val2);
							if (!existsBS1(wholeQuery)) {
								Int2ObjectMap<IntSet> diff = 
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
			Int2ObjectMap<IntSet> bindings) {
		long result = 0;

		for (int binding : bindings.keySet()) {
			result += bindings.get(binding).size();
		}

		return result;
	}


	/**
	 * Get a collection with all the relations of the KB.
	 * @return
	 */
	public IntCollection getRelations() {
		return relationSize.keySet();
	}
	
	/**
	 * It returns all the entities that occur as subjects or objects
	 * in the KB.
	 * @param kb
	 * @return
	 */
	public IntCollection getAllEntities() {
		IntCollection result = new IntOpenHashSet();
		result.addAll(subjectSize.keySet());
		result.addAll(objectSize.keySet());
		return result;
	}
	
	/**
	 * It returns all the entities and the number of facts where they occur.
	 * @param kb
	 * @return
	 */
	public Int2IntMap getEntitiesOccurrences() {
		Int2IntMap result = new Int2IntOpenHashMap();
		increase(result, subjectSize);
		increase(result, objectSize);
		return result;
	}
	

	/**
	 * It returns the number of facts for each of the entities in the
	 * collection provided as argument.
	 * @param kb
	 * @return
	 */
	public Int2IntMap getEntitiesOccurrences(IntCollection entities) {
		Int2IntMap result = new Int2IntOpenHashMap();
		for (int entity : entities) {
			if (subjectSize.containsKey(entity)) increase(result, entity, subjectSize.get(entity));
			if (objectSize.containsKey(entity)) increase(result, entity, objectSize.get(entity));
		}
		return result;
	}
	
	
	/**
	 * Return all the relations (and their sizes) that are bigger than the given
	 * threshold.
	 * @param threshold
	 * @return
	 */
	public Int2IntMap getRelationsBiggerOrEqualThan(int threshold) {	
		Int2IntMap relationsBiggerThan = new Int2IntOpenHashMap();
		for (int relation : relation2subject2object.keySet()) {
			int size = 0;		
			Int2ObjectMap<IntSet> tail = 
					relation2subject2object.get(relation);
			for (int subject : tail.keySet()) {
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
	public IntList getRelationsList() {
		return decreasingKeys(relationSize);
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		int maxCount = 30;
		for (int v1 : subject2relation2object.keySet()) {
			Int2ObjectMap<IntSet> tail = subject2relation2object.get(v1);
			for (int v2 : tail.keySet()) {
				for (int v3 : tail.get(v2)) {
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
	public int maximalRightCumulativeCardinality(int relation, long threshold, int limit) {
		Int2ObjectMap<IntSet> map = get(relation2subject2object, relation);
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
	public int maximalRightCumulativeCardinalityInv(int relation, long threshold, int limit) {
		Int2ObjectMap<IntSet> map = 
				get(relation2object2subject, relation);
		return maximalRightCumulativeCardinality(relation, threshold, map, limit);
	}
	
	private int maximalRightCumulativeCardinality(int relation, long threshold, 
			Int2ObjectMap<IntSet> map, int iMaxThreshold) {
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
	private IntHashMap<Integer> buildCumulativeHistogram(Int2ObjectMap<IntSet> map) {
		IntHashMap<Integer> histogram = new IntHashMap<>();
		for (int subject : map.keySet()) {
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
	public IntHashMap<Integer> buildHistogram(Int2ObjectMap<IntSet> map) {
		IntHashMap<Integer> histogram = new IntHashMap<>();
		for (int subject : map.keySet()) {
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
	public int maximalCardinality(int relation) {
		Int2ObjectMap<IntSet> map = 
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
	public int maximalCardinality(int relation, int limit) {
		Int2ObjectMap<IntSet> map = 
				get(relation2subject2object, relation);
		return maximalCardinality(relation, map, limit);
	}
	
	private int maximalCardinality(int relation, 
			Int2ObjectMap<IntSet> map, int limit) {
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
			val = ((Integer)keysArray[idx]);
			--idx;
		} 
		return val;
	}

	public int maximalCardinalityInv(int relation) {
		Int2ObjectMap<IntSet> map = 
				get(relation2object2subject, relation);
		return maximalCardinality(relation, map);
	}
	
	public int maximalCardinalityInv(int relation, int limit) {
		Int2ObjectMap<IntSet> map = 
				get(relation2object2subject, relation);
		return maximalCardinality(relation, map, limit);
	}
	
	private int maximalCardinality(int relation, Int2ObjectMap<IntSet> map) {
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
		int[] triple = new int[3];
		KB result = new KB();
		for (int subject : subject2relation2object.keySet()) {
			triple[0] = subject;
			Int2ObjectMap<IntSet> tail = subject2relation2object.get(subject);			
			for (int predicate : tail.keySet()) {
				triple[1] = predicate;
				for (int object : tail.get(predicate)) {
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
	public boolean delete(int subject, int predicate, int object) {
		if (contains(subject, predicate, object)) {
			decrease(subjectSize, subject);
			decrease(relationSize, predicate);
			decrease(objectSize, object);
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
			int s1,
			int s2,
			int s3,
			Int2ObjectMap<Int2ObjectMap<IntSet>> index) {
		Int2ObjectMap<IntSet> imap2 = index.get(s1);
		IntSet imap3 = imap2.get(s2);
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
			for(int relation: relationSize.keySet()){
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
		IntList ommittedRelations = IntArrays.asList(KB.map("rdf:type"),
				KB.map("rdfs:domain"), KB.map("rdfs:range"));
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("sample.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
                        System.exit(2);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
                        System.exit(2);
		}
		for (int relation : relationSize.keySet()) {
			if (ommittedRelations.contains(relation))
				continue;
			
			System.out.println(relation);
			IntHashMap<Integer> distribution = new IntHashMap<>();
			Int2ObjectMap<IntSet> theMap = null;
			boolean isFunctional = isFunctional(relation);
			if (isFunctional) {
				theMap = relation2subject2object.get(relation);
			} else {
				theMap = relation2object2subject.get(relation);
			}
			
			if (useSignatureTypes) {				
				IntSet instances = null;
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
				for (int instance : instances) {
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
			for (int argument : theMap.keySet()) {
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
		for (int subject : subject2relation2object.keySet()) {
			Int2ObjectMap<IntSet> iMap = subject2relation2object.get(subject);
			for (int relation : iMap.keySet()) {
				for (int object : iMap.get(relation)) {
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
	public boolean containsRelation(int relation) {
		return relationSize.keySet().contains(relation);
	}

	/**
	 * Outputs statistics about the types (classes) present in the KB.
	 */
	public void summarizeTypes() {
		if (relation2object2subject.containsKey(Schema.typeRelationBS)) {
			Int2ObjectMap<IntSet> map = 
					relation2object2subject.get(Schema.typeRelationBS);
			for (int type : map.keySet()) {
				System.out.println(type + "\t" + map.get(type).size());
			}
		}
	}
	
	public void loadDiff(File file, KB oldKB, int... exceptions)
			throws IOException {
		long size = size();
		IntList exceptionsList = IntArrays.asList(exceptions);
		if (file.isDirectory()) 
			throw new UnsupportedOperationException("Expected kb file, not a directory");
		for (String line : new FileLines(file, "UTF-8", null)) {
			if (line.endsWith("."))
				line = Char17.cutLast(line);
			String[] split = line.split(delimiter);
			if (split.length == 3) {
				if (exceptionsList.contains(KB.map(split[1].trim())) 
						|| !oldKB.contains(split[0].trim(), split[1].trim(), split[2].trim()))
					add(split[0].trim(), split[1].trim(), split[2].trim());
			} else if (split.length == 4) {
				if (exceptionsList.contains(KB.map(split[2].trim())) 
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
	public KB newEntitiesKB(IntSet oldEntities) {
		IntSet newEntities = relation2subject2object.get(Schema.typeRelationBS).keySet();
		int[] triple = new int[3];
		KB result = new KB();
		for (int subject : subject2relation2object.keySet()) {
			triple[0] = subject;
			Int2ObjectMap<IntSet> tail = subject2relation2object.get(subject);			
			for (int predicate : tail.keySet()) {
				triple[1] = predicate;
				for (int object : tail.get(predicate)) {
					triple[2] = object;
					if ((newEntities.contains(subject) && !oldEntities.contains(subject)) || (newEntities.contains(object) && !oldEntities.contains(object))) {
						result.add(triple);
					}
				}
			}
		}
		return result;
	}
        
    public Int2ObjectMap<IntSet> getMap(SignedPredicate sp) {
        if (sp.subject) {
            return this.relation2subject2object.get(sp.predicate);
        } else {
            return this.relation2object2subject.get(sp.predicate);
        }
    }
    
    public Int2IntMap getCount(SignedPredicate sp) {
        Int2IntMap result = new Int2IntOpenHashMap();
        increase(result, getMap(sp));
        return result;
    }
        
        public IntSet getRelationSet() {
            return new IntOpenHashSet(relationSize.keySet());
        }
        
        public IntSet getClassSet() {
            return null;
        }
	
	public static void main(String[] args) {
		System.out.println(Arrays.binarySearch(new int[]{3, 4}, 1));
		
	}
}