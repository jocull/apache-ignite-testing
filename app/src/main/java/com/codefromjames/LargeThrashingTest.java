package com.codefromjames;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class LargeThrashingTest implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LargeThrashingTest.class);
    private static final Random RANDOM = new Random();

    private final IgniteClient client;

    public LargeThrashingTest(IgniteClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        final ClientCache<String, byte[]> cache = client.cache("myCache"); // Should already be created per config!
        final MutableInt counter = new MutableInt(0);
        while (true) {
            try {
                final Instant now = Instant.now();
                final LargeObject largeObject = new LargeObject();
                Optional<String> existingHash = Optional.ofNullable(cache.get(largeObject.key))
                                .map(DigestUtils::sha1Hex);
                cache.put(largeObject.key, largeObject.blob);
                LOGGER.info("Wrote #{}, {} w/ hash {}", counter.getAndIncrement(), largeObject.key, largeObject.hash);
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

        LargeObject() {
            this.key = UUID.randomUUID().toString();
            RANDOM.nextBytes(blob);
            this.hash = DigestUtils.sha1Hex(blob);
        }
    }
}
