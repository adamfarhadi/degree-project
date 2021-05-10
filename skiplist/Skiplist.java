package skiplist;

import java.util.concurrent.ConcurrentSkipListSet;

import abstraction.ConcurrentSet;

public class Skiplist extends ConcurrentSet {

    private ConcurrentSkipListSet<Integer> list;

    public Skiplist() {
        list = new ConcurrentSkipListSet<Integer>();
    }

    public boolean add(int v) {
        return list.add(v);
    }

    public boolean remove(int v) {
        return list.remove(v);
    }

    public boolean contains(int v) {
        return list.contains(v);
    }

    public int size() {
        return list.size();
    }
}