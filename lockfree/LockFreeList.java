package lockfree;

import java.util.Collection;

/**
 * A Java variant of Harris-Michael that uses run-time type identification to
 * determine whether a node is marked as logically deleted. This is the code
 * used in:
 * 
 * A Concurrency-Optimal List-Based Set. Gramoli, Kuznetsov, Ravi, Shang. 2015.
 * 
 * @author Di Shang
 */
public class LockFreeList {
    private final NodeLinked tail;
    private final NodeLinked head;

    public LockFreeList() {
        tail = new NodeLinked(Integer.MAX_VALUE, null);
        head = new NodeLinked(Integer.MIN_VALUE, tail);
    }

    class Window {
        public NodeBase pred, curr;

        public Window(NodeBase pred, NodeBase curr) {
            this.pred = pred;
            this.curr = curr;
        }
    }

    public Window find(NodeBase head, int value) {
        NodeBase pred = null, curr = null, succ = null;
        boolean snip;
        NodeLinked notMarked;
        retry: while (true) {
            pred = head;
            curr = pred.next();
            while (true) {
                succ = curr.next();
                while (succ instanceof NodeMarked) {
                    notMarked = ((NodeMarked) succ).getNonMarked();
                    snip = pred.casNext(curr, notMarked);
                    if (!snip) {
                        continue retry;
                    }
                    curr = notMarked;
                    succ = curr.next();
                }
                if (curr.value() >= value) {
                    return new Window(pred, curr);
                }
                pred = curr;
                curr = succ;
            }
        }
    }

    public boolean add(int x) {
        while (true) {
            Window window = find(head, x);
            NodeBase pred = window.pred, curr = window.curr;
            if (curr.value() == x) {
                return false;
            } else {
                NodeLinked node = new NodeLinked(x, curr);
                if (pred.casNext(curr, node)) {
                    return true;
                }
            }
        }
    }

    public boolean remove(int x) {
        boolean snip;
        while (true) {
            Window window = find(head, x);
            NodeBase pred = window.pred, curr = window.curr;
            if (curr.value() != x) {
                return false;
            } else {
                NodeBase succ = curr.next();
                if (succ instanceof NodeMarked) {
                    continue;
                }
                snip = curr.casNext(succ, new NodeMarked((NodeLinked) succ));
                if (!snip) {
                    continue;
                }
                pred.casNext(curr, succ);
                return true;
            }
        }
    }

    public boolean contains(int x) {
        NodeBase curr = head;
        while (curr.value() < x) {
            curr = curr.next();
        }
        return (curr.value() == x && !(curr.next() instanceof NodeMarked));
    }

    public int size() {
        NodeBase succ;
        int size = 0;
        for (NodeBase curr = head.next(); curr != tail;) {
            succ = curr.next();
            if (!(succ instanceof NodeMarked)) {
                size++;
            }
            curr = succ;
        }
        return size;
    }
}
