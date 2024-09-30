package amie.data;

import amie.data.remote.Queries;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static amie.data.remote.Queries.*;


/**
 * Abstract class to implement KB clients.
 */
public abstract class AbstractKBClient extends AbstractKB {

    protected abstract String getResponse(String jsonQuery, String channel);

    private <R> R runRemoteQuery(IQuerySchema query, String channel, Class<R> responseClass) {

        String jsonQuery = null;

        try {
            // Query to JSON
            jsonQuery = gson.toJson(query);
        } catch (Exception e) {
            System.err.println("Failed to convert query to JSON: " + e);
            System.exit(1);
        }

        String JSONResponse = getResponse(jsonQuery, channel);

        R response = null;
        try {
            // JSON to response object
            response = gson.fromJson(JSONResponse, responseClass);
        } catch (JsonSyntaxException e) {
            System.err.format("Q: %s \n CHANNEL: %s \n RCLASS: %s \n RESPONSE: %s\n",
                    gson.toJson(query, QueriesLinkedHashMap.get(channel)), channel, responseClass.getName(),
                    JSONResponse);
            System.err.format("Generated Cache Key : %s\n",
                    Queries.GenerateCacheKey(channel, jsonQuery));
            System.err.println("Failed to read response: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        return response;
    }

    public static class IntSetDeserializer implements JsonDeserializer<IntSet> {
        @Override
        public IntSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Gson gson = new Gson();
            int[] arr = gson.fromJson(json, int[].class);
            return new IntOpenHashSet(arr);
        }
    }

    public static class Int2IntMapDeserializer implements JsonDeserializer<Int2IntMap> {
        @Override
        public Int2IntMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<Integer, Integer>>() {
            }.getType();
            Map<Integer, Integer> map = gson.fromJson(json, mapType);

            return new Int2IntOpenHashMap(map);
        }
    }

    public static class IntCollectionDeserializer implements JsonDeserializer<IntCollection> {

        @Override
        public IntCollection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Gson gson = new Gson();
            int[] arr = gson.fromJson(json, int[].class);
            return new IntArrayList(arr);
        }

    }

    @Override
    public String getServerConfiguration() {
        GetServerConfigurationQuery query = new GetServerConfigurationQuery() ;
        return runRemoteQuery(query, GetServerConfigurationQueryName, String.class) ;
    }

    @Override
    public long size() {
        SizeQuery query = new SizeQuery() ;
        return runRemoteQuery(query, SizeQueryName, Long.class);
    }


    @Override
    public long countProjection(int[] projectionTriple, List<int[]> otherTriples) {
        CountProjectionQuery query = new CountProjectionQuery(projectionTriple, otherTriples);
        return runRemoteQuery(query, CountProjectionQueryName, Long.class);
    }

    public Int2IntMap countProjectionBindings(int[] projectionTriple, List<int[]> otherTriples, int variable) {
        CountProjectionBindingsQuery query = new CountProjectionBindingsQuery(projectionTriple, otherTriples,
                variable);
        return runRemoteQuery(query, CountProjectionBindingsQueryName, Int2IntMap.class);
    }

    @Override
    public long countDistinct(int variable, List<int[]> query) {
        CountDistinctQuery request = new CountDistinctQuery(variable, query);
        return runRemoteQuery(request, CountDistinctQueryName, Long.class);
    }

    @Override
    public long countDistinctPairs(int var1, int var2, List<int[]> query) {
        CountDistinctPairsQuery request = new CountDistinctPairsQuery(var1, var2, query);
        return runRemoteQuery(request, CountDistinctPairsQueryName, Long.class);
    }

    @Override
    public IntSet selectDistinct(int variable, List<int[]> query) {
        SelectDistinctQuery request = new SelectDistinctQuery(variable, query);
        return runRemoteQuery(request, SelectDistinctQueryName, IntSet.class);
    }

    @Override
    public long countDistinctPairsUpTo(long upperBound, int var1, int var2, List<int[]> query) {
        CountDistinctPairsUpToQuery request = new CountDistinctPairsUpToQuery(upperBound, var1, var2, query);
        return runRemoteQuery(request, CountDistinctPairsUpToQueryName, Long.class);
    }

    @Override
    public long countDistinctPairsUpToWithIterator(long upperBound, int var1, int var2, List<int[]> query) {
        CountDistinctPairsUpToWithIteratorQuery request = new CountDistinctPairsUpToWithIteratorQuery(upperBound, var1,
                var2, query);
        return runRemoteQuery(request, CountDistinctPairsUpToWithIteratorQueryName, Long.class);
    }

    @Override
    public IntCollection getRelations() {
        GetRelationsQuery request = new GetRelationsQuery();
        return runRemoteQuery(request, GetRelationsQueryName, IntCollection.class);
    }

    @Override
    public long count(int... triple) {
        CountQuery request = new CountQuery(triple);
        return runRemoteQuery(request, CountQueryName, Long.class);
    }

    @Override
    public Int2IntMap frequentBindingsOf(int variable, int projectionVariable, List<int[]> query) {
        FrequentBindingsOfQuery request = new FrequentBindingsOfQuery(variable, projectionVariable, query);
        return runRemoteQuery(request, FrequentBindingsOfQueryName, Int2IntMap.class);
    }

    @Override
    public boolean isFunctional(int relation) {
        IsFunctionalQuery request = new IsFunctionalQuery(relation);
        return runRemoteQuery(request, IsFunctionalQueryName, Boolean.class);
    }

    @Override
    public double functionality(int relation) {
        FunctionalityQuery request = new FunctionalityQuery(relation);
        return runRemoteQuery(request, FunctionalityQueryName, Double.class);
    }

    @Override
    public double inverseFunctionality(int relation) {
        InverseFunctionalityQuery request = new InverseFunctionalityQuery(relation);
        return runRemoteQuery(request, InverseFunctionalityQueryName, Double.class);
    }

    @Override
    public int relationColumnSize(int relation, Column column) {
        RelationColumnSizeQuery request = new RelationColumnSizeQuery(relation, column);
        return runRemoteQuery(request, RelationColumnSizeQueryName, Integer.class);
    }

    @Override
    public int overlap(int relation1, int relation2, int overlap) {
        OverlapQuery request = new OverlapQuery(relation1, relation2, overlap);
        return runRemoteQuery(request, OverlapQueryName, Integer.class);
    }

    @Override
    public long countOneVariable(int... triple) {
        CountOneVariableQuery request = new CountOneVariableQuery(triple);
        return runRemoteQuery(request, CountOneVariableQueryName, Integer.class);
    }

    @Override
    public int relationSize(int relation) {
        RelationSizeQuery request = new RelationSizeQuery(relation);
        return runRemoteQuery(request, RelationSizeQueryName, Integer.class);
    }

    @Override
    public int maximalRightCumulativeCardinality(int relation, long threshold, int limit) {
        MaximalRightCumulativeCardinalityQuery request = new MaximalRightCumulativeCardinalityQuery(relation, threshold,
                limit);
        return runRemoteQuery(request, MaximalRightCumulativeCardinalityQueryName, Integer.class);
    }

    @Override
    public int maximalRightCumulativeCardinalityInv(int relation, long threshold, int limit) {
        MaximalRightCumulativeCardinalityInvQuery request = new MaximalRightCumulativeCardinalityInvQuery(relation,
                threshold, limit);
        return runRemoteQuery(request, MaximalRightCumulativeCardinalityInvQueryName, Integer.class);
    }

    @Override
    public int maximalCardinality(int relation, int limit) {
        MaximalCardinalityWithLimitQuery request = new MaximalCardinalityWithLimitQuery(relation, limit);
        return runRemoteQuery(request, MaximalCardinalityWithLimitQueryName, Integer.class);
    }

    @Override
    public int maximalCardinality(int relation) {
        MaximalCardinalityQuery request = new MaximalCardinalityQuery(relation);
        return runRemoteQuery(request, MaximalCardinalityQueryName, Integer.class);
    }

    @Override
    public int maximalCardinalityInv(int relation, int limit) {
        MaximalCardinalityInvWithLimitQuery request = new MaximalCardinalityInvWithLimitQuery(relation, limit);
        return runRemoteQuery(request, MaximalCardinalityInvWithLimitQueryName, Integer.class);
    }

    @Override
    public int maximalCardinalityInv(int relation) {
        MaximalCardinalityInvQuery request = new MaximalCardinalityInvQuery(relation);
        return runRemoteQuery(request, MaximalCardinalityInvQueryName, Integer.class);
    }

    @Override
    public int map(String e) {
        if (Schema.isVariable(e)) {
            return Schema.parseVariable(e);
        }
        MapQuery request = new MapQuery(e);
        return runRemoteQuery(request, MapQueryName, Integer.class);
    }

    @Override
    public int map(CharSequence e) {
        if (Schema.isVariable(e)) {
            return Schema.parseVariable(e);
        }
        MapCharSequenceQuery request = new MapCharSequenceQuery(e);
        return runRemoteQuery(request, MapCharSequenceQueryName, Integer.class);
    }

    @Override
    public String unmap(int e) {
        if (Schema.isVariable(e)) {
            return Schema.unparseVariable(e);
        }
        UnmapQuery request = new UnmapQuery(e);
        return runRemoteQuery(request, UnmapQueryName, String.class);
    }

    @Override
    public int[] triple(CharSequence s, CharSequence p, CharSequence o) {
        TripleQuery request = new TripleQuery(s, p, o);
        return runRemoteQuery(request, TripleQueryName, int[].class);
    }

    @Override
    public int[] triple(CharSequence... triple) {
        TripleArrayQuery request = new TripleArrayQuery(triple);
        return runRemoteQuery(request, TripleArrayQueryName, int[].class);
    }


}
