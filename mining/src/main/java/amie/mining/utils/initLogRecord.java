//package amie.mining.utils;
//
//import org.apache.log4j.PropertyConfigurator;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.Properties;
//
//public class initLogRecord {
//    public static void initLog() {
//        FileInputStream fileInputStream = null;
//        try {
//            Properties properties = new Properties();
//            fileInputStream = new FileInputStream("/mining/src/main/resources/log4j.properties");
//            properties.load(fileInputStream);
//            PropertyConfigurator.configure(properties);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (fileInputStream != null) {
//                try {
//                    fileInputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//}
