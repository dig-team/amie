package amie.data.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

import amie.data.KB;
import amie.data.Schema;

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
        if(args[0].equals("-l")) {
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
        KB kb = new KB();
        kb.load(dataFiles);
        
        FileOutputStream fstream1 = new FileOutputStream("countsClass.tsv");
		BufferedWriter out1 = new BufferedWriter(new OutputStreamWriter(fstream1, "UTF-8"));
		FileOutputStream fstream2 = new FileOutputStream("countsIntersectionClass.tsv");
		BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(fstream2, "UTF-8"));
		
		IntHashMap<ByteString> typesCount = Schema.getTypesCount(kb);
		for (ByteString t : typesCount) {
			if (typesCount.get(t) >= threshold) {
				out1.append(t);
				out1.append("\t" + Integer.toString(typesCount.get(t)) + "\n");
			}
		}
		Map<ByteString, IntHashMap<ByteString>> typesIntersectionCount = Schema.getTypesIntersectionCount(kb);
		for (ByteString t1 : typesIntersectionCount.keySet()) {
			for(ByteString t2 : typesIntersectionCount.get(t1)) {
				if(typesIntersectionCount.get(t1).get(t2) >= threshold) {
					out2.append(t1);
					out2.append("\t");
					out2.append(t2);
					out2.append("\t" + Integer.toString(typesIntersectionCount.get(t1).get(t2)) + "\n");
				}
			}
		}
		out1.close();
		out2.close();
	}
}
