package com.codefromjames;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class SpringBootConsoleApplication implements CommandLineRunner {
    private static Logger LOGGER = LoggerFactory.getLogger(SpringBootConsoleApplication.class);

    private Map<Integer, String> lastAccepted = new HashMap<>(300);

    public static void main(String[] args) {
        LOGGER.info("STARTING THE APPLICATION");
        SpringApplication.run(SpringBootConsoleApplication.class, args);
        LOGGER.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String... args) throws InterruptedException {
        LOGGER.info("EXECUTING : command line runner");

        for (int i = 0; i < args.length; ++i) {
            LOGGER.info("args[{}]: {}", i, args[i]);
        }

        final ClientConfiguration cfg = new ClientConfiguration()
                .setAddresses(
                        // Cluster A
                        "localhost:10800", "localhost:10801", "localhost:10802",
                        // Cluster B
                        "localhost:10803", "localhost:10804", "localhost:10805"
                )
                .setPartitionAwarenessEnabled(true)
                .setReconnectThrottlingPeriod(5_000);

        try (final IgniteClient client = Ignition.startClient(cfg)) {
            final ClientCache<Integer, String> cache = client.cache("myCache"); // Should already be created per config!
            final MutableInt counter = new MutableInt(0);
            while (true) {
                try {
                    int a = counter.getAndIncrement();
                    int b = counter.getAndIncrement();
                    if (counter.getValue() > 100) {
                        counter.setValue(0);
                    }

                    final Instant now = Instant.now();
                    try (ClientTransaction tx = client.transactions().txStart()) {
                        final String aValPrev = cache.get(a);
                        final String bValPrev = cache.get(b);
                        LOGGER.info("Previous {}, {}: {}, {}", a, b, aValPrev, bValPrev);
                        if (lastAccepted.containsKey(a) && lastAccepted.containsKey(b)) {
                            if (!lastAccepted.get(a).equals(aValPrev)) {
                                LOGGER.error("MISMATCH @ {}: {} vs {}", a, lastAccepted.get(a), aValPrev);
                            }
                            if (!lastAccepted.get(b).equals(bValPrev)) {
                                LOGGER.error("MISMATCH @ {}: {} vs {}", b, lastAccepted.get(b), bValPrev);
                            }
                        }

                        final String aVal = "hello " + now;
                        final String bVal = "world " + now;
                        cache.put(a, aVal);
                        cache.put(b, bVal);
                        tx.commit();
                        lastAccepted.put(a, aVal);
                        lastAccepted.put(b, bVal);
                        LOGGER.info("New {}, {}: {}", a, b, cache.getAll(Set.of(a, b)).values());
                    }
                } catch (ClientException ex) {
                    LOGGER.error("DB failed", ex);
                }
                Thread.sleep(250);
            }
        }
    }
}
