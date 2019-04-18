package pt.ulisboa.tecnico.cnv.server.storage;

import java.util.concurrent.atomic.AtomicInteger;
import pt.ulisboa.tecnico.cnv.parser.*;

public class ThreadLocalStorage {
  // Atomic integer containing the next thread ID to be assigned
  private static final AtomicInteger nextId = new AtomicInteger(0);

  // Thread local variable containing each thread's ID
  private static final ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return nextId.getAndIncrement();
    }
  };

  private static final ThreadLocal<Request> requestParams = new ThreadLocal<Request>();

  // Returns the current thread's unique ID, assigning it if necessary
  public static int get() {
    return threadId.get();
  }

  public static void setParams(String params) {
    QueryParser parser = new QueryParser(params);
    requestParams.set(parser.getRequest());
  }

  public static Request getParams() {
    return requestParams.get();
  }
}
