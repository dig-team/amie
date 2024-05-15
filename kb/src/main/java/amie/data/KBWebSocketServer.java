package amie.data;

import amie.data.remote.Caching;
import amie.data.remote.QueryProcessing;
import amie.data.remote.Utils;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
//import org.eclipse.jetty.websocket.api.Session;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
//import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.data.remote.Queries.*;
import static java.util.Map.entry;
//import static spark.Spark.*;


public class KBWebSocketServer extends KB {

    public AbstractKB kb;


    public KBWebSocketServer() {
        super();
        this.kb = this;
        setupServer();
    }

    public KBWebSocketServer(Schema schema) {
        super(schema);
        this.kb = this;
        setupServer();
    }



    private interface WebSocketHandlerInterface {
        String webSocketHandler(String request) throws Exception;
    }

    private final LinkedHashMap<String, WebSocketHandlerInterface> handlers = new LinkedHashMap<>(
            Map.ofEntries(
                    entry(CountProjectionBindingsQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountProjectionBindingsQuery, Int2IntMap> queryFunction =
                                        (requestClass) -> kb.countProjectionBindings(
                                                requestClass.projectionTriple,
                                                requestClass.otherTriples,
                                                requestClass.variable
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountProjectionBindingsQuery.class));
                            }),
                    entry(CountProjectionQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountProjectionQuery, Long> queryFunction =
                                        (requestClass) -> kb.countProjection(
                                                requestClass.projectionTriple,
                                                requestClass.otherTriples
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountProjectionQuery.class));
                            }),
                    entry(CountDistinctQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountDistinctQuery, Long> queryFunction =
                                        (requestClass) -> kb.countDistinct(
                                                requestClass.variable,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountDistinctQuery.class));
                            }),
                    entry(CountDistinctPairsQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountDistinctPairsQuery, Long> queryFunction =
                                        (requestClass) -> kb.countDistinctPairs(
                                                requestClass.var1,
                                                requestClass.var2,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountDistinctPairsQuery.class));
                            }),
                    entry(SelectDistinctQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<SelectDistinctQuery, IntSet> queryFunction =
                                        (requestClass) -> kb.selectDistinct(
                                                requestClass.variable,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        SelectDistinctQuery.class));
                            }),
                    entry(CountDistinctPairsUpToQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountDistinctPairsUpToQuery, Long> queryFunction =
                                        (requestClass) -> kb.countDistinctPairsUpTo(
                                                requestClass.upperBound,
                                                requestClass.var1,
                                                requestClass.var2,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountDistinctPairsUpToQuery.class));
                            }),
                    entry(CountDistinctPairsUpToWithIteratorQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountDistinctPairsUpToWithIteratorQuery, Long> queryFunction =
                                        (requestClass) -> kb.countDistinctPairsUpToWithIterator(
                                                requestClass.upperBound,
                                                requestClass.var1,
                                                requestClass.var2,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountDistinctPairsUpToWithIteratorQuery.class));
                            }),
                    entry(GetRelationsQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<GetRelationsQuery, IntCollection> queryFunction =
                                        (requestClass) -> kb.getRelations();
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        GetRelationsQuery.class));
                            }),
                    entry(CountQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountQuery, Long> queryFunction =
                                        (requestClass) -> kb.count(
                                                requestClass.triple
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountQuery.class));
                            }),
//                    entry(ContainsQueryName,
//                            (req) ->
//                            {
//                                QueryProcessing.IQueryFunction<ContainsQuery, Boolean> queryFunction =
//                                        (requestClass) -> kb.contains(
//                                                requestClass.fact
//                                        );
//                                return (QueryProcessing.processQuery(req, queryFunction,
//                                        ContainsQuery.class));
//                            }),
                    entry(FrequentBindingsOfQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<FrequentBindingsOfQuery, Int2IntMap> queryFunction =
                                        (requestClass) -> kb.frequentBindingsOf(
                                                requestClass.variable,
                                                requestClass.projectionVariable,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        FrequentBindingsOfQuery.class));
                            }),
                    entry(IsFunctionalQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<IsFunctionalQuery, Boolean> queryFunction =
                                        (requestClass) -> kb.isFunctional(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        IsFunctionalQuery.class));
                            }),
                    entry(FunctionalityQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<FunctionalityQuery, Double> queryFunction =
                                        (requestClass) -> kb.functionality(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        FunctionalityQuery.class));
                            }),
                    entry(InverseFunctionalityQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<InverseFunctionalityQuery, Double> queryFunction =
                                        (requestClass) -> kb.inverseFunctionality(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        InverseFunctionalityQuery.class));
                            }),
                    entry(RelationColumnSizeQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<RelationColumnSizeQuery, Integer> queryFunction =
                                        (requestClass) -> kb.relationColumnSize(
                                                requestClass.relation,
                                                requestClass.column
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        RelationColumnSizeQuery.class));
                            }),
                    entry(OverlapQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<OverlapQuery, Integer> queryFunction =
                                        (requestClass) -> kb.overlap(
                                                requestClass.relation1,
                                                requestClass.relation2,
                                                requestClass.overlap
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        OverlapQuery.class));
                            }),
                    entry(CountOneVariableQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountOneVariableQuery, Long> queryFunction =
                                        (requestClass) -> kb.countOneVariable(
                                                requestClass.triple
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountOneVariableQuery.class));
                            }),
                    entry(RelationSizeQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<RelationSizeQuery, Integer> queryFunction =
                                        (requestClass) -> kb.relationSize(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        RelationSizeQuery.class));
                            }),
                    entry(MaximalRightCumulativeCardinalityQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalRightCumulativeCardinalityQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalRightCumulativeCardinality(
                                                requestClass.relation,
                                                requestClass.threshold,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalRightCumulativeCardinalityQuery.class));
                            }),
                    entry(MaximalRightCumulativeCardinalityInvQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalRightCumulativeCardinalityInvQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalRightCumulativeCardinalityInv(
                                                requestClass.relation,
                                                requestClass.threshold,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalRightCumulativeCardinalityInvQuery.class));
                            }),
                    entry(MaximalCardinalityWithLimitQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityWithLimitQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinality(
                                                requestClass.relation,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityWithLimitQuery.class));
                            }),
                    entry(MaximalCardinalityQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinality(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityQuery.class));
                            }),
                    entry(MaximalCardinalityInvWithLimitQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityInvWithLimitQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinalityInv(
                                                requestClass.relation,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityInvWithLimitQuery.class));
                            }),
                    entry(MaximalCardinalityInvQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityInvQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinalityInv(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityInvQuery.class));
                            }),
                    entry(MapQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MapQuery, Integer> queryFunction =
                                        (requestClass) -> kb.map(
                                                requestClass.e
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MapQuery.class));
                            }),
                    entry(MapCharSequenceQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<MapCharSequenceQuery, Integer> queryFunction =
                                        (requestClass) -> kb.map(
                                                requestClass.e
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MapCharSequenceQuery.class));
                            }),
                    entry(UnmapQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<UnmapQuery, String> queryFunction =
                                        (requestClass) -> kb.unmap(
                                                requestClass.e
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        UnmapQuery.class));
                            }),
                    entry(TripleQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<TripleQuery, int[]> queryFunction =
                                        (requestClass) -> kb.triple(
                                                requestClass.s,
                                                requestClass.p,
                                                requestClass.o
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        TripleQuery.class));
                            }),
                    entry(TripleArrayQueryName,
                            (req) ->
                            {
                                QueryProcessing.IQueryFunction<TripleArrayQuery, int[]> queryFunction =
                                        (requestClass) -> kb.triple(
                                                requestClass.triple
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        TripleArrayQuery.class));
                            }))

    );


    private String route(String request, WebSocketHandlerInterface handlerMethod) {
        String responseBody = "";
        try {
            responseBody = handlerMethod.webSocketHandler(request);
        } catch (Exception e) {
            System.err.format("Failed to process query: %s\n", request);
            e.printStackTrace();
            System.exit(1);
        }
        return responseBody;
    }

    // TODO put in separate thread (create stat logger class) with a refresh rate. set metrics to zero after a while
    static int RESPONSE_FETCH_TIMES_WINDOW_SIZE = 1000;
    static int[] nCacheMiss = new int[1];
    static int[] nCacheHit = new int[1];
    static int[] nTotal = new int[1];
    static long[] cacheFetchMillis = new long[RESPONSE_FETCH_TIMES_WINDOW_SIZE] ;
    static long[] KBFetchMillis = new long[RESPONSE_FETCH_TIMES_WINDOW_SIZE] ;
    static long[] globalFetchMillis = new long[RESPONSE_FETCH_TIMES_WINDOW_SIZE] ;
    static long[] cacheFetchTimes = new long[RESPONSE_FETCH_TIMES_WINDOW_SIZE] ;
    static long[] KBFetchTimes = new long[RESPONSE_FETCH_TIMES_WINDOW_SIZE] ;
    static long[] globalFetchTimes = new long[RESPONSE_FETCH_TIMES_WINDOW_SIZE] ;
    static boolean[] cacheFetchTimesInitFlag = new boolean[1];
    static boolean[] KBFetchTimesInitFlag = new boolean[1];
    static boolean[] globalFetchTimesInitFlag = new boolean[1];
    static int[] cacheFetchTimesRollingIndex = new int[1];
    static int[] KBFetchTimesRollingIndex = new int[1];
    static int[] globalFetchTimesRollingIndex = new int[1];
    static float[] cacheFetchTimesRollingRate = new float[1];
    static float[] KBFetchTimesRollingRate = new float[1];
    static float[] globalFetchTimesRollingRate = new float[1];
    static float[] cacheFetchTimesRollingAvg = new float[1];
    static float[] KBFetchTimesRollingAvg = new float[1];
    static float[] globalFetchTimesRollingAvg = new float[1];
    static final Lock lock = new ReentrantLock();

    static private void avg(long[] arr, boolean[] flag, float[] avg) {
        if (!flag[0])
            return ;
        float S = 0;
        for (int k = 0; k < RESPONSE_FETCH_TIMES_WINDOW_SIZE; k++)
            S += arr[k];
        avg[0] = S / RESPONSE_FETCH_TIMES_WINDOW_SIZE;
    }

    static private void rate(long[] millisArr, int[] id, boolean[] flag, float[] rate) {
        if (!flag[0])
            return ;
        int earliest_id = (id[0] + 1 >= RESPONSE_FETCH_TIMES_WINDOW_SIZE ? 0 : id[0] + 1 ) ;
        rate[0] = RESPONSE_FETCH_TIMES_WINDOW_SIZE /  ((float) (millisArr[id[0]] - millisArr[earliest_id]) / 1000) ;
    }

    static private void logStat(long[] millisArr, long[] arr, long v, long millis, int[] id, boolean[] flag,
                                int[] n, float[] rate, float[] avg) {
        synchronized (lock) {
            millisArr[id[0]] = millis ;
            arr[id[0]] = v;
            rate(millisArr, id, flag, rate) ;
            avg(arr, flag, avg) ;
            id[0]++;
            n[0]++;
            if (id[0] >= RESPONSE_FETCH_TIMES_WINDOW_SIZE) {
                id[0] = 0;
                if (!flag[0])
                    flag[0] = true;
            }
        }
    }

    static private String getStats() {
        return String.format("NT:%s threads, CM: %s q, CH: %s q, T: %s q, CFT: %s ms, KB FT: %s ms, GFT: %s ms, " +
                        "CR: %s q/s, KB R: %s q/s, GR: %s q/s",
                Thread.activeCount(),
                nCacheMiss[0],
                nCacheHit[0],
                nTotal[0],
                cacheFetchTimesRollingAvg[0],
                KBFetchTimesRollingAvg[0],
                globalFetchTimesRollingAvg[0],
                cacheFetchTimesRollingRate[0],
                KBFetchTimesRollingRate[0],
                globalFetchTimesRollingRate[0]);
    }

    private class KBWSServer extends WebSocketServer {

        public KBWSServer(int port) {
            super(new InetSocketAddress(port));
        }

        @Override
        public void onMessage(WebSocket session, String jsonQuery) {
            long globalStartTime = System.currentTimeMillis();
            Payload queryPayload = UnmarhsalPayload(jsonQuery);
            String responseJSON = Caching.GetResultFromCache(queryPayload.cacheKey);
            long cacheFetchTime = System.currentTimeMillis() - globalStartTime;

            if (responseJSON == null) {
                long KBFetchTime = System.currentTimeMillis();

                // Running KB query
                WebSocketHandlerInterface handler = handlers.get(queryPayload.queryType);
                responseJSON = route(queryPayload.jsonContent, handler);
                Caching.CacheResponse(responseJSON, queryPayload.cacheKey);
                KBFetchTime = System.currentTimeMillis() -  KBFetchTime;
                logStat(KBFetchMillis, KBFetchTimes, KBFetchTime, globalStartTime, KBFetchTimesRollingIndex,
                        KBFetchTimesInitFlag, nCacheMiss, KBFetchTimesRollingRate, KBFetchTimesRollingAvg);
            } else {
                logStat(cacheFetchMillis, cacheFetchTimes, cacheFetchTime, globalStartTime, cacheFetchTimesRollingIndex,
                        cacheFetchTimesInitFlag, nCacheHit, cacheFetchTimesRollingRate, cacheFetchTimesRollingAvg);
            }
            String responsePayloadJSON =
                    MarhsalPayload(new Payload(queryPayload.cacheKey, queryPayload.queryType, responseJSON));
            session.send(responsePayloadJSON);
            long globalFetchTime = System.currentTimeMillis() - globalStartTime ;
            logStat(globalFetchMillis, globalFetchTimes, globalFetchTime, globalStartTime, globalFetchTimesRollingIndex,
                    globalFetchTimesInitFlag, nTotal, globalFetchTimesRollingRate, globalFetchTimesRollingAvg);
            System.out.format("%s\r", getStats());
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {

        }

        @Override
        public void onStart() {

        }

        @Override
        public void onOpen(WebSocket session, org.java_websocket.handshake.ClientHandshake handshake)  {
            System.out.format("[%s %s] New connection with : %s\n",
                    Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    session.getRemoteSocketAddress());
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }

    }

    private KBWSServer server ;
    public void setupServer() {
        Runtime r = Runtime.getRuntime();
        r.addShutdownHook(new ShutdownSequenceThread());

        // Loading cache
        Caching.LoadCache();
        server = new KBWSServer(Port);
        server.start();
        System.out.println("WebSocket Server listening on " + Port);

    }

    public void shutdown() {
        // Closing socket
        System.out.println("Stopping WebSocket Server");
        try {
            server.stop();
        } catch (InterruptedException e) {
            System.err.println("Exception caught while attempting to shut down server.");
            e.printStackTrace();
        }
        // Saving cache to file
        Caching.SaveCache();
    }

    class ShutdownSequenceThread extends Thread {
        public void run() {
            shutdown();
        }
    }

}
