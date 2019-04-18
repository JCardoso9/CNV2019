import BIT.highBIT.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;

import pt.ulisboa.tecnico.cnv.server.storage.ThreadLocalStorage;

//Counts number of instructions per thread of the whole program
public class ThreadAwareICount {
  private static PrintStream out = null;
  private static int i_count = 0, b_count = 0, m_count = 0;
  private static Map<Long, BigInteger> instructionCounter = new HashMap<Long, BigInteger>();
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
      routine.addAfter("ThreadAwareICount", "printICount", routine.getMethodName());
      for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements();) {
        BasicBlock bb = (BasicBlock) b.nextElement();
        bb.addBefore("ThreadAwareICount", "count", new Integer(bb.size()));
      }
    }
    // ci.addAfter("ThreadAwareICount", "printICount", ci.getClassName());
    ci.write(argv[1] + System.getProperty("file.separator") + infilename);
  }

  public static synchronized void printICount(String foo) {
    Long threadID = new Long(Thread.currentThread().getId());
    BigInteger instructionCount = (BigInteger) instructionCounter.get(threadID);

    try {
      BufferedWriter writer;
      String str = "Request #" + ThreadLocalStorage.get() + " Params " + ThreadLocalStorage.getParams()
          + " instructions " + instructionCount;

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
    instructionCounter.put(threadID, new BigInteger("0"));
  }

  public static synchronized void count(int instr) {
    Long threadID = new Long(Thread.currentThread().getId());
    BigInteger currentInstr = (BigInteger) instructionCounter.get(threadID);
    if (currentInstr == null) {
      currentInstr = new BigInteger("0");
    }
    BigInteger newValue = currentInstr.add(BigInteger.valueOf(instr));
    instructionCounter.put(threadID, newValue);
  }
}
