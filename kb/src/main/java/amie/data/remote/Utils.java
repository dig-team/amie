package amie.data.remote;

import amie.data.javatools.datatypes.Pair;

import java.util.Random;

/**
 * Contains various utilities for remote KB communication.
 */
public class Utils {

    public static final char SEP = '/' ;
    public static final int RANDOM_STRING_LENGTH = 10 ;

    private static final Random random = new Random();
    private static final int leftLimit = 0; // numeral '0'
    private static final int rightLimit = 254; // letter 'y'


    /**
     * Gets random string.
     *
     * @return the random string
     */

    public static String getRandomString() {
        return random.ints(leftLimit, rightLimit + 1)
                .limit(RANDOM_STRING_LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

//
//    public static String getRandomStringBM(int length) {
//        return random.ints(leftLimit, rightLimit + 1)
//                .filter(i -> i <= 57 || (i >= 65 && i <= 90) || i >= 97)
//                .limit(length)
//                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
//                .toString();
//    }

    public static Pair<String, String> unpackMessage(String message) {
        String key = message.substring(0, Utils.RANDOM_STRING_LENGTH) ;
        String response = message.substring(Utils.RANDOM_STRING_LENGTH) ;
        return new Pair<>(key, response) ;
    }

    public static void printReceivedMessage(String source, String raw) {
//        System.out.format("MESSAGE CHAN: %s\n\t RAW CONTENT: %s\n", source, raw);
    }
    public static void printReceivedPayload(String source, Queries.Payload pl) {
//        System.out.format("RECEIVED CHAN: %s KEY: %s\n\t QUERY TYPE %s CONTENT %s\n", source, pl.cacheKey, pl.queryType, pl.jsonContent);
    }

    public static void printSent(String destination, Queries.Payload pl) {
//        System.out.format("SENT CHAN: %s KEY: %s\n\t QUERY TYPE %s CONTENT %s\n", destination, pl.cacheKey, pl.queryType, pl.jsonContent);
    }


}
