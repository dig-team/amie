package amie.mining.miniAmie.Unit;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.miniAmie.miniAMIE;
import amie.mining.miniAmie.utils;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static amie.mining.miniAmie.utils.miningAssistant;


public abstract class UnitTest extends TestCase {
    static protected KB kb;
    private void miniAmieKBSetup () throws IOException {
        kb = new KB () ;
        miniAMIE.Kb = kb ;
        utils.miningAssistant = new DefaultMiningAssistant(kb);
    }

    protected void setUp() throws Exception {
        super.setUp () ;
        if (kb != null)
            return;
        miniAmieKBSetup () ;
    }
}
