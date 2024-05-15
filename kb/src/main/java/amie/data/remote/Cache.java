package amie.data.remote;

/**
 * Cache interface for implementing custom cache policies.
 * Cache policies are to be implemented in the cachepolicies sub package.
 */
public interface Cache {
        void SetScale(int scale) ;
        String GetResultFromCache(String cacheKey) ;
        void CacheResponse(String JSONResponse, String cacheKey) ;
        void InitClientDir() ;
        void SaveCache() ;
        void LoadCache() ;
}
