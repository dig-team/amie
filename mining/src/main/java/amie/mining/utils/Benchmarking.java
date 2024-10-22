package amie.mining.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public abstract class Benchmarking {
    public static int PeakMemory() throws IOException, InterruptedException {
        long pid = ProcessHandle.current().pid();
        String command = "cat /proc/"+pid+"/status" ;
        Process process = Runtime
                .getRuntime()
                .exec(command) ;
        process.waitFor();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;
        String strVmRSS = "-1";
        String strVmHWM = "-1"  ;
        while ((line = reader.readLine()) != null) {
            if (line.contains("VmHWM")) {
//                System.err.println(Arrays.toString(line.split("[ ]+")));
                strVmHWM = line.split("[ ]+")[1];
            }
        }
        reader.close();
        return Integer.parseInt(strVmHWM);
    }
}
