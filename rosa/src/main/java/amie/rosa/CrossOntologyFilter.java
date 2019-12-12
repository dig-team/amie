package amie.rosa;

import amie.data.KB;
import java.io.File;
import java.io.IOException;
import java.util.List;

import amie.rules.AMIEParser;
import amie.rules.Rule;

import javatools.filehandlers.TSVFile;

public class CrossOntologyFilter {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub	
		TSVFile tsv = new TSVFile(new File(args[0]));
		for(List<String> line : tsv){
			Rule rule = AMIEParser.rule(line.get(0));
			int r1, r2;
			r1 = rule.getHead()[1];
			r2 = rule.getBody().get(0)[1];
			if((KB.unmap(r1).startsWith("<dbo:") && !KB.unmap(r2).startsWith("<dbo:")) || 
				(KB.unmap(r2).startsWith("<dbo:") && !KB.unmap(r1).startsWith("<dbo:"))) {
				System.out.print(line.get(0));
				for (int i = 1; i < line.size(); ++i) {
					System.out.print("\t" + line.get(i));
				}
				System.out.println();
			}
		}
		tsv.close();
	}

}
