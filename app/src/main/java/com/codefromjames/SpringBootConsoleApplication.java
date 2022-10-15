package com.codefromjames;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@SpringBootApplication
public class SpringBootConsoleApplication implements CommandLineRunner {
    private static Logger LOGGER = LoggerFactory.getLogger(SpringBootConsoleApplication.class);

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
                .setAddresses("localhost:10800", "localhost:10801", "localhost:10802")
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
                    LOGGER.info("Previous {}, {}: {}", a, b, cache.getAll(Set.of(a, b)).values());
                    cache.put(a, "hello " + now);
                    cache.put(b, "world " + now);
                    LOGGER.info("New {}, {}: {}", a, b, cache.getAll(Set.of(a, b)).values());
                } catch (ClientException ex) {
                    LOGGER.error("DB failed", ex);
                }
                Thread.sleep(250);
            }
        }
    }
}
