package com.codefromjames;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
    public void run(String... args) {
        LOGGER.info("EXECUTING : command line runner");

        for (int i = 0; i < args.length; ++i) {
            LOGGER.info("args[{}]: {}", i, args[i]);
        }

        final ClientConfiguration cfg = new ClientConfiguration()
                .setAddresses("localhost:10800", "localhost:10801", "localhost:10802")
                .setPartitionAwarenessEnabled(true);

        try (final IgniteClient client = Ignition.startClient(cfg)) {
            final ClientCache<Integer, String> cache = client.getOrCreateCache("myCache");
            LOGGER.info("Previous: {}", cache.getAll(Set.of(1, 2)).values());
            cache.put(1, "hello " + UUID.randomUUID());
            cache.put(2, "world " + UUID.randomUUID());
            LOGGER.info("New: {}", cache.getAll(Set.of(1, 2)).values());
        } catch (ClientException ex) {
            LOGGER.error("DB failed", ex);
        }
    }
}
