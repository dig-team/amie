package amie.data.remote;

import amie.data.AbstractKBClient;
import amie.data.KB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Contains various utilities to manage queries.
 */
public abstract class Queries {

    // Key generation
    private static final String DIGEST_ALG = "SHA-256";

    /**
     * Generate a deterministic key from topic name and JSON query associated with response in cache.
     *
     * @param topic
     * @param jsonQuery
     * @return
     */
    public static String GenerateCacheKey(String topic, String jsonQuery) {
        String keyString = topic + jsonQuery;
        String cacheKey = null;
        try {
            cacheKey = Base64.getUrlEncoder().encodeToString(MessageDigest.getInstance(DIGEST_ALG).digest(
                    keyString.getBytes(StandardCharsets.UTF_8)

            ));
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to generate cache key");
            e.printStackTrace();
            System.exit(1);
        }
        return cacheKey;
    }

    /**
     * Interface to implement query schemas.
     */
    public interface IQuerySchema {

    }

    /**
     * Wraps content along with query type and cache key, ready to be serialized to JSON and sent over network.
     */
    static public class Payload {
        public String cacheKey, queryType, jsonContent = null;

        public Payload(String cacheKey, String queryType, String jsonContent) {
            this.cacheKey = cacheKey;
            this.queryType = queryType;
            this.jsonContent = jsonContent;
        }
    }

    /**
     * Used to serialize/deserialize queries, responses and payloads to/from JSON.
     */
    public static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(IntSet.class, new AbstractKBClient.IntSetDeserializer())
            .registerTypeAdapter(Int2IntMap.class, new AbstractKBClient.Int2IntMapDeserializer())
            .registerTypeAdapter(IntCollection.class, new AbstractKBClient.IntCollectionDeserializer())
            .create();

    /**
     * Generated Payload instance from JSON.
     *
     * @param jsonPayload JSON to convert to Payload instance.
     * @return Payload instance
     */
    static public Payload UnmarhsalPayload(String jsonPayload) {
        return gson.fromJson(jsonPayload, Payload.class);
    }

    /**
     * Generated JSON String from Payload instance.
     *
     * @param jsonPayload Payload instance to convert to JSON String.
     * @return JSON String
     */
    static public String MarhsalPayload(Payload jsonPayload) {
        return gson.toJson(jsonPayload, Payload.class);
    }

    static public class CountProjectionBindingsQuery implements IQuerySchema {

        public int[] projectionTriple;
        public List<int[]> otherTriples;
        public int variable;

        public CountProjectionBindingsQuery(int[] projectionTriple, List<int[]> otherTriples, int variable) {
            this.projectionTriple = projectionTriple;
            this.otherTriples = otherTriples;
            this.variable = variable;
        }
    }

    static public class CountProjectionQuery implements IQuerySchema {
        public int[] projectionTriple;
        public List<int[]> otherTriples;

        public CountProjectionQuery(int[] projectionTriple, List<int[]> otherTriples) {
            this.projectionTriple = projectionTriple;
            this.otherTriples = otherTriples;
        }
    }

    static public class CountDistinctQuery implements IQuerySchema {
        public int variable;
        public List<int[]> query;

        public CountDistinctQuery(int variable, List<int[]> query) {
            this.variable = variable;
            this.query = query;
        }
    }

    static public class CountDistinctPairsQuery implements IQuerySchema {
        public int var1;
        public int var2;
        public List<int[]> query;

        public CountDistinctPairsQuery(int var1, int var2, List<int[]> query) {
            this.var1 = var1;
            this.var2 = var2;
            this.query = query;
        }
    }


    static public class SelectDistinctQuery implements IQuerySchema {

        public int variable;
        public List<int[]> query;

        public SelectDistinctQuery(int variable, List<int[]> query) {
            this.variable = variable;
            this.query = query;
        }
    }

    static public class CountDistinctPairsUpToQuery implements IQuerySchema {
        public long upperBound;
        public int var1;
        public int var2;
        public List<int[]> query;

        public CountDistinctPairsUpToQuery(long upperBound, int var1, int var2, List<int[]> query) {
            this.upperBound = upperBound;
            this.var1 = var1;
            this.var2 = var2;
            this.query = query;
        }
    }

    static public class CountDistinctPairsUpToWithIteratorQuery implements IQuerySchema {
        public long upperBound;
        public int var1;
        public int var2;
        public List<int[]> query;

        public CountDistinctPairsUpToWithIteratorQuery(long upperBound, int var1, int var2, List<int[]> query) {
            this.upperBound = upperBound;
            this.var1 = var1;
            this.var2 = var2;
            this.query = query;
        }

    }

    static public class GetRelationsQuery implements IQuerySchema {

    }

    static public class CountQuery implements IQuerySchema {
        public int[] triple;

        public CountQuery(int... triple) {
            this.triple = triple;
        }
    }

//    static public class ContainsQuery implements IQuerySchema {
//        public CharSequence[] fact;
//
//        public ContainsQuery(CharSequence... fact) {
//            this.fact = fact;
//        }
//    }

    static public class FrequentBindingsOfQuery implements IQuerySchema {
        public int variable;
        public int projectionVariable;
        public List<int[]> query;

        public FrequentBindingsOfQuery(int variable, int projectionVariable, List<int[]> query) {
            this.variable = variable;
            this.projectionVariable = projectionVariable;
            this.query = query;
        }
    }

    static public class IsFunctionalQuery implements IQuerySchema {
        public int relation;

        public IsFunctionalQuery(int relation) {
            this.relation = relation;
        }
    }

    static public class FunctionalityQuery implements IQuerySchema {
        public int relation;

        public FunctionalityQuery(int relation) {
            this.relation = relation;
        }
    }

    static public class InverseFunctionalityQuery implements IQuerySchema {
        public int relation;

        public InverseFunctionalityQuery(int relation) {
            this.relation = relation;
        }
    }

    static public class RelationColumnSizeQuery implements IQuerySchema {
        public int relation;
        public KB.Column column;

        public RelationColumnSizeQuery(int relation, KB.Column column) {
            this.relation = relation;
            this.column = column;
        }
    }

    static public class OverlapQuery implements IQuerySchema {
        public int relation1;
        public int relation2;
        public int overlap;

        public OverlapQuery(int relation1, int relation2, int overlap) {
            this.relation1 = relation1;
            this.relation2 = relation2;
            this.overlap = overlap;
        }
    }

    static public class CountOneVariableQuery implements IQuerySchema {
        public int[] triple;

        public CountOneVariableQuery(int[] triple) {
            this.triple = triple;
        }
    }

    static public class RelationSizeQuery implements IQuerySchema {
        public int relation;

        public RelationSizeQuery(int relation) {
            this.relation = relation;
        }
    }


    static public class MaximalRightCumulativeCardinalityQuery implements IQuerySchema {
        public int relation;
        public int limit;
        public long threshold;

        public MaximalRightCumulativeCardinalityQuery(int relation, long threshold, int limit) {
            this.relation = relation;
            this.threshold = threshold;
            this.limit = limit;
        }
    }

    static public class MaximalRightCumulativeCardinalityInvQuery implements IQuerySchema {
        public int relation;
        public int limit;
        public long threshold;

        public MaximalRightCumulativeCardinalityInvQuery(int relation, long threshold, int limit) {
            this.relation = relation;
            this.threshold = threshold;
            this.limit = limit;
        }
    }

    static public class MaximalCardinalityWithLimitQuery implements IQuerySchema {
        public int relation;
        public int limit;

        public MaximalCardinalityWithLimitQuery(int relation, int limit) {
            this.relation = relation;
            this.limit = limit;
        }
    }

    static public class MaximalCardinalityQuery implements IQuerySchema {
        public int relation;

        public MaximalCardinalityQuery(int relation) {
            this.relation = relation;
        }
    }

    static public class MaximalCardinalityInvWithLimitQuery implements IQuerySchema {
        public int relation;
        public int limit;

        public MaximalCardinalityInvWithLimitQuery(int relation, int limit) {
            this.relation = relation;
            this.limit = limit;
        }
    }

    static public class MaximalCardinalityInvQuery implements IQuerySchema {
        public int relation;

        public MaximalCardinalityInvQuery(int relation) {
            this.relation = relation;
        }

    }

    static public class MapQuery implements IQuerySchema {
        public String e;

        public MapQuery(String e) {
            this.e = e;
        }
    }

    // TODO check if compatible with MapQuery
    static public class MapCharSequenceQuery implements IQuerySchema {
        public CharSequence e;

        public MapCharSequenceQuery(CharSequence e) {
            this.e = e;
        }
    }

    static public class UnmapQuery implements IQuerySchema {
        public int e;

        public UnmapQuery(int e) {
            this.e = e;
        }
    }

    static public class TripleQuery implements IQuerySchema {
        public CharSequence s;
        public CharSequence p;
        public CharSequence o;

        public TripleQuery(CharSequence s, CharSequence p, CharSequence o) {
            this.s = s;
            this.p = p;
            this.o = o;
        }
    }

    static public class TripleArrayQuery implements IQuerySchema {
        public CharSequence[] triple;

        public TripleArrayQuery(CharSequence... triple) {
            this.triple = triple;
        }
    }

    // Query names
    static public String CountProjectionBindingsQueryName = "CountProjectionBindings";
    static public String CountProjectionQueryName = "CountProjection";
    static public String CountDistinctQueryName = "CountDistinct";
    static public String CountDistinctPairsQueryName = "CountDistinctPairs";
    static public String SelectDistinctQueryName = "SelectDistinct";
    static public String CountDistinctPairsUpToQueryName = "CountDistinctPairsUpTo";
    static public String CountDistinctPairsUpToWithIteratorQueryName = "CountDistinctPairsUpToWithIterator";
    static public String GetRelationsQueryName = "GetRelations";
    static public String CountQueryName = "Count";
//    static public String ContainsQueryName = "Contains";
    static public String FrequentBindingsOfQueryName = "FrequentBindingOf";
    static public String IsFunctionalQueryName = "IsFunctional";
    static public String FunctionalityQueryName = "Functionality";
    static public String InverseFunctionalityQueryName = "InverseFunctionality";
    static public String RelationColumnSizeQueryName = "RelationColumnSize";
    static public String OverlapQueryName = "Overlap";
    static public String CountOneVariableQueryName = "CountOneVariable";
    static public String RelationSizeQueryName = "RelationSize";
    static public String MaximalRightCumulativeCardinalityQueryName = "MaximalRightCumulativeCardinality";
    static public String MaximalRightCumulativeCardinalityInvQueryName = "MaximalRightCumulativeCardinalityInv";
    static public String MaximalCardinalityWithLimitQueryName = "MaximalCardinalityWithLimit";
    static public String MaximalCardinalityQueryName = "MaximalCardinality";
    static public String MaximalCardinalityInvWithLimitQueryName = "MaximalCardinalityInvWithLimit";
    static public String MaximalCardinalityInvQueryName = "MaximalCardinalityInv";
    static public String MapQueryName = "Map";
    static public String MapCharSequenceQueryName = "MapCharSequence";
    static public String UnmapQueryName = "Unmap";
    static public String TripleQueryName = "Triple";
    static public String TripleArrayQueryName = "TripleArray";

    static public final List<String> QueryList = List.of(
            CountProjectionBindingsQueryName, CountProjectionQueryName, CountDistinctQueryName,
            CountDistinctPairsQueryName, SelectDistinctQueryName, CountDistinctPairsUpToQueryName,
            CountDistinctPairsUpToWithIteratorQueryName, GetRelationsQueryName, CountQueryName,
//            ContainsQueryName,
            FrequentBindingsOfQueryName, IsFunctionalQueryName,
            FunctionalityQueryName, InverseFunctionalityQueryName, RelationColumnSizeQueryName,
            OverlapQueryName, CountOneVariableQueryName, RelationSizeQueryName,
            MaximalRightCumulativeCardinalityQueryName, MaximalRightCumulativeCardinalityInvQueryName,
            MaximalCardinalityWithLimitQueryName, MaximalCardinalityQueryName,
            MaximalCardinalityInvWithLimitQueryName, MaximalCardinalityInvQueryName, MapQueryName,
            MapCharSequenceQueryName, UnmapQueryName, TripleQueryName, TripleArrayQueryName
    );

    // Response topic names
    static public String ResponseTopic = "Response";

    static public final LinkedHashMap<String, Class<? extends IQuerySchema>> QueriesLinkedHashMap = new LinkedHashMap<>(
            Map.ofEntries(
                    entry(CountProjectionBindingsQueryName, CountProjectionBindingsQuery.class),
                    entry(CountProjectionQueryName, CountProjectionQuery.class),
                    entry(CountDistinctQueryName, CountDistinctQuery.class),
                    entry(CountDistinctPairsQueryName, CountDistinctPairsQuery.class),
                    entry(SelectDistinctQueryName, SelectDistinctQuery.class),
                    entry(CountDistinctPairsUpToQueryName, CountDistinctPairsUpToQuery.class),
                    entry(CountDistinctPairsUpToWithIteratorQueryName, CountDistinctPairsUpToWithIteratorQuery.class),
                    entry(GetRelationsQueryName, GetRelationsQuery.class),
                    entry(CountQueryName, CountQuery.class),
//                    entry(ContainsQueryName, ContainsQuery.class),
                    entry(FrequentBindingsOfQueryName, FrequentBindingsOfQuery.class),
                    entry(IsFunctionalQueryName, IsFunctionalQuery.class),
                    entry(FunctionalityQueryName, FunctionalityQuery.class),
                    entry(InverseFunctionalityQueryName, InverseFunctionalityQuery.class),
                    entry(RelationColumnSizeQueryName, RelationColumnSizeQuery.class),
                    entry(OverlapQueryName, OverlapQuery.class),
                    entry(CountOneVariableQueryName, CountOneVariableQuery.class),
                    entry(RelationSizeQueryName, RelationSizeQuery.class),
                    entry(MaximalRightCumulativeCardinalityQueryName, MaximalRightCumulativeCardinalityQuery.class),
                    entry(MaximalRightCumulativeCardinalityInvQueryName, MaximalRightCumulativeCardinalityInvQuery.class),
                    entry(MaximalCardinalityWithLimitQueryName, MaximalCardinalityWithLimitQuery.class),
                    entry(MaximalCardinalityQueryName, MaximalCardinalityQuery.class),
                    entry(MaximalCardinalityInvWithLimitQueryName, MaximalCardinalityInvWithLimitQuery.class),
                    entry(MaximalCardinalityInvQueryName, MaximalCardinalityInvQuery.class),
                    entry(MapQueryName, MapQuery.class),
                    entry(MapCharSequenceQueryName, MapCharSequenceQuery.class),
                    entry(UnmapQueryName, UnmapQuery.class),
                    entry(TripleQueryName, TripleQuery.class),
                    entry(TripleArrayQueryName, TripleArrayQuery.class)
            )
    );


}
