import BIT.highBIT.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;

import pt.ulisboa.tecnico.cnv.storage.*;

//Counts number of instructions per thread of the whole program
public class BasicBlockCount {
  private static PrintStream out = null;
  private static int b_count = 0;
  private static Map<Long, Long> basicBlockCounter = new HashMap<Long, Long>();
  private static Integer i = 0;

  /*
   * main reads in all the files class files present in the input directory,
   * instruments them, and outputs them to the specified output directory.
   */
  public static void main(String argv[]) {
    String infilename = new String(argv[0]);
    String outfilename = new String(argv[1]);
    ClassInfo ci = new ClassInfo(infilename);
    // loop through all the routines
    // see java.util.Enumeration for more information on Enumeration class
    for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements();) {
      Routine routine = (Routine) e.nextElement();
      if (!routine.getMethodName().equals("solveImage")) {
        continue;
      }
      routine.addAfter("BasicBlockCount", "printBBCount", routine.getMethodName());
      for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements();) {
        BasicBlock bb = (BasicBlock) b.nextElement();
        bb.addBefore("BasicBlockCount", "count", new Integer(1));
      }
    }
    // ci.addAfter("ThreadAwareICount", "printICount", ci.getClassName());
    ci.write(argv[1] + System.getProperty("file.separator") + infilename);
  }

  public static synchronized void printBBCount(String foo) {
    long threadID = Thread.currentThread().getId();
    long basicBlockCount = (long) basicBlockCounter.get(threadID);

    /*try {
      BufferedWriter writer;
      
      String str = "Thread ID: " + threadID + " |  Params: " + ThreadLocalStorage.getParams().toString()
          + " basic blocks " + basicBlockCount;

      if (new File("metrics.txt").isFile()) {
        writer = new BufferedWriter(new FileWriter("metrics.txt", true));
      } else {
        writer = new BufferedWriter(new FileWriter("metrics.txt"));
      }

      System.out.println(str);
      writer.write(str);
      writer.newLine();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    basicBlockCounter.put(threadID, new BigInteger("0"));*/
    DynamoDBStorage.storeMetricsGathered(threadID, basicBlockCount);

  }

  public static synchronized void count(int instr) {
    long threadID = Thread.currentThread().getId();
    long currentBB = 0;
    if (basicBlockCounter.get(threadID) != null) {
      currentBB = basicBlockCounter.get(threadID);
    }
    long newValue = currentBB + instr;
    basicBlockCounter.put(threadID, newValue);
  }
}
