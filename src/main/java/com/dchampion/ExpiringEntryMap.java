package com.dchampion;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A map (i.e. "dictionary") with expiring entries. Users of this class should supply their own
 * concrete {@link Map} implementation to this class's constructor, and an optional parameter
 * specifying the time after which the map's entries should expire. If the optional "lifetime"
 * parameter is omitted, the map's entries will be set to expire after a default lifetime of 60
 * minutes.
 * <p>
 * Users can construct an instance of this class via its static {@link Builder} methods, and use it
 * as they would any standard {@link Map} implementation; e.g.,
 * <pre>
 * // Construct a java.util.HashMap instance and populate it with entries.
 * Map&lt;Integer,String> myHashMap = new HashMap<>();
 * myHashMap.put(1, "one");
 * myHashMap.put(2, "two");
 * 
 * // Construct an ExpiringEntryMap backed by the HashMap, with entries that will
 * // be removed (i.e., expire) 100 milliseconds after they have been put into the
 * // map. Note the lifetime() method can be omitted, in which case the map's entries
 * // will be set to expire after a default lifetime of 60 minutes.
 * Map&lt;Integer,String> myExpiringEntryMap = 
 *      ExpiringEntryMap.Builder.map(myHashMap).lifetime(TimeUnit.MILLISECONDS, 100).build();
 * 
 * // Use the ExpiringEntryMap as you would use any other HashMap.
 * if (myExpiringEntryMap.get(1).equals("one")) {
 *      myExpiringEntryMap.put(1, "ONE");
 *      assert myExpiringEntryMap.get(1).equals("ONE");
 * }
 * 
 * // Sleep for 100 milliseconds; a call to isEmpty() should return true, as the previous entries
 * // should have by now expired.
 * Thread.sleep(100);
 * assert myExpiringEntryMap.isEmpty();
 * </pre>
 * Users can expect instances of this class to behave in a manner consistent with the {@link Map}
 * implementation supplied to its constructor; that is, instances of {@link HashMap},
 * {@link java.util.LinkedHashMap}, {@link java.util.TreeMap}, or any concrete {@link Map}
 * implementation for that matter, will behave as expected when wrapped by an instance of this class.
 */
public final class ExpiringEntryMap<K,V> implements Map<K,V> {
    
    @SuppressWarnings("rawtypes")
    Map wrapper = null;
    
    /**
     * Lifetime of map values (in milliseconds).
     */
    long lifetime;

    /**
     * A wrapper that timestamps map values.
     *
     * @param <V> The map's value.
     */
    private static final class TimestampedValue<V> {
        final V value;
        final long timestamp;

        TimestampedValue(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        V getValue() {
            return value;
        }

        long getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof TimestampedValue)) {
                return false;
            }
            TimestampedValue<?> other = (TimestampedValue<?>) o;
            if (other.getValue() != null && getValue() == null) {
                return false;
            }
            if (other.getValue() == null && getValue() != null) {
                return false;
            }
            if (other.getValue() == null && getValue() == null) {
                return true;
            }
            return other.getValue().equals(getValue());
        }

        @Override
        public int hashCode() {
            return getValue() != null ? getValue().hashCode() : 31;
        }
    }

    /**
     * Use this class to construct instances of {@link ExpiringEntryMap}.
     */
    public static final class Builder<K,V> {
        private Map<K,V> map;
        private long lifetimeInMillis = 60 * 60 * 1000;

        /**
         * Returns an instance of this {@link ExpiringEntryMap}'s builder.
         * 
         * @param <K> The data type of the map's keys
         * @param <V> The data type of the map's values
         * @param map The map, which can be empty or prepopulated with entries.
         * 
         * @return An instance of this builder.
         * 
         * @throws IllegalArgumentException if the supplied map is null.
         */
        public static <K,V> Builder<K,V> map(Map<K,V> map) {
            if (map == null) {
                throw new IllegalArgumentException("map cannot be null");
            }
            Builder<K,V> builder = new Builder<K,V>(map);
            return builder;
        }

        /**
         * Specifies the lifetime of this {@link ExpiringEntryMap}'s entries. This method
         * is optional and, if omitted, the map's entries will be set to expire after a default
         * lifetime of 60 minutes.
         * 
         * @param unit The {@link TimeUnit}
         * @param lifetime The number of {@link TimeUnit}s after which this map's entries will expire.
         * 
         * @return An instance of this builder.
         * 
         * @throws IllegalArgumentException if the supplied unit is either null or finer than
         * {@link TimeUnit#MILLISECONDS} resolution, or if lifetime is less than 1.
         */
        public Builder<K,V> lifetime(TimeUnit unit, long lifetime) {
            if (unit == null) {
                throw new IllegalArgumentException("unit must be specified");
            }
            if (unit == TimeUnit.MICROSECONDS || unit == TimeUnit.NANOSECONDS) {
                throw new IllegalArgumentException("unit must be of resolution " + TimeUnit.MILLISECONDS + " or greater");
            }
            if (lifetime < 1) {
                throw new IllegalArgumentException("lifetime must be one " + unit + " or greater");
            }
            convertLifetimeToMillis(unit, lifetime);
            return this;
        }

        /**
         * The terminal operation of this builder, which returns a new {@link ExpriringEntryMap} instance.
         * 
         * @return An {@link ExpiringEntryMap} instance.
         */
        public ExpiringEntryMap<K,V> build() {
            return new ExpiringEntryMap<>(this.map, this.lifetimeInMillis);
        }

        private Builder(Map<K,V> map) {
            this.map = map;
        }

        private void convertLifetimeToMillis(TimeUnit unit, long lifetimeInTimeUnit) {
            switch(unit) {
                case DAYS:
                    lifetimeInMillis = lifetimeInTimeUnit * 24 * 60 * 60 * 1_000;
                    break;
                case HOURS:
                    lifetimeInMillis = lifetimeInTimeUnit * 60 * 60 * 1_000;
                    break;
                case MINUTES:
                    lifetimeInMillis = lifetimeInTimeUnit * 60 * 1_000;
                    break;
                case SECONDS:
                    lifetimeInMillis = lifetimeInTimeUnit * 1_000;
                    break;
                case MILLISECONDS:
                    lifetimeInMillis = lifetimeInTimeUnit;
                    break;
                default:
                    break;
            }
        }
    }

    private ExpiringEntryMap(Map<K,V> map, long lifetime) {
        assert map != null;
        assert lifetime > 0;
        try {
            wrapper = map.getClass().getDeclaredConstructor((Class<?>[])null).newInstance((Object[])null);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Unable to instantiate map of type "
                + map.getClass().getTypeName() + " using no-arg constructor", e);
        }
        this.putAll(map);
        this.lifetime = lifetime;
    }

    @Override
    public int size() {
        flush();
        return wrapper.size();
    }

    @Override
    public boolean isEmpty() {
        flush();
        return wrapper.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        flush();
        return wrapper.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsValue(Object value) {
        flush();
        for (Entry<K,TimestampedValue<V>> entry : (Set<Entry<K,TimestampedValue<V>>>) wrapper.entrySet()) {
            V val = entry.getValue().getValue();
            if (val != null && val.equals(value)) {
                return true;
            }
            if (val == null && value == null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        flush();
        @SuppressWarnings("unchecked")
        TimestampedValue<V> tv = (TimestampedValue<V>) wrapper.get(key);
        return tv != null ? tv.getValue() : null;
    }

    @Override
    public V put(K key, V value) {
        flush();
        @SuppressWarnings("unchecked")
        TimestampedValue<V> tv = (TimestampedValue<V>) wrapper.put(key, new TimestampedValue<V>(value, now()));
        return tv != null ? tv.getValue() : null;
    }

    @Override
    public V remove(Object key) {
        flush();
        @SuppressWarnings("unchecked")
        TimestampedValue<V> tv = (TimestampedValue<V>) wrapper.remove(key);
        return tv != null ? tv.getValue() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends K,? extends V> m) {
        flush();
        long now = now();
        m.entrySet().forEach(entry -> wrapper.put(entry.getKey(), new TimestampedValue<V>(entry.getValue(), now)));
    }

    @Override
    public void clear() {
        wrapper.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        flush();
        return wrapper.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> values() {
        flush();
        return (Collection<V>) wrapper.values()
                                      .stream()
                                      .map(tv -> ((TimestampedValue<V>)tv).getValue())
                                      .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Entry<K,V>> entrySet() {
        flush();
        Map<K,V> map = new HashMap<>();
        wrapper.entrySet().forEach(entry ->
            map.put(((Entry<K,TimestampedValue<V>>)entry).getKey(), ((Entry<K,TimestampedValue<V>>)entry).getValue().getValue()));
        return map.entrySet();
    }

    @SuppressWarnings("unchecked")
    private void flush() {
        long now = now();
        Set<Entry<K,TimestampedValue<V>>> entries = Set.copyOf(wrapper.entrySet());
        for (Entry<K,TimestampedValue<V>> entry : entries) {
            if (now - entry.getValue().getTimestamp() > lifetime) {
                wrapper.remove(entry.getKey());
            }
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
