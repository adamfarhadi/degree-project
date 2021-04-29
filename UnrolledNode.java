import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UnrolledNode {
    public volatile int[] keys;
    public volatile int count;
    public volatile int anchor;
    public volatile boolean marked;
    public volatile UnrolledNode next;

    public final ReentrantLock lock;

    // constructor for internal nodes
    public UnrolledNode() {
        this.lock = new ReentrantLock();
        this.keys = new int[Constants.K];
        Arrays.fill(this.keys,Constants.unusedSlot);
    }

    public void lock() {
        this.lock.lock();
    }

    public void unlock() {
        this.lock.unlock();
    }
}
