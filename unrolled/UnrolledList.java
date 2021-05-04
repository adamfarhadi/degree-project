package unrolled;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;

import abstraction.ConcurrentSet;

public class UnrolledList extends ConcurrentSet {
    public final UnrolledNode head;
    public final UnrolledNode tail;

    public UnrolledList() {

        head = new UnrolledNode();
        tail = new UnrolledNode();
        UnrolledNode mid = new UnrolledNode();

        head.next = mid;
        mid.next = tail;
        tail.next = null;

        head.anchor = Constants.sentinalMin;
        mid.anchor = Constants.sentinalMin;
        tail.anchor = Constants.sentinalMax;
    }

    private UnrolledNodePair scan(int key) {
        UnrolledNode pred = head;
        UnrolledNode curr = pred.next;
        UnrolledNode succ = curr.next;

        while (true) {
            if (succ.anchor > key)
                break;
            pred = curr;
            curr = succ;
            succ = succ.next;
        }

        return new UnrolledNodePair(pred, curr);
    }

    private int seek(UnrolledNode curr, int key) {
        for (int i = 0; i < Constants.K; i++) {
            if (curr.keys[i] == key)
                return i;
        }

        return -1;
    }

    private boolean validate(UnrolledNode pred, UnrolledNode curr) {
        return !pred.marked && !curr.marked && pred.next == curr;
    }

    private UnrolledNodePair split(UnrolledNode curr) {
        UnrolledNode new1 = new UnrolledNode();
        UnrolledNode new2 = new UnrolledNode();

        Arrays.sort(curr.keys);

        // initialize new1
        for (int i = 0; i < Constants.K / 2; i++) {
            new1.keys[i] = curr.keys[i];
        }
        new1.anchor = curr.anchor;
        new1.count = Constants.K / 2;
        new1.next = new2;

        // initialize new2
        for (int i = 0; i < Constants.K / 2; i++) {
            new2.keys[i] = curr.keys[i + Constants.K / 2];
        }
        new2.anchor = new2.keys[0];
        new2.count = Constants.K / 2;
        new2.next = curr.next;

        return new UnrolledNodePair(new1, new2);
    }

    private UnrolledNode merge(UnrolledNode curr, UnrolledNode succ) {
        UnrolledNode new1 = new UnrolledNode();
        int slot = 0;
        for (int i = 0; i < Constants.K; i++) {
            if (curr.keys[i] != Constants.unusedSlot) {
                new1.keys[slot] = curr.keys[i];
                slot++;
            }

            if (succ.keys[i] != Constants.unusedSlot) {
                new1.keys[slot] = succ.keys[i];
                slot++;
            }
        }

        new1.anchor = curr.anchor;
        new1.count = curr.count + succ.count;
        new1.next = succ.next;
        return new1;
    }

    private UnrolledNodePair redistribute(UnrolledNode curr, UnrolledNode succ) {
        int M;
        if ((curr.count + succ.count) % 2 == 0) {
            M = (curr.count + succ.count) / 2 - curr.count;
        } else {
            M = (curr.count + succ.count) / 2 + 1 - curr.count;
        }

        // Find the M-th smallest key in succ
        int[] temp = new int[succ.count];
        int slot = 0;
        for (int i = 0; i < Constants.K; i++) {
            if (succ.keys[i] != Constants.unusedSlot) {
                temp[slot] = succ.keys[i];
                slot++;
            }
        }
        Arrays.sort(temp);

        // Create two new nodes new1 and new2
        UnrolledNode new1 = new UnrolledNode();
        UnrolledNode new2 = new UnrolledNode();

        // initialize new1
        new1.count = M + curr.count;
        // copy all valid keys in curr to new1
        slot = 0;
        for (int i = 0; i < Constants.K; i++) {
            if (curr.keys[i] != Constants.unusedSlot) {
                new1.keys[slot++] = curr.keys[i];
            }
        }
        // copy all valid keys in succ whose key value is at most K to new1
        for (int i = 0; i < M; i++) {
            new1.keys[slot++] = temp[i];
        }
        new1.anchor = curr.anchor;
        new1.next = new2;

        // initialize new2
        new2.count = (curr.count + succ.count) / 2;
        // Copy all valid keys in succ whose key value is strictly greater than K to
        // new2
        slot = 0;
        for (int i = M; i < succ.count; i++) {
            new2.keys[slot++] = temp[i];
        }
        new2.anchor = new2.keys[0];
        new2.next = succ.next;

        return new UnrolledNodePair(new1, new2);
    }

    public boolean add(int key) {
        while (true) {
            UnrolledNodePair temp = scan(key);
            UnrolledNode pred = temp.node1;
            UnrolledNode curr = temp.node2;

            pred.lock();

            try {
                if (!validate(pred, curr)) {
                    continue;
                }

                if (seek(curr, key) != -1) {
                    return false;
                }

                int slot = seek(curr, Constants.unusedSlot);
                if (slot != -1) {
                    curr.keys[slot] = key;
                    curr.count++;
                } else {
                    curr.lock();
                    try {
                        UnrolledNodePair temp2 = split(curr);
                        UnrolledNode new1 = temp2.node1;
                        UnrolledNode new2 = temp2.node2;

                        if (key < new2.anchor) {
                            new1.keys[Constants.K / 2] = key;
                            new1.count++;
                        } else {
                            new2.keys[Constants.K / 2] = key;
                            new2.count++;
                        }

                        curr.marked = true;
                        pred.next = new1;
                    } finally {
                        curr.unlock();
                    }
                }
                return true;
            } finally {
                pred.unlock();
            }
        }
    }

    public boolean remove(int key) {
        while (true) {
            UnrolledNodePair temp = scan(key);
            UnrolledNode pred = temp.node1;
            UnrolledNode curr = temp.node2;

            pred.lock();

            try {
                if (!validate(pred, curr)) {
                    continue;
                }

                int slot = seek(curr, key);
                if (slot == -1) {
                    return false;
                }
                curr.keys[slot] = Constants.unusedSlot;
                curr.count--;

                if (curr.count < Constants.MINFULL) {
                    curr.lock();
                    try {
                        UnrolledNode succ = curr.next;

                        if (curr.count == 0) {
                            if (curr.anchor != Constants.sentinalMin) {
                                curr.marked = true;
                                pred.next = succ;
                            }
                            return true;
                        }

                        if (succ.anchor == Constants.sentinalMax) {
                            return true;
                        }

                        succ.lock();
                        try {
                            UnrolledNode new1;
                            if (curr.count + succ.count < Constants.MAXMERGE) {
                                new1 = merge(curr, succ);
                            } else {
                                UnrolledNodePair temp2 = redistribute(curr, succ);
                                new1 = temp2.node1;
                            }
                            curr.marked = true;
                            succ.marked = true;
                            pred.next = new1;
                            return true;
                        } finally {
                            succ.unlock();
                        }
                    } finally {
                        curr.unlock();
                    }
                }
                return true;
            } finally {
                pred.unlock();
            }
        }
    }

    public boolean contains(int key) {
        UnrolledNode curr = scan(key).node2;

        int slot = seek(curr, key);
        if (slot >= 0) {
            int slotKey = curr.keys[slot];
            if (slotKey == key)
                return true;
        }
        return false;
    }

    public synchronized void print() {
        UnrolledNode curr = this.head;
        while (curr != null) {
            System.out.print(curr + ": A=" + curr.anchor + ", C=" + curr.count);
            System.out.print(", keys=[");
            for (int i = 0; i < Constants.K; i++) {
                if (i == Constants.K - 1) {
                    if (curr.keys[i] == Constants.unusedSlot)
                        System.out.print("E");
                    else
                        System.out.print(curr.keys[i]);
                    continue;
                }
                if (curr.keys[i] == Constants.unusedSlot)
                    System.out.print("E,");
                else
                    System.out.print(curr.keys[i] + ",");
            }
            System.out.println("] -> " + curr.next);
            curr = curr.next;
        }
    }

    public synchronized int size() {
        UnrolledNode curr = this.head;
        int count = 0;
        while (curr != null) {
            count += curr.count;
            curr = curr.next;
        }
        return count;
    }

    public synchronized int size2() {
        UnrolledNode curr = this.head;
        int count = 0;
        while (curr != null) {
            for (int i = 0; i < Constants.K; i++) {
                if(curr.keys[i] != Constants.unusedSlot)
                    count++;
            }
            curr = curr.next;
        }
        return count;
    }

    public static void main(String[] args) {
        UnrolledList unrolledList = new UnrolledList();
        ConcurrentSkipListSet<Integer> javaList = new ConcurrentSkipListSet<Integer>();
        int range = 20000;
        int initialKeys = 10000;
        int numOps = 10000;
        int numThreads = 6;

        // initialize
        for (int i = 0; i < initialKeys; i++) {
            int x = ThreadLocalRandom.current().nextInt(0, range + 1);
            unrolledList.add(x);
            javaList.add(x);
        }

        System.out.println(unrolledList.size());
        System.out.println(javaList.size());

        Runnable runnable = () -> {
            for (int i = 0; i < numOps / numThreads; i++) {
                int coin = ThreadLocalRandom.current().nextInt(0, 100 + 1);
                int x = ThreadLocalRandom.current().nextInt(0, range + 1);

                if (coin <= 50) {
                    unrolledList.add(x);
                    javaList.add(x);
                } else if (coin > 50 && coin <= 100) {
                    unrolledList.remove(x);
                    javaList.remove(x);
                } else {
                    unrolledList.contains(x);
                    javaList.contains(x);
                }
            }
        };

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.print(unrolledList.size() + " " + unrolledList.size2() + "\n");
        System.out.println(javaList.size());
    }
}
