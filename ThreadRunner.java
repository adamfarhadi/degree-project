import java.util.concurrent.ThreadLocalRandom;

import abstraction.ConcurrentSet;

public class ThreadRunner implements Runnable {
    private volatile boolean stop = false;
    private int ratioAdds, ratioRemoves, ratioContains;
    ConcurrentSet list;
    int range;

    public long numCompletedOps = 0;

    public ThreadRunner(ConcurrentSet list, int range, int ratioAdds, int ratioRemoves, int ratioContains) {
        this.list = list;
        this.range = range;
        this.ratioAdds = ratioAdds;
        this.ratioRemoves = ratioRemoves;
        this.ratioContains = ratioContains;
    }

    public void stopThread() {
        this.stop = true;
    }

    public void run() {
        while(!stop) {
            int coin = ThreadLocalRandom.current().nextInt(0, 100 + 1);
            int randomInt = ThreadLocalRandom.current().nextInt(0, range + 1);
            if(coin <= ratioAdds) {
                list.add(randomInt);
                numCompletedOps++;
            } else if (coin > ratioAdds && coin <= (ratioAdds+ratioRemoves)) {
                list.remove(randomInt);
                numCompletedOps++;
            } else {
                list.contains(randomInt);
                numCompletedOps++;
            }
        }
    }
}
