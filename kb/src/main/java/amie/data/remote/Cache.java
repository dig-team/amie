package amie.data.remote;

/**
 * Cache interface for implementing custom cache policies.
 * Cache policies are to be implemented in the cachepolicies sub package.
 */
public interface Cache {
        void InvalidateCache() ;
        void SetScale(int scale) ;
        String GetResultFromCache(String cacheKey) ;
        void CacheResponse(String JSONResponse, String cacheKey) ;
        void SaveCache() ;
        void LoadCache(String config) ;
}
