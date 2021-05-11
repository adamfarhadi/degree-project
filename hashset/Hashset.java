package hashset;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import abstraction.ConcurrentSet;

public class Hashset extends ConcurrentSet {

    Set<Integer> set;

    public Hashset() {
        set = ConcurrentHashMap.newKeySet();
    }

    public boolean add(int v) {
        return set.add(v);
    }

    public boolean remove(int v) {
        return set.remove(v);
    }

    public boolean contains(int v) {
        return set.contains(v);
    }

    public int size() {
        return set.size();
    }
}