//package amie.data.utils;
//
//import com.hp.hpl.jena.rdf.model.*;
//import com.hp.hpl.jena.util.FileManager;
//
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderTransformationBase.log;
//
//public class ParseInputFiles {
//    public static List<String[]> parseTTLOrNTFile(String filePath) {
//        List<String[]> resultList = new ArrayList<>();
//        Model model = ModelFactory.createDefaultModel();
//
//        InputStream in = FileManager.get().open(filePath);
//        if (in == null)
//        {
//            throw new IllegalArgumentException("File: " + filePath + " not found");
//        }
//        if (filePath.endsWith(".ttl")) {
//            model.read(in, "","TTL");
//        }
//        if (filePath.endsWith(".nt")) {
//            model.read(in, "","N3");
//        }
//        model.read(in, "","TTL");
//        System.out.println("begin parse input file");
//        // list the statements in the graph
//        StmtIterator iter = model.listStatements();
//
//        // print out the predicate, subject and object of each statement
//        while (iter.hasNext())
//        {
//            String[] line = new String[3];
//            Statement stmt = iter.nextStatement(); // get next statement
//            //Resource subject = stmt.getSubject(); // get the subject
//            //Property predicate = stmt.getPredicate(); // get the predicate
//            //RDFNode object = stmt.getObject(); // get the object
//
//            String subject = stmt.getSubject().toString(); // get the subject
//            String predicate = stmt.getPredicate().toString(); // get the predicate
//            String object = stmt.getObject().toString(); // get the object
//
//            line[0] = subject;
//            line[1] = predicate;
//            line[2] = object;
//
//            resultList.add(line);
////            System.out.print("subject: " + subject+"\t");
////            System.out.print(" predicate: " + predicate+"\t");
////            if (object instanceof Resource)
////            {
////                System.out.print(" object " + object);
////            }
////            else {// object is a literal
////                System.out.print("object \"" + object.toString() + "\"");
////            }
////            System.out.println(" .");
//        }
//        return resultList;
//    }
//
//    public static void main(String args[]) {
//        try {
//            FileInputStream fileInputStream = new FileInputStream("D:\\amie\\sample5.tsv");
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
//            while (bufferedReader.readLine() != null) {
//                String str = bufferedReader.readLine();
//                String[] line = str.split("\\s+");
//                System.out.println(line);
//            }
//        } catch (Exception e) {
//            log.error("");
//        }
//    }
//    public static List<String[]> parseTTLOrNTFileInLine(String filePath) {
//        List<String[]> resultList = new ArrayList<>();
//        try {
//            InputStream in = FileManager.get().open(filePath);
//            if (in == null) {
//                throw new IllegalArgumentException("File: " + filePath + " not found");
//            }
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
//            while (bufferedReader.readLine() != null) {
//                String lineStr = bufferedReader.readLine();
//                resultList.add(lineStr.split("\t"));
//            }
//        } catch (Exception e) {
//            log.error("ParseInputFiles.parseTTLOrNTFileInLine parse TTL or NT error, filePath:{}", filePath);
//        }
//        return resultList;
//    }
//
////        if (filePath.endsWith(".ttl")) {
////            model.read(in, "","TTL");
////        }
////        if (filePath.endsWith(".nt")) {
////            model.read(in, "","N3");
////        }
////        model.read(in, "","TTL");
////        System.out.println("begin parse input file");
////        // list the statements in the graph
////        StmtIterator iter = model.listStatements();
////
////        // print out the predicate, subject and object of each statement
////        while (iter.hasNext())
////        {
////            String[] line = new String[3];
////            Statement stmt = iter.nextStatement(); // get next statement
////            //Resource subject = stmt.getSubject(); // get the subject
////            //Property predicate = stmt.getPredicate(); // get the predicate
////            //RDFNode object = stmt.getObject(); // get the object
////
////            String subject = stmt.getSubject().toString(); // get the subject
////            String predicate = stmt.getPredicate().toString(); // get the predicate
////            String object = stmt.getObject().toString(); // get the object
////
////            line[0] = subject;
////            line[1] = predicate;
////            line[2] = object;
////
////            resultList.add(line);
//////            System.out.print("subject: " + subject+"\t");
//////            System.out.print(" predicate: " + predicate+"\t");
//////            if (object instanceof Resource)
//////            {
//////                System.out.print(" object " + object);
//////            }
//////            else {// object is a literal
//////                System.out.print("object \"" + object.toString() + "\"");
//////            }
//////            System.out.println(" .");
////        }
////        return resultList;
////    }
//}
