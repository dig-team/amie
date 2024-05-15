package amie.data.remote;

import com.google.gson.*;

import java.lang.reflect.Type;

import static amie.data.remote.Queries.gson;

public class QueryProcessing {

    /**
     * Generic interface to implement query functions.
     *
     * @param <Q> Query type.
     * @param <R> Response type.
     */
    public interface IQueryFunction<Q, R> {
        /**
         * Runs query.
         *
         * @param query Query instance.
         * @return Result.
         */
        R runQuery(Q query);
    }

    /**
     * Processes JSON Query and returns the answer fetched either through cache or by running query on remote KB server.
     *
     * @param value         JSON Query.
     * @param queryFunction Query method.
     * @param queryClass    Class of query.
     * @param <Q>           Type of query.
     * @param <R>           Type of response.
     * @return Result after processing query.
     */
    public static <Q, R> String processQuery(
            String value,
            IQueryFunction<Q, R> queryFunction,
            Class<Q> queryClass) {
        // JSON to query object

        Q deserializedMessageValue = gson.fromJson(value, queryClass);

        // Running query
        R result = queryFunction.runQuery(deserializedMessageValue);

        // Result to JSON
        String jsonResult = gson.toJson(result);

        // Sending result
        return jsonResult;
    }
}
