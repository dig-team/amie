package amie.data;

import amie.data.tuple.IntArrays;
import it.unimi.dsi.fastutil.ints.*;
import amie.data.javatools.datatypes.Pair;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static amie.data.U.increase;

/**
 * Abstract class to implement KB.
 */
public abstract class AbstractKB {

    protected static boolean enableLiveMetrics = false ;
    protected static String baseURL = null ;
    public static final int DEFAULT_PORT = 9092;
    public static int Port = DEFAULT_PORT ;
    public static final String DEFAULT_SERVER_ADDRESS = "localhost:" + DEFAULT_PORT ;
    public static String ServerAddress = DEFAULT_SERVER_ADDRESS ;

    private static final String WS_LAYER = "WS"  ;

    private static final List<String> Layers = List.of(
            WS_LAYER
            // Add other layer names here
    ) ;
    private static final Class<? extends AbstractKBClient> DEFAULT_CLIENT_COMMUNICATION_LAYER_TYPE = KBWebSocketClient.class;
    private static final Class<? extends KB> DEFAULT_SERVER_COMMUNICATION_LAYER_TYPE = KBWebSocketServer.class;

    public static String GetDefaultCommunicationLayerType() {
        return DEFAULT_CLIENT_COMMUNICATION_LAYER_TYPE.getName() ;
    }

    public static void SetServerAddress(String serverAddress){
        ServerAddress = serverAddress ;
        System.out.println("Set server address to "+serverAddress);
    }

    public static void SetPort(int port){
        Port = port ;
        System.out.println("Set port to "+port);
    }

    public static void EnableLiveMetrics() {
        enableLiveMetrics = true ;
    }

    /** NewKBServer was initially implemented to choose between several communication layer
    * implementations. To simplify AMIE's usage, this option has been removed and only WebSocket is
    * available. This function might be removed in the future if no use for it has been found.
     */
    public static KB NewKBServer(String args) {
        return new KBWebSocketServer(args) ;
    }

    /** NewKBClient was initially implemented to choose between several communication layer
     * implementations. To simplify AMIE's usage, this option has been removed and only WebSocket is
     * available. This function might be removed in the future if no use for it has been found.
     */
    public static AbstractKBClient NewKBClient(String args) {
        KBWebSocketClient.SetFormattedServerAddress();
        return  new KBWebSocketClient(args);
    }

    public Schema schema ;

    public IntCollection MapOptionValues(CommandLine cli, String option) {
        IntArrayList result = null ;
        if (cli.hasOption(option)) {
            result = new IntArrayList();
            String excludedValuesStr = cli.getOptionValue(option);
            String[] excludedValueArr = excludedValuesStr.split(",");
            for (String excludedValue : excludedValueArr) {
                result.add(this.map(excludedValue.trim()));
            }
        }
        return result ;
    }

    // ---------------------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------------------

    /** X transitiveType T predicate **/
    public static final String TRANSITIVETYPEstr = "transitiveType";

    public int TRANSITIVETYPEbs ;

    /** (X differentFrom Y Z ...) predicate */
    public static final String DIFFERENTFROMstr = "differentFrom";

    /** (X differentFrom Y Z ...) predicate */
    public int DIFFERENTFROMbs ;

    /** (X equals Y Z ...) predicate */
    public static final String EQUALSstr = "equals";

    /** (X equals Y Z ...) predicate */
    public int EQUALSbs ;

    /** r(X, y') exists for some y', predicate */
    public static final String EXISTSstr = "exists";

    /** r(X, y') exists for some y', predicate */
    public int EXISTSbs ;

    /** r(y', X) exists for some y', predicate */
    public static final String EXISTSINVstr = "existsInv";

    /** r(y', X) exists for some y', predicate */
    public int EXISTSINVbs ;

    /** r(X, y') does NOT exists for some y', predicate */
    public static final String NOTEXISTSstr = "~exists";

    /** r(X, y') does NOT exists for some y', predicate */
    public int NOTEXISTSbs ;

    /** r(y', X) does NOT exists for some y', predicate */
    public static final String NOTEXISTSINVstr = "~existsInv";

    /** r(y', X) does NOT exists for some y', predicate */
    public int NOTEXISTSINVbs ;

    public final IntList specialRelations = IntArrays.asList(TRANSITIVETYPEbs, DIFFERENTFROMbs,
            EQUALSbs, EXISTSbs, EXISTSINVbs, NOTEXISTSbs, NOTEXISTSINVbs);

    /** Identifiers for the overlap maps */
    public static final int SUBJECT2SUBJECT = 0;

    public static final int SUBJECT2OBJECT = 2;

    public static final int OBJECT2OBJECT = 4;

    public enum Column { Subject, Relation, Object };

    /** Pattern of a triple */
    public static final Pattern triplePattern = Pattern
            .compile("(\\w+)\\((\\??\\w+)\\s*,\\s*(\\??\\w+)\\)");

    private static final String uriPattern = "<?[-._\\w\\p{L}:/–'\\(\\),]+>?";
    /** We do not still support typed literals **/
    private static final String literalPattern = "\\\"?[-._\\w\\p{L}\\s,'–:/]+\\\"?(@\\w+)?";
    private static final String variablePattern = "\\?\\w+";

    /** Pattern of a triple */
    public static final Pattern amieTriplePattern = Pattern
            .compile("(" + variablePattern + "|" + uriPattern + ")\\s+(" + variablePattern + "|" + uriPattern +
                    ")\\s+(" + literalPattern + "|" + variablePattern + "|" + uriPattern + ")");


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

    protected void initMapping() {
        TRANSITIVETYPEbs = map(TRANSITIVETYPEstr) ;
        DIFFERENTFROMbs = map(DIFFERENTFROMstr);
        EQUALSbs = map(EQUALSstr);
        EXISTSbs = map(EXISTSstr);
        EXISTSINVbs = map(EXISTSINVstr);
        NOTEXISTSbs = map(NOTEXISTSstr);
        NOTEXISTSINVbs = map(NOTEXISTSINVstr);
    }



   // Utils

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
     * Functionality of a relation given the position.
     * @param relation
     * @param col Subject = functionality, Object = Inverse functionality
     * @return
     */
    public double colFunctionality(int relation, KB.Column col) {
        if (col == KB.Column.Subject)
            return functionality(relation);
        else if (col == KB.Column.Object)
            return inverseFunctionality(relation);
        else
            return -1.0;
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

   public static boolean isVariable(int s) {
       return (s < 0 && s > -2048);
   }

    public static int numVariables(int... fact) {
        int counter = 0;
        for (int i = 0; i < fact.length; i++)
            if (isVariable(fact[i]))
                counter++;
        return (counter);
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

    public int compress(CharSequence s) {
        return map(s);
    }


    /** Makes a list of triples */
    public static List<int[]> triples(int[]... triples) {
        return (Arrays.asList(triples));
    }

    /** makes triples */
    @SuppressWarnings("unchecked")
    public List<int[]> triples(
            List<? extends CharSequence[]> triples) {
        List<int[]> t = new ArrayList<>();
        for (CharSequence[] c : triples)
            t.add(triple(c));
        return (t);
    }

    /** returns the instances that fulfill a certain condition */
    public IntSet selectDistinct(CharSequence variable,
                                 List<CharSequence[]> query) {
        return (selectDistinct(compress(variable), triples(query)));
    }

    /** ToString for a triple */
    public String toString(int[] s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length; i++)
            b.append(unmap(s[i])).append(" ");
        return (b.toString());
    }

    /** ToString for a triple */
    public static <T> String toString(T[] s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length; i++)
            b.append(s[i]).append(" ");
        return (b.toString());
    }

    /** ToString for a query */
    public String toString(List<int[]> s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.size(); i++)
            b.append(toString(s.get(i))).append(" ");
        return (b.toString());
    }

    /**
     * Parses a rule of the form triple &amp; triple &amp; ... =&gt; triple or triple :-
     * triple &amp; triple &amp; ...
     * @return A pair where the first element is the list of body atoms (left-hand side
     * of the rule) and the second element is triple pattern, the head of the rule.
     */
    public Pair<List<int[]>, int[]> rule(String s) {
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
     * It parses a Datalog query with atoms of the form r(x,y) and turns into a list of
     * AMIE triples of the form [x, r, y].
     * @param s
     * @return
     **/
    public ArrayList<int[]> triples(String s) {
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



    // KB methods
    /**
     * Returns the number of facts in the KB.
     **/
    public abstract long size() ;

    /* --- Abstract Queries --- */

    /**
     * Counts the number of instances of the projection triple that exist in
     * joins with the other triples
     */
    public abstract long countProjection(int[] projectionTriple,
                                List<int[]> otherTriples) ;

    /**
     * For each instantiation of variable, it returns the number of instances of
     * the projectionTriple satisfy the query. The projection triple can have
     * either one or two variables.
     *
     * @return IntHashMap A map of the form {string : frequency}
     **/
    public abstract Int2IntMap countProjectionBindings(int[] projectionTriple, List<int[]> otherTriples, int variable) ;


    /** returns the number of instances that fulfill a certain condition */
    public abstract long countDistinct(int variable, List<int[]> query) ;
    /**
     * returns the number of distinct pairs (var1,var2) for the query
     */
    public abstract long countDistinctPairs(int var1, int var2,
                                   List<int[]> query) ;



    /** returns the instances that fulfill a certain condition */
    public abstract IntSet selectDistinct(int variable,
                                 List<int[]> query) ;

    /**
     * returns the number of distinct pairs (var1,var2) for the query
     */
    public abstract long countDistinctPairsUpTo(long upperBound, int var1, int var2,
                                       List<int[]> query);

    public abstract long countDistinctPairsUpToWithIterator(long upperBound, int var1,
                                                   int var2, List<int[]> query);

    /**
     * Get a collection with all the relations of the KB.
     * @return
     */
    public abstract IntCollection getRelations() ;

    /** returns number of instances of this triple */
    public abstract long count(int... triple) ;

//    /**
//     * It returns TRUE if the database contains this fact (no variables). If the fact
//     * containst meta-relations (e.g. differentFrom, equals, exists), it returns TRUE
//     * if the expression evaluates to TRUE.
//     * @param fact A triple without variables, e.g., [Barack_Obama, wasBornIn, Hawaii]
//     **/
//    public abstract boolean contains(CharSequence... fact) ;

    /**
     * For each instantiation of variable, it returns the number of different
     * instances of projectionVariable that satisfy the query.
     *
     * @return IntHashMap A map of the form {string : frequency}
     **/
    public abstract Int2IntMap frequentBindingsOf(int variable,
                                         int projectionVariable, List<int[]> query) ;

    /**
     * Determines whether a relation is functional, i.e., its harmonic functionality
     * is greater than its inverse harmonic functionality.
     * @param relation
     * @return
     */
    public abstract boolean isFunctional(int relation) ;

    /**
     * It returns the functionality of a relation.
     * @param relation
     * @return
     */
    public abstract double functionality(int relation) ;

    /**
     * It returns the inverse functionality of a relation.
     * @param relation
     * @return
     */
    public abstract double inverseFunctionality(int relation) ;

    /**
     * It returns the number of distinct instance of one of the arguments (columns)
     * of a relation.
     * @param relation
     * @param column
     * @return
     */
    public abstract int relationColumnSize(int relation, KB.Column column);

    /**
     * Given two relations, it returns the number of entities in common (the overlap) between
     * two of their columns
     * @param relation1
     * @param relation2
     * @param overlap 0 = Subject-Subject, 2 = Subject-Object, 4 = Object-Object
     * @return
     */
    public abstract int overlap(int relation1, int relation2, int overlap) ;

    /**
     * Returns the number of distinct results of the triple pattern query
     * with 1 variable.
     **/
    public abstract long countOneVariable(int... triple) ;

    /**
     * It returns the number of facts of a relation in the KB.
     * @param relation
     * @return
     */
    public abstract int relationSize(int relation) ;

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
    public abstract int maximalRightCumulativeCardinality(int relation, long threshold, int limit) ;

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
    public abstract int maximalRightCumulativeCardinalityInv(int relation, long threshold, int limit) ;

    /**
     * Returns the maximal number of values smaller than limit than
     * an entity can have for the given relation.
     * @param relation
     * @param limit
     * @return
     */
    public abstract int maximalCardinality(int relation, int limit);

    /**
     * Returns the maximal number of values an entity can have for the given relation.
     * @param relation
     * @return
     */
    public abstract int maximalCardinality(int relation) ;


    public abstract int maximalCardinalityInv(int relation, int limit) ;
    public abstract int maximalCardinalityInv(int relation) ;

    public abstract int map(String e);
    public abstract int map(CharSequence e);
    public abstract String unmap(int e);
    public abstract int[] triple(CharSequence s, CharSequence p, CharSequence o);
    public abstract int[] triple(CharSequence... triple);

    /**
     * Returns server configuration
     * @return KG identifier and options
     */
    public abstract String getServerConfiguration() ;
}
