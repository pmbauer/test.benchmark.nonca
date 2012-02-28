/**
 * The Computer Language Benchmarks Game
 * http://shootout.alioth.debian.org/
 * contributed by Klaus Friedel
 * Modified by Paul M Bauer for clojure/test.benchmark
 */
package alioth.java;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.Exchanger;

public class threadring {
  static final int THREAD_COUNT = 503;
  static final Exchanger outputPipe = new Exchanger();

  public static class MessageThread extends Thread {
    MessageThread nextThread;
    volatile Integer message;

    public MessageThread(MessageThread nextThread, int name) {
      super(""+name);
      this.nextThread = nextThread;
    }

    public void run() {
      while(true) nextThread.enqueue(dequeue());
    }

    public void enqueue(Integer hopsRemaining) {
      if(hopsRemaining == 0){
        try { outputPipe.exchange(Integer.decode(getName())); }
        catch (Exception ignore) { return; }
      }
      // as only one message populates the ring, it's impossible
      // that queue is not empty
      message = hopsRemaining - 1;
      LockSupport.unpark(this); // work waiting...
    }

    private Integer dequeue(){
      while(message == null){
        LockSupport.park();
      }
      Integer msg = message;
      message = null;
      return msg;
    }
  }

  public static int run(int hopCount) throws Exception {
    MessageThread first = null;
    MessageThread last = null;
    for (int i = THREAD_COUNT; i >= 1 ; i--) {
      first = new MessageThread(first, i);
      if(i == THREAD_COUNT) last = first;
    }
    // close the ring:
    last.nextThread = first;

    // start all Threads
    MessageThread t = first;
    do{
      t.start();
      t = t.nextThread;
    }while(t != first);
    // inject message
    first.enqueue(hopCount);
    int ret = ((Integer)outputPipe.exchange(null)).intValue();

    // shutdown threads
    t = first;
    do{
      t.stop(); // yea yea ..
      t = t.nextThread;
    }while(t != first);

    return ret;
  }

  public static void main(String args[]) throws Exception {
    int hopCount = Integer.parseInt(args[0]);
    System.out.println(run(hopCount));
  }
}
