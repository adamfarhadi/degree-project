import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;

public class UnrolledList {
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
        // System.out.println("merge called");
        // System.out.println(Arrays.toString(curr.keys));
        // System.out.println(Arrays.toString(succ.keys));

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
        // System.out.println("redistribute called");

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

            System.out.println("### add() 1: pred.lock(), node=" + pred + " time:" + System.nanoTime() + " thread="
                    + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
            pred.lock();

            if (!validate(pred, curr)) {
                System.out.println("### add() 2: pred.unlock(), node=" + pred + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
                pred.unlock();
                continue;
            }

            if (seek(curr, key) != -1) {
                System.out.println("### add() 3: pred.unlock(), node=" + pred + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
                pred.unlock();
                return false;
            }

            int slot = seek(curr, Constants.unusedSlot);
            if (slot != -1) {
                curr.keys[slot] = key;
                curr.count++;
            } else {
                System.out.println("### add() 4: curr.lock(), node=" + curr + " time:" + System.nanoTime() + " thread="
                        + Thread.currentThread().getId() + " holdcount=" + curr.lock.getHoldCount());
                curr.lock();
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
                System.out.println("### add() 5: curr.unlock(), node=" + curr + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + curr.lock.getHoldCount());
                curr.unlock();
            }
            System.out.println("### add() 6: pred.unlock(), node=" + pred + " time:" + System.nanoTime() + " thread="
                    + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
            pred.unlock();
            return true;
        }
    }

    public boolean remove(int key) {
        System.out.println("remove(" + key + ")");
        while (true) {
            UnrolledNodePair temp = scan(key);
            UnrolledNode pred = temp.node1;
            UnrolledNode curr = temp.node2;

            // int x = pred.lock.getHoldCount();
            // if (x > 0) {
            // System.out.println("test");
            // }

            System.out
                    .println("### remove() 1: pred.lock(), node=" + pred + " key= " + key + " time:" + System.nanoTime()
                            + " thread=" + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
            pred.lock();

            if (!validate(pred, curr)) {
                System.out.println("### remove() 2: pred.unlock(), node=" + pred + " key= " + key + " time:"
                        + System.nanoTime() + " thread=" + Thread.currentThread().getId() + " holdcount="
                        + pred.lock.getHoldCount());
                pred.unlock();
                continue;
            }

            int slot = seek(curr, key);
            if (slot == -1) {
                System.out.println("### remove() 3: pred.unlock(), node=" + pred + " key= " + key + " time:"
                        + System.nanoTime() + " thread=" + Thread.currentThread().getId() + " holdcount="
                        + pred.lock.getHoldCount());
                pred.unlock();
                return false;
            }
            curr.keys[slot] = Constants.unusedSlot;
            curr.count--;

            UnrolledNode succ = curr.next;
            if (curr.count < Constants.MINFULL) {
                System.out.println("### remove() 4: curr.lock(), node=" + curr + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + curr.lock.getHoldCount());
                curr.lock();
                System.out.println("### remove() 5: succ.lock(), node=" + pred + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
                succ.lock();
                if (curr.count == 0) {
                    if (curr.anchor != Constants.sentinalMin) {
                        curr.marked = true;
                        pred.next = succ;
                    }
                    System.out.println("### remove() 6: succ.unlock(), node=" + succ + " time:" + System.nanoTime()
                            + " thread=" + Thread.currentThread().getId() + " holdcount=" + succ.lock.getHoldCount());
                    succ.unlock();
                    System.out.println("### remove() 7: curr.unlock(), node=" + curr + " time:" + System.nanoTime()
                            + " thread=" + Thread.currentThread().getId() + " holdcount=" + curr.lock.getHoldCount());
                    curr.unlock();
                    System.out.println("### remove() 8: pred.unlock(), node=" + pred + " time:" + System.nanoTime()
                            + " thread=" + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
                    pred.unlock();
                    return true;
                }
                succ = curr.next;

                if (succ.anchor == Constants.sentinalMax) {
                    System.out.println("### remove() 9: succ.unlock(), node=" + succ + " time:" + System.nanoTime()
                            + " thread=" + Thread.currentThread().getId() + " holdcount=" + succ.lock.getHoldCount());
                    succ.unlock();
                    System.out.println("### remove() 10: curr.unlock(), node=" + curr + " time:" + System.nanoTime()
                            + " thread=" + Thread.currentThread().getId() + " holdcount=" + curr.lock.getHoldCount());
                    curr.unlock();
                    System.out.println("### remove() 11: pred.unlock(), node=" + pred + " time:" + System.nanoTime()
                            + " thread=" + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
                    pred.unlock();
                    return true;
                }
                // succ.lock();
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
                System.out.println("### remove() 12: succ.unlock(), node=" + succ + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + succ.lock.getHoldCount());
                succ.unlock();
                System.out.println("### remove() 13: curr.unlock(), node=" + curr + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + curr.lock.getHoldCount());
                curr.unlock();
                System.out.println("### remove() 14: pred.unlock(), node=" + pred + " time:" + System.nanoTime()
                        + " thread=" + Thread.currentThread().getId() + " holdcount=" + pred.lock.getHoldCount());
                pred.unlock();
                return true;
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

    public static void main(String[] args) {
        UnrolledList unrolledList = new UnrolledList();
        ConcurrentSkipListSet<Integer> javaList = new ConcurrentSkipListSet<Integer>();
        int range = 250000;
        int initialKeys = 10000;
        int numOps = 100000;

        // initialize
        for (int i = 0; i < initialKeys; i++) {
            int x = ThreadLocalRandom.current().nextInt(0, range + 1);
            unrolledList.add(x);
            javaList.add(x);
        }

        for (int i = 0; i < numOps; i++) {
            int coin = ThreadLocalRandom.current().nextInt(0, 100 + 1);
            int x = ThreadLocalRandom.current().nextInt(0, range + 1);

            if (coin <= 20) {
                unrolledList.add(x);
                javaList.add(x);
            } else if (coin > 20 && coin <= 40) {
                unrolledList.remove(x);
                javaList.remove(x);
            } else {
                unrolledList.contains(x);
                javaList.contains(x);
            }
        }
        // unrolledList.print();
        System.out.println(unrolledList.size());
        System.out.println(javaList.size());
    }
}
