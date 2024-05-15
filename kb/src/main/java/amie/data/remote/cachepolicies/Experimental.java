package amie.data.remote.cachepolicies;

import amie.data.remote.Cache;

public class Experimental implements Cache {
    @Override
    public void SetScale(int scale) {
        return ;
    }

    @Override
    public String GetResultFromCache(String cacheKey) {
        return null;
    }

    @Override
    public void CacheResponse(String JSONResponse, String cacheKey) {

    }

    @Override
    public void InitClientDir() {

    }

    @Override
    public void SaveCache() {

    }

    @Override
    public void LoadCache() {

    }
}
