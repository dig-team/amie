package amie.data.remote.cachepolicies;

import amie.data.javatools.datatypes.Pair;
import amie.data.remote.Cache;
import amie.data.remote.Caching;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LRU implements Cache {
    private final Lock lock = new ReentrantLock();
    private final HashMap<String, String> Cache = new HashMap<>();
    private final LinkedList<String> CacheKeys = new LinkedList<String>(); // replace by heap?
    private int maxCacheSize = Caching.DEFAULT_CACHE_SIZE;

    // Cache saving
    private int maxIndex = 0;
    private final LinkedList<Pair<Integer, String>> CacheKeysBuffer = new LinkedList<Pair<Integer, String>>();
    private String CACHE_DIRECTORY = "cache";
    private String CACHE_DIRECTORY_PATH = CACHE_DIRECTORY + File.separator;
    private final String POSITION_KEY_SEP = "~";
    private final long MAX_FILE_SIZE_BYTES = 100_000;

    public void InitClientDir() {
        CACHE_DIRECTORY = String.format("client-%s", CACHE_DIRECTORY) ;
    }

    @Override
    public void SetScale(int size) {
        if (size < 0) {
            System.err.println("Cache size should be non-negative.");
            System.exit(1);
        }
        maxCacheSize = size;
        System.out.println("Set cache size to " + size + " queries.");
    }

    /**
     * @param cacheKey
     * @return If result is in cache, positions it on top and then returns it. Returns null otherwise.
     */
    @Override
    public String GetResultFromCache(String cacheKey) {
        String result = null;
        if (maxCacheSize > 0) {
            lock.lock();
            if (CacheKeys.remove(cacheKey)) {
                CacheKeys.add(cacheKey);
                result = Cache.get(cacheKey);
            }
            lock.unlock();
        }
        return result;
    }

    private void ErrResponseStructuresState() {
        System.err.println("Cache Keys: " + CacheKeys);
        System.err.println("Cache : " + Cache);
        System.exit(1);
    }

    /**
     * Adds response to cache at highest position.
     *
     * @param JSONResponse
     * @param cacheKey
     */
    @Override
    public void CacheResponse(String JSONResponse, String cacheKey) {

        if (maxCacheSize > 0) {
            lock.lock();
            if (Cache.size() >= maxCacheSize) {
                String popped = CacheKeys.pop();
                Cache.remove(popped);
            }
            CacheKeys.remove(cacheKey); // Is CacheKeys.remove(cacheKey) necessary?
            CacheKeys.add(cacheKey);
            Cache.put(cacheKey, JSONResponse);
//            System.out.println("CacheResponse "+ cacheKey + " " + JSONResponse + " sizes ok? " + CacheKeys.size() + " " + Cache.size());
            lock.unlock();
        }
    }

    /**
     * Creates cache directory if it does not exist then creates and saves each cache entry in separate files named after
     * their respective cache key and their placement in the cache.
     */
    @Override
    public void SaveCache() {
        lock.lock();
        if (CacheKeys.isEmpty()) {
            System.out.printf("Empty cache, no saving.");
            return;
        }
        System.out.printf("Saving cache (%s queries) ... ", CacheKeys.size());
        try {
            // Creating cache dir if not exists
            File dir = new File(CACHE_DIRECTORY_PATH);
            if (!(dir.exists() && dir.isDirectory())) {
                if (!(dir.mkdir()))
                    throw new IOException("Couldn't create cache directory " + CACHE_DIRECTORY);
            } else {
                // Deleting files with filename matching cache file pattern
                dir.listFiles(
                        (file, filename) -> {
                            try {
                                if (GetOptionalKeyString(filename).isPresent()) {
                                    new File(CACHE_DIRECTORY_PATH + filename).delete();
                                }
                            } catch (Exception e) {
                                System.err.printf("Couldn't delete %s\n", filename);
                            }
                            return false;
                        }
                );
            }

            // Creating and saving to files
            Iterator<String> cacheKeyIterator = CacheKeys.iterator();
            int i = 0;
            while (cacheKeyIterator.hasNext()) {
                String key = cacheKeyIterator.next();
                FileWriter fw = new FileWriter(CACHE_DIRECTORY_PATH + i + POSITION_KEY_SEP + key);
                BufferedWriter bufferedWriter = new BufferedWriter(fw);
                bufferedWriter.write(Cache.get(key));
                bufferedWriter.close();
                fw.close();
                i++;
            }
        } catch (IOException e) {
            System.err.println("Couldn't save cache.");
            e.printStackTrace();
        }
        System.out.println("done.");
        lock.unlock();
    }

    private Optional<Pair<Integer, String>> GetOptionalKeyString(String filename) {
        String[] split = filename.split(POSITION_KEY_SEP);
        if (split.length != 2)
            return Optional.empty();
        try {
            int pos = Integer.parseInt(split[0]);
            if (pos > maxIndex) {
                maxIndex = pos;
            }
            return Optional.of(new Pair<Integer, String>(pos, split[1]));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void LoadIfValid(String filename) {
        try {
            Optional<Pair<Integer, String>> optionalKeyString = GetOptionalKeyString(filename);
            if (optionalKeyString.isEmpty())
                return;
            String key = optionalKeyString.get().second;
            File cacheFile = new File(CACHE_DIRECTORY_PATH + filename);
            if (cacheFile.length() > MAX_FILE_SIZE_BYTES) {
                return;
            }
            Scanner obj = new Scanner(cacheFile);
            StringBuilder content = new StringBuilder();
            while (obj.hasNextLine()) content.append(obj.nextLine());
            CacheKeysBuffer.add(optionalKeyString.get());
            Cache.put(key, content.toString());
        } catch (Exception ignored) {
        }
    }

    private void InvalidateCache() {
        Cache.clear();
        CacheKeys.clear();
    }

    /**
     * Load saved cache from cache directory if it exists.
     */
    @Override
    public void LoadCache(String config) {
        CACHE_DIRECTORY = String.format("%s-%s", CACHE_DIRECTORY, config) ;
        CACHE_DIRECTORY_PATH = CACHE_DIRECTORY + File.separator;

        if (maxCacheSize == 0) return;
        File dir = new File(CACHE_DIRECTORY);
        if (!(dir.exists() && dir.isDirectory())) {
            System.out.printf("Couldn't find cache content to load. Cache content will be saved to %s after " +
                    "execution.\n", CACHE_DIRECTORY);
        }

        System.out.format("Loading %s ... ", CACHE_DIRECTORY_PATH);
        dir.listFiles((dir1, filename) -> {
            LoadIfValid(filename);
            return false;
        });

        try {

            if (CacheKeysBuffer.size() != maxIndex + 1) {
                System.err.println("Inconsistent number of cache files. (Highest position does not match number of cache files.)");
                throw new Exception();
            }

            for (int i = 0; i <= maxIndex; i++) {
                boolean found = false;
                for (Pair<Integer, String> posAndKey : CacheKeysBuffer) {
                    if (posAndKey.first == i) {
                        CacheKeys.add(posAndKey.second);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.err.println("Missing cache files.");
                    throw new Exception();
                }
            }


            System.out.printf("done (%s queries).\n", CacheKeys.size());
        } catch (Exception e) {
            System.err.println("Couldn't load cache. (Note that content of cache directory should be automatically" +
                    " replaced by a clean version on shutdown)");
            InvalidateCache();
        } finally {
            CacheKeysBuffer.clear();
            ;
        }
    }
}
