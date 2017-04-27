package amie.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javatools.filehandlers.TSVFile;

public class RemoveLanguageCode {

	public static void main(String[] args) throws IOException {
		try (TSVFile file = new TSVFile(new File(args[0]))) {
			for (List<String> line : file) {
				if (!line.get(1).equals("<hasLanguageCode>")) {
					System.out.println(line.get(0) + "\t" + line.get(1) + "\t" + line.get(2));
				}
			}
		}
	}
}
