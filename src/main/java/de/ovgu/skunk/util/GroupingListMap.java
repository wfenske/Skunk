package de.ovgu.skunk.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GroupingListMap<K, V> extends GroupingMap<K, V, List<V>> {
    private final int defaultListCapacity;

    public GroupingListMap() {
        this.defaultListCapacity = -1;
    }

    public GroupingListMap(int defaultListCapacity) {
        this.defaultListCapacity = defaultListCapacity;
    }

    private static class EmptyGroupingListMap<K, V> extends GroupingListMap<K, V> {
        @Override
        protected Map<K, List<V>> newMap() {
            return Collections.emptyMap();
        }

        @Override
        protected List<V> newCollection() {
            return Collections.emptyList();
        }
    }

    private static final EmptyGroupingListMap EMPTY_GROUPING_LIST_MAP = new EmptyGroupingListMap();

    public static <K, V> GroupingListMap<K, V> emptyMap() {
        return EMPTY_GROUPING_LIST_MAP;
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
