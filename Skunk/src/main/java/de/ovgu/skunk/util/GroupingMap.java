package de.ovgu.skunk.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A map that holds multiple values for the same key.  Used to group elements by keys.
 *
 * @param <K> Type of keys of this map
 * @param <V> Type of values of this map
 * @param <C> Type of collection in which multiple values for the same key are stored
 */
public abstract class GroupingMap<K, V, C extends Collection<V>> {
    protected final Map<K, C> map;

    public GroupingMap() {
        this.map = newMap();
    }

    protected abstract C newCollection();

    protected Map<K, C> newMap() {
        return new HashMap<>();
    }

    public void put(K key, V value) {
        C valuesForKey = ensureMapping(key);
        valuesForKey.add(value);
    }

    public C ensureMapping(K key) {
        C valuesForKey = map.get(key);
        if (valuesForKey == null) {
            valuesForKey = newCollection();
            map.put(key, valuesForKey);
        }
        return valuesForKey;
    }

    /**
     * Remove the given mapping from the map.  If no other values are associated with the same key, also remove the
     * collection for holding the values.
     *
     * @param key
     * @param value
     * @return <code>true</code> if a mapping from key to value existed prior to calling this method
     */
    public boolean remove(K key, V value) {
        C valuesForKey = get(key);
        if (valuesForKey == null) {
            return false;
        }
        boolean removedValue = valuesForKey.remove(value);
        if (valuesForKey.isEmpty()) {
            remove(key);
        }
        return removedValue;
    }

    /**
     * Remove all mappings for the given key from the map.  Return the current collection of values for this
     * key, or <code>null</code>, if the key was unmapped prior to calling this method.
     *
     * @param key The key whose mapping to remove
     * @return The collection of values for this key or <code>null</code> if the key was not mapped.
     */
    public C remove(K key) {
        return map.remove(key);
    }

    public C get(K key) {
        return map.get(key);
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public Map<K, C> getMap() {
        return this.map;
    }
}
