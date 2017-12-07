/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import java.io.File;
import java.io.IOException;
import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.parsers.NumberFormatter;

/**
 *
 * @author jlajus
 */
public class WikidataSimpleTypingKB extends SimpleTypingKB {

    @Override
    protected void load(File f, String message)
            throws IOException {
        long size = size();
        if (f.isDirectory()) {
            long time = System.currentTimeMillis();
            Announce.doing("Loading files in " + f.getName());
            for (File file : f.listFiles()) {
                load(file);
            }
            Announce.done("Loaded "
                    + (size() - size)
                    + " facts in "
                    + NumberFormatter.formatMS(System.currentTimeMillis()
                            - time));
        }
        for (String line : new FileLines(f, "UTF-8", message)) {
            if (line.endsWith(".")) {
                line = Char17.cutLast(line);
            }
            String[] split = line.trim().split(">" + delimiter);
            if (split.length == 3) {
                add(split[0].trim() + ">", split[1].trim() + ">", split[2].trim());
            } else if (split.length == 4) {
                add(split[0].trim() + ">", split[1].trim() + ">", split[2].trim() + ">");
            }
        }

        if (message != null) {
            Announce.message("     Loaded", (size() - size), "facts");
        }
    }

}
