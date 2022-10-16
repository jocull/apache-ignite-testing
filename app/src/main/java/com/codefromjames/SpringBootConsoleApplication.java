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
            // TODO: A great place for multi-threaded load and consistency testing
            new LargeThrashingTest(client).run();
        }
    }
}
