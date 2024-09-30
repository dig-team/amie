package amie.data.remote;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A wrapper class for KB to perform actions before and after calls to query methods, such as logging.
 */
public abstract class QueryLogger {

    static public boolean enableQueryLogger;
    static FileWriter logFw;
    public static BufferedWriter logWriter;
    public static ReentrantReadWriteLock.WriteLock fileLock = new ReentrantReadWriteLock().writeLock();

    public static final String SERVER_LOG_PREFIX = "Server";

    public static final String CLIENT_LOG_PREFIX = "Client";

    public static final String LOG_SUFFIX = "Log.tsv";
    public static final String PROFILE_SUFFIX = "Profile.txt";


    public static final String LOG_DIRECTORY = "query-logs";

    public static final int TRYLOCK_TIMEOUT = 10;


    public static Map<String, String> ExecutionProfile = new HashMap<String, String>();

    public static void initQueryLog(String prefix) {
        if (!enableQueryLogger) {
            return;
        }
        try {
            File dir = new File(LOG_DIRECTORY);
            if (!(dir.exists() && dir.isDirectory())) {
                if (!dir.mkdir())
                    throw new IOException("Couldn't create log directory " + LOG_DIRECTORY);
            }
            final int[] maxLogIndex = {0};
            dir.listFiles((dir1, name) -> {
                int index;
                try {
                    index = Integer.parseInt(name);
                    maxLogIndex[0] = Math.max(index, maxLogIndex[0]);
                } catch (NumberFormatException e) {
                    return false;
                }
                return false;
            });

            String subDirPath = LOG_DIRECTORY + "/" + (maxLogIndex[0] + 1);
            if (!(new File(subDirPath).mkdir()))
                throw new IOException("Couldn't create log subdirectory " + subDirPath);

            // Printing execution profile to text file
            FileWriter profileFw = new FileWriter(subDirPath + "/" + prefix + PROFILE_SUFFIX, true);
            BufferedWriter profileWriter = new BufferedWriter(profileFw);
            profileWriter.write(ExecutionProfile.toString());

            // Init query logging file
            logFw = new FileWriter(subDirPath + "/" + prefix + LOG_SUFFIX, true);
            logWriter = new BufferedWriter(logFw);
            if (Objects.equals(prefix, SERVER_LOG_PREFIX)) {
                logWriter.write("query_topic_lag\t");
                logWriter.write("query_topic_partition\t");
                logWriter.write("sent\t");
                logWriter.write("received\t");
                logWriter.write("delay\t");
                logWriter.write("topic\n");
            } else {
                logWriter.write("response_topic_lag\t");
                logWriter.write("response_topic_partition\t");
                logWriter.write("sent\t");
                logWriter.write("received\t");
                logWriter.write("delay\t");
                logWriter.write("query\t");
                logWriter.write("response\t");
                logWriter.write("topic\t");
                logWriter.write("cache_hit\t");
                logWriter.write("cache_key\n");
            }
        } catch (Exception e) {
            System.err.println("Couldn't init query logger.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void printTimeAndTopicToLog(long startTime, String topicName) {
        if (!enableQueryLogger) {
            return;
        }
        long endTime = System.currentTimeMillis();
        try {
            if (!fileLock.isHeldByCurrentThread()) {
                throw new Exception(Thread.currentThread().getName() + " : File is held by another thread.");
            }
            logWriter.write(startTime + "\t");
            logWriter.write(endTime + "\t");
            logWriter.write(endTime - startTime + "\t");
            logWriter.write(topicName + "\n");
            fileLock.unlock();
        } catch (Exception e) {
            System.out.println(Thread.currentThread().getName() + " : Couldn't write exchange log.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void printPartitionToLog(int partition) {
        if (!enableQueryLogger) {
            return;
        }
        try {
            if (!fileLock.isHeldByCurrentThread()) {
                throw new Exception(Thread.currentThread().getName() + " : File is already locked.");
            }
            logWriter.write(partition + "\t");
        } catch (Exception e) {
            System.out.println(Thread.currentThread().getName() + " : Couldn't write partition to exchange log.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void printLagToLog(String lag) {
        if (!enableQueryLogger) {
            return;
        }
        try {
            if (!fileLock.tryLock(TRYLOCK_TIMEOUT, TimeUnit.SECONDS)) {
                throw new Exception(Thread.currentThread().getName() + " : File is already locked.");
            }
            logWriter.write(lag + "\t");
        } catch (Exception e) {
            System.out.println(Thread.currentThread().getName() + " : Couldn't write lag to exchange log.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void printClientDataToLog(long sentTime, String jsonQuery,
                                            String JSONResponse, String topicName, String queryKey, String cacheKey) {
        if (!enableQueryLogger) {
            return;
        }
        long receivedTime = System.currentTimeMillis();
        long delay = receivedTime - sentTime;
        try {
            if (!fileLock.isHeldByCurrentThread()) {
                throw new Exception(Thread.currentThread().getName() + " : File isn't locked by current thread " +
                        fileLock.toString());
            }
            QueryLogger.logWriter.write(sentTime + "\t");
            QueryLogger.logWriter.write(receivedTime + "\t");
            QueryLogger.logWriter.write(delay + "\t");
            QueryLogger.logWriter.write(jsonQuery.replaceAll("\n", "") + "\t");
            QueryLogger.logWriter.write(JSONResponse.replaceAll("\n", "") + "\t");
            QueryLogger.logWriter.write(topicName + "\t");
            QueryLogger.logWriter.write((queryKey == null ? "1" : "0") + "\t");
            QueryLogger.logWriter.write(cacheKey + "\n");
            QueryLogger.fileLock.unlock();
        } catch (Exception e) {
            System.err.println("Couldn't write exchange log.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void closeWriter() {
        if (!enableQueryLogger) {
            return;
        }
        try {
            logWriter.close();
            logFw.close();
        } catch (Exception e) {
            System.out.println(Thread.currentThread().getName() + " : Couldn't close exchange log.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
