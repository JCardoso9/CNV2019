import BIT.highBIT.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;

import pt.ulisboa.tecnico.cnv.server.storage.ThreadLocalStorage;

//Counts number of instructions per thread of the whole program
public class ArrayAllocationCount {
  private static PrintStream out = null;
  private static Map<Long, BigInteger> allocationsCounter = new HashMap<Long, BigInteger>();
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
    for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
            Routine routine = (Routine) e.nextElement();
            if (routine.getMethodName().equals("solveImage")){
              routine.addAfter("ArrayAllocationCount", "printAllocCount", routine.getMethodName());
            }
            InstructionArray instructions = routine.getInstructionArray();
      
            for (Enumeration instrs = instructions.elements(); instrs.hasMoreElements(); ) {
              Instruction instr = (Instruction) instrs.nextElement();
              int opcode=instr.getOpcode();
              if ((opcode==InstructionTable.NEW) ||
                (opcode==InstructionTable.newarray) ||
                (opcode==InstructionTable.anewarray) ||
                (opcode==InstructionTable.multianewarray)) {
                instr.addBefore("ArrayAllocationCount", "allocCount", new Integer(1));
              }
            }
    }
    // ci.addAfter("ThreadAwareICount", "printICount", ci.getClassName());
    ci.write(argv[1] + System.getProperty("file.separator") + infilename);
  }

  public static synchronized void printAllocCount(String foo) {
    Long threadID = new Long(Thread.currentThread().getId());
    BigInteger allocationCount = (BigInteger) allocationsCounter.get(threadID);

    try {
      BufferedWriter writer;
      String str = "Request #" + ThreadLocalStorage.get() + " Params " + ThreadLocalStorage.getParams()
          + " allocs " + allocationCount;

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
    allocationsCounter.put(threadID, new BigInteger("0"));
  }

  public static synchronized void allocCount(int instr) {
    Long threadID = new Long(Thread.currentThread().getId());
    BigInteger currentAlloc = (BigInteger) allocationsCounter.get(threadID);
    if (currentAlloc == null) {
      currentAlloc = new BigInteger("0");
    }
    BigInteger newValue = currentAlloc.add(BigInteger.valueOf(instr));
    allocationsCounter.put(threadID, newValue);
  }
}
