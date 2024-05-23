package amie.data.remote;

import amie.data.AbstractKB;
import amie.data.remote.cachepolicies.LRU;

import java.util.Arrays;
import java.util.List;

/**
 * Cache is used for remote KB mode, either by client or server depending on user passed parameters.

 * Performances:
 * Cache can significantly improve performances by reducing the amount of queries sent over network or
 * executed by the KB.
 * Performances will vary based on policy (see below), scale, knowledge graph or other AMIE user parameters.

 * Policies:
 * NOTE: As of now, only Least Recently Used (LRU) cache policy has been implemented.
 * Policies are implemented in the cachepolicies sub package.

 */
public abstract class Caching {

    private static final String LRU_POLICY = "LRU" ;
    private static final List<String> Policies = List.of(
            LRU_POLICY
            // Add here other policy names
    ) ;


    /**
     * Default cache policy.
     */
    public static final Class<? extends Cache> DEFAULT_POLICY = LRU.class;

    /**
     * Default cache scale.
     */
    public static final int DEFAULT_CACHE_SIZE = 10_000;
    private static Cache cache;

    /**
     * Enable caching.
     * @param policyStr : Cache policy.
     */
    public static void EnableCache(String policyStr) {

       switch (policyStr) {
                default:
                    System.err.format("Unrecognized cache policy \"%s\". Please select a policy among the followings:\n%s.\n",
                            policyStr,
                            Policies);
                    System.exit(1);
                    break ;
                case "LRU":
                    cache = new LRU();
                    break;
                // Add here other policies
            }
        System.out.println("Set cache policy to " + cache.getClass());
    }

    /**
     * Enable caching with default policy.
     */
    public static void EnableDefaultCache() {
            System.out.println("Unspecified cache policy. Using default "+DEFAULT_POLICY.getSimpleName());
            try {
                cache = DEFAULT_POLICY.getConstructor().newInstance() ;
            } catch (Exception e) {
                System.err.println("Couldn't instantiate default cache policy.");
                e.printStackTrace();
                System.exit(1);
            }
    }

    /**
     * @return true if caching is enabled.
     */
    public static boolean IsEnabled() {
        return cache != null ;
    }

    /**
     * Scales cache.
     * @param scale: scale for the cache. Unit depends on policy. For LRU: number of queries.
     */
    public static void SetScale(int scale) {
        if (cache == null) {EnableDefaultCache() ;}
        cache.SetScale(scale);
    }

    /**
     * Gets result if present and reorder cache accordingly.
     * @param cacheKey
     * @return result if present in cache or null otherwise.
     */
    public static String GetResultFromCache(String cacheKey) {
        if (cache == null) {return null;}
        return cache.GetResultFromCache(cacheKey) ;
    }

    /**
     * Puts response in cache.
     * @param JSONResponse Response to put in cache.
     * @param cacheKey Cache key generated from the query.
     */
    public static void CacheResponse(String JSONResponse, String cacheKey) {
        if (cache == null) {return;}
        cache.CacheResponse(JSONResponse, cacheKey);
    }

    /**
     * If it exists, locally saved cache content from a previous execution will be ignored and overwritten by a
     * newer version.
     */
    public static void InvalidateCache() {
        cache.InvalidateCache();
    }

    /**
     * Saves cache content to local directory.
     */
    public static void SaveCache() {
        if (cache == null) {return;}
        cache.SaveCache();
    }

    /**
     * Loads cache content from local directory.
     */
    public static void LoadCache(String config) {
        if (cache == null) {return;}
        cache.LoadCache(config);
    }
}
