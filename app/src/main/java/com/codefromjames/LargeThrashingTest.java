package com.codefromjames;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LargeThrashingTest implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LargeThrashingTest.class);
    private static final Random RANDOM = new Random();

    private final IgniteClient client;
    private final List<String> keySet;
    private final Map<String, String> ackedKeyHashes = new HashMap<>();

    public LargeThrashingTest(IgniteClient client) {
        this.client = client;
        this.keySet = IntStream.range(0, 1024)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
    }

    @Override
    public void run() {
        final ClientCache<String, byte[]> cache = client.cache("myCache"); // Should already be created per config!
        final MutableInt counter = new MutableInt(0);
        final Deque<String> keyRotation = new ArrayDeque<>(keySet);
        while (true) {
            if (keyRotation.isEmpty()) {
                keyRotation.addAll(keySet);
            }
            final LargeObject largeObject = new LargeObject(keyRotation.pop());
            try {
                Optional.ofNullable(cache.get(largeObject.key))
                                .map(DigestUtils::sha1Hex)
                                        .ifPresent(currentHash -> {
                                            final String ackedHash = ackedKeyHashes.get(largeObject.key);
                                            if (ackedHash != null
                                                    && !ackedHash.equals(currentHash)) {
                                                LOGGER.warn("Current key {} hash {} does not match {}", largeObject.key, currentHash, ackedHash);
                                            }
                                        });

                cache.put(largeObject.key, largeObject.blob);
                LOGGER.info("Wrote #{}, {} w/ hash {}", counter.getAndIncrement(), largeObject.key, largeObject.hash);
                ackedKeyHashes.put(largeObject.key, largeObject.hash);
            } catch (ClientException ex) {
                LOGGER.error("DB failed", ex);
            }
//            try {
//                Thread.sleep(250);
//            } catch (InterruptedException ex) {
//                LOGGER.info("Interrupted", ex);
//                return;
//            }
        }
    }

    private static class LargeObject {
        final String key;
        final byte[] blob = new byte[1024 * 1024]; // 1 MB
        final String hash;

        LargeObject(String key) {
            this.key = key;
            RANDOM.nextBytes(blob);
            this.hash = DigestUtils.sha1Hex(blob);
        }
    }
}
