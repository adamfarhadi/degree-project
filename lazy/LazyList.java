package lazy;

import java.util.Collection;
import java.util.Random;

import abstraction.ConcurrentSet;

public class LazyList extends ConcurrentSet {

    final public Node head;
    final public Node tail;

    public LazyList() {
        head = new Node(Integer.MIN_VALUE);
        tail = new Node(Integer.MAX_VALUE);
        head.next = tail;
        tail.next = null;
    }

    private boolean validate(Node pred, Node curr) {
        return !pred.marked && pred.next == curr;
    }

    public boolean add(int v) {
        while (true) {
            Node pred = head;
            Node curr = head.next;
            while (curr.value < v) {
                pred = curr;
                curr = curr.next;
            }
            pred.lock();
            try {
                curr.lock();
                try {
                    if (validate(pred, curr)) {
                        if (curr.value == v) {
                            return false;
                        } else {
                            Node node = new Node(v);
                            node.next = curr;
                            pred.next = node;
                            return true;
                        }
                    }
                } finally {
                    curr.unlock();
                }
            } finally {
                pred.unlock();
            }
        }
    }

    public boolean remove(int v) {
        while (true) {
            Node pred = head;
            Node curr = head.next;
            while (curr.value < v) {
                pred = curr;
                curr = curr.next;
            }
            pred.lock();
            try {
                curr.lock();
                try {
                    if (validate(pred, curr)) {
                        if (curr.value != v) {
                            return false;
                        } else {
                            curr.marked = true;
                            pred.next = curr.next;
                            return true;
                        }
                    }
                } finally {
                    curr.unlock();
                }
            } finally {
                pred.unlock();
            }
        }
    }

    public boolean contains(int v) {
        Node curr = head;
        while (curr.value < v) {
            curr = curr.next;
        }
        return curr.value == v && !curr.marked;
    }

    public int size() {
        int count = 0;

        Node curr = head.next;
        while (curr != tail) {
            curr = curr.next;
            count++;
        }
        return count;
    }
}