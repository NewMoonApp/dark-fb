package com.newmoon.dark.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A utility map from keys to a list of values.
 */
public class MultiHashMap<K, V> extends HashMap<K, List<V>> {

    public MultiHashMap() {
    }

    public MultiHashMap(int size) {
        super(size);
    }

    /**
     * @return Number of items for this key *AFTER* this operation.
     */
    public int putToList(K key, V value) {
        List<V> list = get(key);
        if (list == null) {
            list = new ArrayList<>();
            list.add(value);
            put(key, list);
        } else {
            list.add(value);
        }
        return list.size();
    }

    @Override
    public MultiHashMap<K, V> clone() {
        MultiHashMap<K, V> map = new MultiHashMap<>(size());
        for (Entry<K, List<V>> entry : entrySet()) {
            map.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return map;
    }
}