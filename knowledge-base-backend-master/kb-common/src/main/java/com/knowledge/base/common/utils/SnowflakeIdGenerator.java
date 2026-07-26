package com.knowledge.base.common.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Snowflake algorithm ID generator
 *
 * <p>Uses Twitter's Snowflake algorithm to generate distributed IDs, guaranteeing global uniqueness</p>
 * <p>Generates a 19-digit Long-type ID</p>
 *
 * <p>ID structure: 1 sign bit + 41-bit timestamp + 10-bit machine ID + 12-bit sequence number</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {

    private static final long START_TIMESTAMP = 1704067200000L;

    private static final long DATACENTER_ID_BITS = 5L;

    private static final long WORKER_ID_BITS = 5L;

    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;

    private final long workerId;

    private long sequence = 0L;

    private long lastTimestamp = -1L;

    private static volatile SnowflakeIdGenerator instance;

    @PostConstruct
    public void init() {
        log.info("SnowflakeIdGenerator initialized with datacenterId: {}, workerId: {}", datacenterId, workerId);
    }

    public SnowflakeIdGenerator() {
        this(1L, 1L);
    }

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("Datacenter ID can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("Worker ID can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public static SnowflakeIdGenerator getInstance(long datacenterId, long workerId) {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    instance = new SnowflakeIdGenerator(datacenterId, workerId);
                }
            }
        }
        return instance;
    }

    public static SnowflakeIdGenerator getInstance() {
        return getInstance(1L, 1L);
    }

    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();

        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    Thread.sleep(offset << 1);
                    timestamp = getCurrentTimestamp();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException("Clock moved backwards. Refusing to generate id");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Clock moved backwards. Refusing to generate id");
                }
            } else {
                throw new RuntimeException("Clock moved backwards. Refusing to generate id");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
            | (datacenterId << DATACENTER_ID_SHIFT)
            | (workerId << WORKER_ID_SHIFT)
            | sequence;
    }

    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}