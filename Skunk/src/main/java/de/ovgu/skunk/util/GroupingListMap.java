package de.ovgu.skunk.util;

import java.util.ArrayList;
import java.util.List;

public class GroupingListMap<K, V> extends GroupingMap<K, V, List<V>> {
    private final int defaultListCapacity;

    public GroupingListMap() {
        this.defaultListCapacity = -1;
    }

    public GroupingListMap(int defaultListCapacity) {
        this.defaultListCapacity = defaultListCapacity;
    }

    @Override
    protected List<V> newCollection() {
        if (defaultListCapacity > 0) {
            return new ArrayList<>(defaultListCapacity);
        } else {
            return new ArrayList<>();
        }
    }
}
