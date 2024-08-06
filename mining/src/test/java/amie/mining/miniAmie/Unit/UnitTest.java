package amie.mining.miniAmie.Unit;

import amie.data.AbstractKB;
import amie.data.KB;
import amie.mining.AMIE;
import amie.mining.miniAmie.miniAMIE;
import amie.mining.utils.AMIEOptions;
import com.google.gson.Gson;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public abstract class UnitTest extends TestCase {
    static final private String RESOURCE_DIR_PATH = "src/test/resources" ;
    static final private String CONFIG_FILE_NAME = "config" ;

    static protected KB kb;

    private File getKBFile () throws FileNotFoundException {
        String configFilePath = String.format("%s/%s", RESOURCE_DIR_PATH, CONFIG_FILE_NAME);
        File configFile = new File(configFilePath) ;
        Scanner configFileReader = new Scanner(configFile);
        String fileName = configFileReader.nextLine() ;
        String filePath = String.format("%s/%s", RESOURCE_DIR_PATH, fileName);
        return new File (filePath);
    }

    private void miniAmieKBSetup () throws IOException {
        List<File> dataFiles = new ArrayList<> ();
        File kbFile = getKBFile ();
        dataFiles.add (kbFile);
        kb = new KB () ;
        kb.load (dataFiles);
        miniAMIE.kb = kb ;
    }

    protected void setUp() throws Exception {
        super.setUp () ;
        if (kb != null)
            return;
        miniAmieKBSetup () ;
    }
}
