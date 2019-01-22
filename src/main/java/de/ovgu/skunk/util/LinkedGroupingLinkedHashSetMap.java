package de.ovgu.skunk.util;

import java.util.LinkedHashSet;
import java.util.Set;

public class LinkedGroupingLinkedHashSetMap<K, V> extends LinkedGroupingMap<K, V, Set<V>> {
    @Override
    protected Set<V> newCollection() {
        return new LinkedHashSet<>();
    }
}
