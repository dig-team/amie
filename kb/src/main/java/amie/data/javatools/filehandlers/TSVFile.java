package amie.data.javatools.filehandlers;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).

This class iterates over the lines in a TSV file.<BR>
Example:
<PRE>
for(List&lt;String> line : new TSVFile("blah.tsv")) {
  System.out.println(line);
}
</PRE>
*/

public class TSVFile implements Iterable<List<String>>, Iterator<List<String>>, Closeable{

/** Holds the reader*/
  protected FileLines in;

  public TSVFile(File f) throws IOException {
    in=new FileLines(f);
  }

  @Override
    public Iterator<List<String>> iterator() {
      return this;
    }
  
  @Override
    public List<String> next() {
      return Arrays.asList(in.next().split("\t"));
    }
  @Override
    public boolean hasNext() {
      return in.hasNext();
    }
  
  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

  @Override
    public void close() throws IOException {
     in.close(); 
    }
  
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove on TSVFile");
  }

}
