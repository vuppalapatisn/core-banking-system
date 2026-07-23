package com.amol.microservices.events.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A single partition: an append-only, offset-addressed log with size-based retention. Offsets are
 * absolute and monotonic; when retention trims the oldest records, {@code baseOffset} advances so
 * offsets never repeat.
 */
public class Partition {

    private final String topic;
    private final int id;
    private final int retention;
    private final List<EventRecord> records = new ArrayList<>();
    private long baseOffset = 0;

    public Partition(String topic, int id, int retention) {
        this.topic = topic;
        this.id = id;
        this.retention = retention;
    }

    public synchronized EventRecord append(String key, Object payload, long timestampMs) {
        long offset = baseOffset + records.size();
        EventRecord record = new EventRecord(topic, id, offset, key, payload, timestampMs);
        records.add(record);
        if (records.size() > retention) {
            records.remove(0);
            baseOffset++;
        }
        return record;
    }

    /** Records with offset &gt;= {@code fromOffset}, up to {@code max}. Offsets below the retained base start at the base. */
    public synchronized List<EventRecord> read(long fromOffset, int max) {
        int start = (int) Math.max(0, fromOffset - baseOffset);
        List<EventRecord> out = new ArrayList<>();
        for (int i = start; i < records.size() && out.size() < max; i++) {
            out.add(records.get(i));
        }
        return out;
    }

    /** The offset that will be assigned to the next appended record. */
    public synchronized long highWatermark() {
        return baseOffset + records.size();
    }

    public synchronized long baseOffset() {
        return baseOffset;
    }

    public synchronized int size() {
        return records.size();
    }

    public int id() {
        return id;
    }
}
