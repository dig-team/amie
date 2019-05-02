/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

/**
 *
 * @author jlajus
 */

public abstract class MTCore<T> extends Thread {
    
    protected static int nThreads = 1;
    
    public MTCore() {}
    
    public class MTWorker extends Thread {
        
        protected BlockingQueue<T> queryQ;
        protected Object[] params;
        
        public MTWorker(BlockingQueue<T> queryQ, Object... params) {
            this.queryQ = queryQ;
            this.params = params;
        }
        
        public void run() {
            T q;
            while(true) {
                try {
                    q = queryQ.take();
                    if (q.equals(endToken())) break;
                    runQ(q, params);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MTWorker.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                } catch (IOException ex) {
                    Logger.getLogger(MTWorker.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    protected abstract void runQ(T query, Object[] params) throws IOException;
    public abstract T endToken();
    
    public static Option getMTOption() {
        return OptionBuilder.withArgName("nc")
            .hasArg()
            .withDescription("Number of core")
            .create("nc");
    }
    
    public static void parseMTOption(CommandLine cli) {
        int nProcessors = Runtime.getRuntime().availableProcessors();
        nThreads = Math.max(1, nProcessors - 1);
        if (cli.hasOption("nc")) {
            String nCoresStr = cli.getOptionValue("nc");
            try {
                nThreads = Math.max(Math.min(nThreads, Integer.parseInt(nCoresStr)), 1);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -nc (number of threads) must be an integer");
                System.exit(1);
            }
        }
    }
    
    public void work(BlockingQueue<T> queryQ, Object... params) throws InterruptedException {
        long timeStamp1 = System.currentTimeMillis();
        List<Thread> threadList = new ArrayList<>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            threadList.add(new MTWorker(queryQ, params));
            queryQ.add(endToken());
        }
        
        for (Thread thread : threadList) {
            thread.start();
        }
        
        for (Thread thread : threadList) {
            thread.join();
        }
        long timeStamp2 = System.currentTimeMillis();
        System.out.println("Processing done with "+Integer.toString(nThreads)+" threads in "+Long.toString(timeStamp2 - timeStamp1)+"ms.");
    }
    
}
