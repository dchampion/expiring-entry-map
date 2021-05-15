package com.dchampion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExpiringEntryMapTest {

    List<Map<Integer,String>> maps = new ArrayList<>();

    @Before
    public void setup() {
        createMaps(TimeUnit.SECONDS, 10);
    }

    void createMaps(TimeUnit unit, long lifetime) {
        // HashMap
        maps.add(ExpiringEntryMap.Builder.map(new HashMap<Integer,String>()).lifetime(unit, lifetime).build());
        // TreeMap
        maps.add(ExpiringEntryMap.Builder.map(new TreeMap<Integer,String>()).lifetime(unit, lifetime).build());
        // Hashtable
        maps.add(ExpiringEntryMap.Builder.map(new Hashtable<Integer,String>()).lifetime(unit, lifetime).build());
        // LinkedHashMap
        maps.add(ExpiringEntryMap.Builder.map(new LinkedHashMap<Integer,String>()).lifetime(unit, lifetime).build());
    }

    @After
    public void tearDown() {
        clearMaps();
    }

    void clearMaps() {
        maps.clear();
    }

    // Begin Builder functionality tests.
    @Test
    public void testBuildMap() {
        Map<String,Integer> map = ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).build();
        map.put("one", 1);
        assertEquals(map.get("one"), Integer.valueOf(1));

        map = ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).lifetime(TimeUnit.HOURS, 2).build();
        map.put("one", 1);
        assertEquals(map.get("one"), Integer.valueOf(1));
    }

    @Test
    public void testBuildMapWithExistingEntries() {
        Map<Integer,String> map = new TreeMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        map = ExpiringEntryMap.Builder.map(map).build();
        assertEquals(map.get(1), "one");
        assertEquals(map.get(2), "two");
        assertEquals(map.get(3), "three");
    }

    @Test
    public void testTimeUnitConversions() {
        ExpiringEntryMap<String,Integer> map = 
            ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).build();
        assertEquals(map.lifetime, 60 * 60 * 1000);

        map = ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).lifetime(TimeUnit.DAYS, 1).build();
        assertEquals(map.lifetime, 24 * 60 * 60 * 1000);

        map = ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).lifetime(TimeUnit.HOURS, 1).build();
        assertEquals(map.lifetime, 60 * 60 * 1000);

        map = ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).lifetime(TimeUnit.MINUTES, 1).build();
        assertEquals(map.lifetime, 60 * 1000);

        map = ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).lifetime(TimeUnit.SECONDS, 1).build();
        assertEquals(map.lifetime, 1000);

        map = ExpiringEntryMap.Builder.map(new HashMap<String,Integer>()).lifetime(TimeUnit.MILLISECONDS, 1).build();
        assertEquals(map.lifetime, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMap() {
        ExpiringEntryMap.Builder.map(null);
    }

    @Test
    public void testLifetimeExceptions() {
        try {
            ExpiringEntryMap.Builder.map(new HashMap<String,String>()).lifetime(TimeUnit.MICROSECONDS, 1);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            ExpiringEntryMap.Builder.map(new HashMap<String,String>()).lifetime(TimeUnit.NANOSECONDS, 1);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            ExpiringEntryMap.Builder.map(new HashMap<String,String>()).lifetime(TimeUnit.MILLISECONDS, -1);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
    // End Builder functionality tests.

    // Begin standard map functionality tests.
    void populate(Map<Integer,String> map) {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
    }

    @Test
    public void testClear() {
        maps.forEach(map -> {
            populate(map);
            map.clear();
            assertTrue(map.isEmpty());
        });
    }

    @Test
    public void testCompute() {
        maps.forEach(map -> {
            populate(map);
            assertEquals(map.compute(3, (k, v) -> v.concat("blindmice")), "threeblindmice");
            assertEquals(map.get(3), "threeblindmice");
        });
    }

    @Test
    public void testComputeIfAbsent() {
        maps.forEach(map -> {
            populate(map);
            assertEquals(map.computeIfAbsent(3, k -> "three"), "three");
            assertEquals(map.get(3), "three");
            assertNull(map.get(5));
            assertEquals(map.computeIfAbsent(5, k -> "five"), "five");
            assertEquals(map.get(5), "five");
        });
    }

    @Test
    public void testComputeIfPresent() {
        maps.forEach(map -> {
            populate(map);
            assertEquals(map.computeIfPresent(3, (k, v) -> "THREE"), "THREE");
            assertEquals(map.get(3), "THREE");
            assertNull(map.get(5));
            assertNull(map.computeIfPresent(5, (k, v) -> "five"));
            assertNull(map.get(5));
        });
    }

    @Test
    public void testContainsKey() {
        maps.forEach(map -> {
            populate(map);
            assertTrue(map.containsKey(4));
            assertFalse(map.containsKey(5));
        });
    }

    @Test
    public void testContainsValue() {
        maps.forEach(map -> {
            populate(map);
            assertTrue(map.containsValue("two"));
            assertFalse(map.containsValue("five"));
        });
    }

    @Test
    public void testEntrySet() {
        maps.forEach(map -> {
            populate(map);
            Set<Entry<Integer,String>> entrySet = map.entrySet();
            assertEquals(entrySet.size(), 4);
            entrySet.forEach(entry -> {
                assertTrue(entry.getKey() instanceof Integer);
                assertTrue(entry.getValue() instanceof String);
            });
        });
    }

    @Test
    public void testGet() {
        maps.forEach(map -> {
            populate(map);
            assertEquals("one", map.get(1));
            assertEquals("two", map.get(2));
            assertEquals("three", map.get(3));
            assertEquals("four", map.get(4));
            assertEquals(null, map.get(5));
        });
    }

    @Test
    public void testGetOrDefault() {
        maps.forEach(map -> {
            populate(map);
            assertEquals("one", map.getOrDefault(1, "one"));
            assertEquals("two", map.getOrDefault(2, "two"));
            assertEquals("three", map.getOrDefault(3, "three"));
            assertEquals("four", map.getOrDefault(4, "four"));
            assertEquals("five", map.getOrDefault(5, "five"));
        });
    }

    @Test
    public void testIsEmpty() {
        maps.forEach(map -> {
            assertTrue(map.isEmpty());
            populate(map);
            assertFalse(map.isEmpty());
        });
    }

    @Test
    public void testKeySet() {
        maps.forEach(map -> {
            populate(map);
            Set<Integer> keySet = map.keySet();
            assertEquals(4, keySet.size());
            assertTrue(keySet.containsAll(Arrays.asList(1, 2, 3, 4)));
        });
    }

    @Test
    public void testMerge() {
        maps.forEach(map -> {
            populate(map);
            assertEquals(map.merge(1, "ofakind", String::concat), "oneofakind");
            assertEquals(map.get(1), "oneofakind");
        });
    }

    @Test
    public void testPut() {
        maps.forEach(map -> {
            assertTrue(map.isEmpty());
            assertNull(map.put(1, "one"));
            assertEquals(map.get(1), "one");
            assertEquals(map.put(1, "ONE"), "one");
            assertEquals(map.get(1), "ONE");
        });
    }

    @Test
    public void testPutAll() {
        maps.forEach(map -> {
            assertTrue(map.isEmpty());
            Map<Integer,String> m = new HashMap<>();
            m.put(1, "one");
            m.put(2, "two");
            map.putAll(m);
            assertEquals(map.get(1), "one");
            assertEquals(map.get(2), "two");
        });
    }

    @Test
    public void testPutIfAbsent() {
        maps.forEach(map -> {
            populate(map);
            assertEquals(map.putIfAbsent(1, "one"), "one");
            assertNull(map.putIfAbsent(5, "five"));
            assertEquals(map.get(5), "five");
        });
    }

    @Test
    public void testRemove() {
        maps.forEach(map -> {
            populate(map);
            assertFalse(map.isEmpty());
            assertEquals(map.remove(1), "one");
            assertEquals(map.remove(2), "two");
            assertEquals(map.remove(3), "three");
            assertEquals(map.remove(4), "four");
            assertNull(map.remove(4));
        });
    }

    @Test
    public void testSize() {
        maps.forEach(map -> {
            populate(map);
            assertEquals(map.size(), 4);
            map.clear();
            assertEquals(map.size(), 0);
        });
    }

    @Test
    public void testSerialize() {
        // TODO: Implement me.
    }
    // End standard map functionality tests.

    // Begin expiring map value functionality tests.
    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testEntriesExpire() {
        clearMaps();
        createMaps(TimeUnit.MILLISECONDS, 100);
        maps.forEach(map -> {
            populate(map);
            assertEquals(map.size(), 4);
            sleep(200);
            assertEquals(map.size(), 0);

            map.put(1, "ONE");
            assertEquals(map.size(), 1);
            sleep(200);
            assertEquals(map.size(), 0);

            map.put(2, "TWO");
            sleep(200);
            assertEquals(map.size(), 0);
        });
    }

    @Test
    public void testPreexistingEntriesExpire() {
        Map<Integer,String> map = new TreeMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        map = ExpiringEntryMap.Builder.map(map).lifetime(TimeUnit.MILLISECONDS, 100).build();
        assertEquals(map.size(), 3);

        sleep(200);
        assertTrue(map.isEmpty());
        assertEquals(map.size(), 0);
    }
    // End expiring map value functionality tests.

    // Begin concurrent map functionality tests.
    @Test
    public void testConcurrentMap() {
        Map<String,Long> map =
            ExpiringEntryMap.Builder.map(new ConcurrentHashMap<String,Long>()).lifetime(TimeUnit.MILLISECONDS, 1000).build();
        ExecutorService es = Executors.newFixedThreadPool(5);

        for(int i=0; i<5; i++) {
            map.put(Thread.currentThread().getName()+i, Thread.currentThread().getId());
            es.execute(() -> {
                for(int j=0; j<5; j++) {
                    map.put(Thread.currentThread().getName()+j, Thread.currentThread().getId());
                }
            });
            map.put(Thread.currentThread().getName()+i, Thread.currentThread().getId());
        }
        es.shutdown();
        try {
            es.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals(30, map.size());

        sleep(1000);
        assertEquals(0, map.size());
    }
    // End concurrent map functionality tests.
}
