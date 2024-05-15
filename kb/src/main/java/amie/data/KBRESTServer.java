package amie.data;

import amie.data.remote.QueryProcessing;
import com.sun.net.httpserver.HttpExchange;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static amie.data.remote.Queries.*;
import static java.util.Map.entry;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;

public class KBRESTServer extends KB {

    public AbstractKB kb;
    static public int DEFAULT_PORT = 9092;

    public KBRESTServer() {
        super();
        this.kb = this;
        setupRouter();
    }

    public KBRESTServer(Schema schema) {
        super(schema);
        this.kb = this;
        setupRouter();
    }


    private final LinkedHashMap<String, HttpHandler> handlers = new LinkedHashMap<>(
            Map.ofEntries(
                    entry(CountProjectionBindingsQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountProjectionBindingsQuery, Int2IntMap> queryFunction =
                                        (requestClass) -> kb.countProjectionBindings(
                                                requestClass.projectionTriple,
                                                requestClass.otherTriples,
                                                requestClass.variable
                                        );
                                return QueryProcessing.processQuery(req, queryFunction,
                                        CountProjectionBindingsQuery.class);
                            })),
                    entry(CountProjectionQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountProjectionQuery, Long> queryFunction =
                                        (requestClass) -> kb.countProjection(
                                                requestClass.projectionTriple,
                                                requestClass.otherTriples
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountProjectionQuery.class));
                            })),
                    entry(CountDistinctQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountDistinctQuery, Long> queryFunction =
                                        (requestClass) -> kb.countDistinct(
                                                requestClass.variable,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountDistinctQuery.class));
                            })),
                    entry(CountDistinctPairsQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountDistinctPairsQuery, Long> queryFunction =
                                        (requestClass) -> kb.countDistinctPairs(
                                                requestClass.var1,
                                                requestClass.var2,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountDistinctPairsQuery.class));
                            })),
                    entry(SelectDistinctQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<SelectDistinctQuery, IntSet> queryFunction =
                                        (requestClass) -> kb.selectDistinct(
                                                requestClass.variable,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        SelectDistinctQuery.class));
                            })),
                    entry(CountDistinctPairsUpToQueryName,
                            (exchange) -> route(exchange, (req) ->
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
                            })),
                    entry(CountDistinctPairsUpToWithIteratorQueryName,
                            (exchange) -> route(exchange, (req) ->
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
                            })),
                    entry(GetRelationsQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<GetRelationsQuery, IntCollection> queryFunction =
                                        (requestClass) -> kb.getRelations();
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        GetRelationsQuery.class));
                            })),
                    entry(CountQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountQuery, Long> queryFunction =
                                        (requestClass) -> kb.count(
                                                requestClass.triple
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountQuery.class));
                            })),
                    entry(FrequentBindingsOfQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<FrequentBindingsOfQuery, Int2IntMap> queryFunction =
                                        (requestClass) -> kb.frequentBindingsOf(
                                                requestClass.variable,
                                                requestClass.projectionVariable,
                                                requestClass.query
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        FrequentBindingsOfQuery.class));
                            })),
                    entry(IsFunctionalQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<IsFunctionalQuery, Boolean> queryFunction =
                                        (requestClass) -> kb.isFunctional(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        IsFunctionalQuery.class));
                            })),
                    entry(FunctionalityQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<FunctionalityQuery, Double> queryFunction =
                                        (requestClass) -> kb.functionality(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        FunctionalityQuery.class));
                            })),
                    entry(InverseFunctionalityQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<InverseFunctionalityQuery, Double> queryFunction =
                                        (requestClass) -> kb.inverseFunctionality(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        InverseFunctionalityQuery.class));
                            })),
                    entry(RelationColumnSizeQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<RelationColumnSizeQuery, Integer> queryFunction =
                                        (requestClass) -> kb.relationColumnSize(
                                                requestClass.relation,
                                                requestClass.column
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        RelationColumnSizeQuery.class));
                            })),
                    entry(OverlapQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<OverlapQuery, Integer> queryFunction =
                                        (requestClass) -> kb.overlap(
                                                requestClass.relation1,
                                                requestClass.relation2,
                                                requestClass.overlap
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        OverlapQuery.class));
                            })),
                    entry(CountOneVariableQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<CountOneVariableQuery, Long> queryFunction =
                                        (requestClass) -> kb.countOneVariable(
                                                requestClass.triple
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        CountOneVariableQuery.class));
                            })),
                    entry(RelationSizeQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<RelationSizeQuery, Integer> queryFunction =
                                        (requestClass) -> kb.relationSize(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        RelationSizeQuery.class));
                            })),
                    entry(MaximalRightCumulativeCardinalityQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalRightCumulativeCardinalityQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalRightCumulativeCardinality(
                                                requestClass.relation,
                                                requestClass.threshold,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalRightCumulativeCardinalityQuery.class));
                            })),
                    entry(MaximalRightCumulativeCardinalityInvQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalRightCumulativeCardinalityInvQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalRightCumulativeCardinalityInv(
                                                requestClass.relation,
                                                requestClass.threshold,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalRightCumulativeCardinalityInvQuery.class));
                            })),
                    entry(MaximalCardinalityWithLimitQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityWithLimitQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinality(
                                                requestClass.relation,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityWithLimitQuery.class));
                            })),
                    entry(MaximalCardinalityQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinality(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityQuery.class));
                            })),
                    entry(MaximalCardinalityInvWithLimitQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityInvWithLimitQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinalityInv(
                                                requestClass.relation,
                                                requestClass.limit
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityInvWithLimitQuery.class));
                            })),
                    entry(MaximalCardinalityInvQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MaximalCardinalityInvQuery, Integer> queryFunction =
                                        (requestClass) -> kb.maximalCardinalityInv(
                                                requestClass.relation
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MaximalCardinalityInvQuery.class));
                            })),
                    entry(MapQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MapQuery, Integer> queryFunction =
                                        (requestClass) -> kb.map(
                                                requestClass.e
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MapQuery.class));
                            })),
                    entry(MapCharSequenceQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<MapCharSequenceQuery, Integer> queryFunction =
                                        (requestClass) -> kb.map(
                                                requestClass.e
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        MapCharSequenceQuery.class));
                            })),
                    entry(UnmapQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<UnmapQuery, String> queryFunction =
                                        (requestClass) -> kb.unmap(
                                                requestClass.e
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        UnmapQuery.class));
                            })),
                    entry(TripleQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<TripleQuery, int[]> queryFunction =
                                        (requestClass) -> kb.triple(
                                                requestClass.s,
                                                requestClass.p,
                                                requestClass.o
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        TripleQuery.class));
                            })),
                    entry(TripleArrayQueryName,
                            (exchange) -> route(exchange, (req) ->
                            {
                                QueryProcessing.IQueryFunction<TripleArrayQuery, int[]> queryFunction =
                                        (requestClass) -> kb.triple(
                                                requestClass.triple
                                        );
                                return (QueryProcessing.processQuery(req, queryFunction,
                                        TripleArrayQuery.class));
                            }))
            )
    );

    private interface QueryHandlerInterface {
        String handler(String request) throws Exception;
    }

    private void route(HttpExchange exchange, QueryHandlerInterface handlerMethod) {
        String request = "", responseBody = "";
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        OutputStream responseOutputStream = exchange.getResponseBody();
        try {
            // Get the request body
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBuilder.append(line);
            }
            br.close();
            request = requestBuilder.toString() ;

            responseBody = handlerMethod.handler(request);
            exchange.sendResponseHeaders( HttpURLConnection.HTTP_CREATED, responseBody.length());

            responseOutputStream.write(responseBody.getBytes());
            responseOutputStream.close();
        } catch (Exception e) {
            responseBody = "Failed to process query: " + e;
            System.err.println(responseBody);
            e.printStackTrace();
            System.exit(1);
        }
    }

    void printHandlerSetup(int index, String key) {
        System.out.print("\rHandlers " + index + "/" + handlers.keySet().size() + " " + key + "\t\t\t\t");
    }
    HttpServer server = null ;
    public void setupRouter() {
        try {
            server = HttpServer.create(new InetSocketAddress(Port), 0);;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String sep = "/";
        Iterator<String> handlerKeys = handlers.keySet().iterator();
        int index = 0;

        while (handlerKeys.hasNext()) {
            String key = handlerKeys.next();
            index++;
            printHandlerSetup(index, key);
            server.createContext(sep + key, handlers.get(key));
//            System.out.println(sep + key);
        }
        printHandlerSetup(index, "");
        System.out.println("complete.");
        server.start();
    }

}
