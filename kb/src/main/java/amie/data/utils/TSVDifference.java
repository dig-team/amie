package amie.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javatools.datatypes.Pair;
import javatools.filehandlers.TSVFile;

public class TSVDifference {
	/**
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Big file
		Set<Pair<String, String>> pairs = new HashSet<>();
		try (TSVFile file = new TSVFile(new File(args[0]))) {
			for (List<String> line : file) {
				if (line.size() >= 3) {
					pairs.add(new Pair<String, String>(line.get(0).trim(), line.get(2).trim()));	
				}
			}
		}
		
		try (TSVFile file = new TSVFile(new File(args[1]))) {
			for (List<String> line : file) {
				if (line.size() >= 3) {
					Pair<String, String> p = new Pair<String, String>(line.get(0).trim(), line.get(2).trim());
					Pair<String, String> pp = new Pair<String, String>(line.get(2).trim(), line.get(0).trim());
					if (!pairs.contains(p) 
							&& !pairs.contains(pp)) {
						System.out.println(p.first + "\t" + line.get(1) + "\t" + p.second);
					}
				}
			}
		}
	}
}

