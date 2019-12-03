package amie.data.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javatools.datatypes.ByteString;

import amie.data.Schema;
import amie.data.SetU;
import amie.data.SimpleTypingKB;

public class PrintCounts {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        List<File> dataFiles = new ArrayList<>();

        if (args.length < 1 || (args[0].equals("-l") && args.length < 3)) {
            System.err.println("No input file has been provided");
            System.err.println("PrintCounts [-l threshold] <.tsv INPUT FILES>");
            System.exit(1);
        }

        int threshold = 0;
        int i = 0;
        if (args[0].equals("-l")) {
            i += 2;
            try {
                threshold = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Error: threshold must be an integer");
                System.err.println("PrintCounts [-l threshold] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        //Load database
        for (; i < args.length; ++i) {
            dataFiles.add(new File(args[i]));
        }
        SimpleTypingKB kb = new SimpleTypingKB();
        kb.setDelimiter(" ");
        Schema.typeRelation = "<P106>";
        Schema.typeRelationBS = ByteString.of(Schema.typeRelation);
        Schema.subClassRelation = "<P279>";
        Schema.subClassRelationBS = ByteString.of(Schema.subClassRelation);
        Schema.top = "<Q35120>";
        Schema.topBS = ByteString.of(Schema.top);
        kb.load(dataFiles);

        FileOutputStream fstream1 = new FileOutputStream("countsClass.tsv");
        BufferedWriter out1 = new BufferedWriter(new OutputStreamWriter(fstream1, "UTF-8"));
        FileOutputStream fstream2 = new FileOutputStream("countsIntersectionClass.tsv");
        BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(fstream2, "UTF-8"));
        int s = 0;

        for (ByteString t : kb.classes.keySet()) {
            if ((s = kb.classes.get(t).size()) >= threshold) {
                out1.append(t.toString() + "\t" + Integer.toString(s) + "\n");
            }
        }
        for (ByteString t1 : kb.classes.keySet()) {
            for (ByteString t2 : kb.classes.keySet()) {
                if ((s = (int) SetU.countIntersection(kb.classes.get(t1), kb.classes.get(t2))) >= threshold) {
                    out2.append(t1.toString() + "\t" + t2.toString() + "\t" + s + "\n");
                }
            }
        }
        out1.close();
        out2.close();
    }
}
