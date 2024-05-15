package amie.data;

import amie.data.remote.Caching;
import amie.data.remote.Queries;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class KBRESTClient extends AbstractKBClient {

    HttpClient httpClient ;

    public KBRESTClient() {
        setup();
        this.schema = new Schema() ;
        initMapping();
    }

    static public void SetFormattedServerAddress() {
        baseURL = String.format("http://%s/", ServerAddress) ;
    }

    public void setup() {
        httpClient = HttpClient.newHttpClient() ;
    }

    @Override
    protected String getResponse(String jsonQuery, String channel) {
        String result = null ;

        // Checking cache content before sending HTTP request
        String cacheKey = Queries.GenerateCacheKey(channel, jsonQuery) ;
        String resultFromCache = Caching.GetResultFromCache(cacheKey) ;
        if(resultFromCache != null) {
            return resultFromCache ;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(baseURL+ channel))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonQuery))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString()) ;
            if (httpResponse.statusCode() != HttpURLConnection.HTTP_CREATED) {
                throw new Exception("Bad status code: "+httpResponse.statusCode()) ;
            }
            result = httpResponse.body() ;

            // Caching result
            Caching.CacheResponse(result, cacheKey) ;
//            System.out.println(baseURL+ channel +": Ran query: "+jsonQuery);
        } catch (Exception e) {
            System.err.println(baseURL+ channel +": Failed to run query: "+jsonQuery);
            e.printStackTrace();
            System.exit(1);
        }
        return result ;
    }
}
