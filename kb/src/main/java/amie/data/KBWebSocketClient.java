package amie.data;

import amie.data.remote.Caching;
import amie.data.remote.Queries;
import amie.data.remote.Queries.Payload;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.data.remote.Queries.UnmarhsalPayload;
import static amie.data.remote.Queries.gson;

public class KBWebSocketClient extends AbstractKBClient {

    static private final ConcurrentHashMap<Long, CompletableFuture<KBWSClient>> OpenSockets = new ConcurrentHashMap<>();

    static private final int RESPONSE_WAITING_TIME_MS = 60_000;

    public KBWebSocketClient() {
        initClient();
        this.schema = new Schema();
        initMapping();
    }

    public KBWebSocketClient(Schema schema) {
        initClient();
        this.schema = schema;
        initMapping();
    }

    private void initClient() {
        Caching.LoadCache();
        Runtime r = Runtime.getRuntime();
        r.addShutdownHook(new ShutdownSequenceThread());
    }

    static public void SetFormattedServerAddress() {
        baseURL = String.format("ws://%s", ServerAddress);
    }

    static private class KBWSClient extends WebSocketClient {
        private CompletableFuture<String> futureResponse;

        private long parentgetId;

        public KBWSClient(URI serverUri, long parentgetId) {
            super(serverUri);
            this.parentgetId = parentgetId;
        }

        /**
         * Send JSON payload and set futureResponse for completion
         *
         * @param payload
         * @param futureResponse
         */
        public void sendPayload(String payload, CompletableFuture<String> futureResponse) {
            this.futureResponse = futureResponse;
            this.send(payload);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
//            System.out.format("[%s] Connection opened with %s",
//                    Thread.currentThread().getName(),
//                    getURI());
            OpenSockets.get(parentgetId).complete(this);
        }

        @Override
        public void onMessage(String jsonPayload) {
            futureResponse.complete(jsonPayload);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.format("[%s] Connection closed to %s (reason %s)\n",
                    Thread.currentThread().getName(),
                    getURI(),
                    reason);
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("WebSocket error");
            ex.printStackTrace();
            System.exit(1);
        }
    }

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
        if (enableLiveMetrics) {
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


    /**
     * Run query:
     * - If query is found in cache, un-cache
     *
     * @param jsonQuery
     * @param queryType
     * @return
     */
    @Override
    protected String getResponse(String jsonQuery, String queryType) {
        String cacheKey = Queries.GenerateCacheKey(queryType, jsonQuery);
        long globalStartTime = System.currentTimeMillis();

        String responsePayloadJSON = Caching.GetResultFromCache(cacheKey);

        long cacheFetchTime = System.currentTimeMillis() - globalStartTime;

        if (responsePayloadJSON == null) {
            long KBFetchTime = System.currentTimeMillis();
            try {
                // Fetching KB response
                CompletableFuture<String> futureResponse = new CompletableFuture<>();
                long parentgetId = Thread.currentThread().getId();
                if (!OpenSockets.containsKey(parentgetId)) {
                    // new web socket will put itself in map once open
                    OpenSockets.putIfAbsent(parentgetId, new CompletableFuture<>());
                    new KBWSClient(new URI(baseURL), parentgetId).connect();
                }

                // Waiting for socket to be open
                KBWSClient webSocketClient = OpenSockets.get(parentgetId).get();

                // Setting expectations in WebSocket (synchronously with Cache thread to avoid late execution and more
                // recent expectations being overwritten)
                String jsonPayload = gson.toJson(new Payload(cacheKey, queryType, jsonQuery));
                webSocketClient.sendPayload(jsonPayload, futureResponse);
                responsePayloadJSON = futureResponse.get(RESPONSE_WAITING_TIME_MS, TimeUnit.MILLISECONDS);
                Caching.CacheResponse(responsePayloadJSON, cacheKey);

            } catch (Exception e) {
                System.err.println("Failed to send query.");
                e.printStackTrace();
                System.exit(1);
            }
            KBFetchTime = System.currentTimeMillis() - KBFetchTime;
            logStat(KBFetchMillis, KBFetchTimes, KBFetchTime, globalStartTime, KBFetchTimesRollingIndex,
                    KBFetchTimesInitFlag, nCacheMiss, KBFetchTimesRollingRate, KBFetchTimesRollingAvg);
        } else {
            logStat(cacheFetchMillis, cacheFetchTimes, cacheFetchTime, globalStartTime, cacheFetchTimesRollingIndex,
                    cacheFetchTimesInitFlag, nCacheHit, cacheFetchTimesRollingRate, cacheFetchTimesRollingAvg);
        }
        long globalFetchTime = System.currentTimeMillis() - globalStartTime;
        logStat(globalFetchMillis, globalFetchTimes, globalFetchTime, globalStartTime, globalFetchTimesRollingIndex,
                globalFetchTimesInitFlag, nTotal, globalFetchTimesRollingRate, globalFetchTimesRollingAvg);

        if (enableLiveMetrics)
            System.out.format("%s\r", getStats());

        Payload responsePayload = UnmarhsalPayload(
                responsePayloadJSON
        );
        return responsePayload.jsonContent;
    }

    public void shutdown() {
        // Closing sockets
        for (Map.Entry<Long, CompletableFuture<KBWSClient>> completableWebSocketClientEntry : OpenSockets.entrySet()) {
            try {
                if (completableWebSocketClientEntry.getValue().isDone())
                    completableWebSocketClientEntry.getValue().get().close();
            } catch (Exception e) {
                System.err.println("Failed to shutdown socket entry " + completableWebSocketClientEntry);
            }
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
