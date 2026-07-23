package com.amol.microservices.events.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A topic: a fixed set of partitions. Keyed records hash to a stable partition (ordering per key);
 * unkeyed records are spread round-robin for throughput.
 */
public class Topic {

    private final String name;
    private final List<Partition> partitions = new ArrayList<>();
    private final AtomicInteger roundRobin = new AtomicInteger(0);

    public Topic(String name, int partitionCount, int retention) {
        this.name = name;
        for (int i = 0; i < partitionCount; i++) {
            partitions.add(new Partition(name, i, retention));
        }
    }

    public String name() {
        return name;
    }

    public int partitionCount() {
        return partitions.size();
    }

    public Partition partition(int id) {
        return partitions.get(id);
    }

    public List<Partition> partitions() {
        return partitions;
    }

    public int partitionFor(String key) {
        if (key == null || key.isEmpty()) {
            return Math.floorMod(roundRobin.getAndIncrement(), partitions.size());
        }
        return Math.floorMod(key.hashCode(), partitions.size());
    }

    public EventRecord publish(String key, Object payload, long timestampMs) {
        return partitions.get(partitionFor(key)).append(key, payload, timestampMs);
    }

    /** Total records currently retained across all partitions. */
    public long retainedCount() {
        return partitions.stream().mapToLong(Partition::size).sum();
    }
}
