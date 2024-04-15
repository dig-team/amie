package amie.data.utils;

import java.io.File;
import java.io.Writer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import amie.data.javatools.administrative.Announce;
import amie.data.javatools.filehandlers.FileLines;
import amie.data.javatools.filehandlers.TSVFile;

/**
 * formats a TSV file into an HTML file
 * @author Fabian M. Suchanek
 *
 */
@SuppressWarnings("deprecation")
public class TsvToHtml {

  public static final Pattern colpat = Pattern.compile("<table\\s+tsv\\s*=\\s*['\"]([^\"']+)['\"]\\s*(?:num\\s*=\\s*['\"]?(\\d+)['\"]?)?");

public static void main(String[] args) throws Exception {
    if (args.length != 3) Announce.help("TsvToHtml <tsvFile> <template> <htmlFile>",
        "<template> should contain a table element on a single line with 'tsv=colname:(l|r), colname:(l|r), ...' num=<numLines>");
    File tsvFile = new File(args[0]);
    File template = new File(args[1]);
    File htmlFile = new File(args[2]);
    Writer out = amie.data.javatools.util.FileUtils.getBufferedUTF8Writer(htmlFile);
    for (String line : new FileLines(template)) {
      out.write(line);
      out.write("\n");
      Matcher m = colpat.matcher(line);
      if (!m.find()) continue;
      String[] columns = m.group(1).split("\\s*,\\s*");
      out.write(" <TR>");
      for (String c : columns) {
        if(c.indexOf(':')==-1) Announce.error("Column name needs l/r indicator:",c);
        out.write("<TH>" + c.substring(0, c.indexOf(':')));
      }
      out.write("\n");
      List<String> tsvColumnNames = null;
      int lines=100;
      if(m.groupCount()>1 && m.group(2)!=null) {
      try {
        lines=Integer.parseInt(m.group(2));
      } catch(Exception e) {
        Announce.error("Could not parse number of lines in",line);
      }
      }
      for (List<String> tsvLine : new TSVFile(tsvFile)) {
        if (tsvColumnNames == null) {
          tsvColumnNames = tsvLine;
          for(String c : columns) {
            if(!tsvColumnNames.contains(c.substring(0,c.indexOf(':')))) Announce.error("Could not find column",c,"in TSV file headers",tsvColumnNames);
          }
          continue;
        }
        if(lines--<0) break;
        out.write("<TR>");
        for (int i = 0; i < columns.length; i++) {
          String align = null;
          if(columns[i].endsWith("r")) align = "right";
          else if(columns[i].endsWith("l")) align = "left";
          else align = "center";
          out.write("<TD align=" + align + ">");
          out.write(tsvLine.get(tsvColumnNames.indexOf(columns[i].substring(0,columns[i].indexOf(':')))));
        }
      }
    }
    out.close();
  }
}
