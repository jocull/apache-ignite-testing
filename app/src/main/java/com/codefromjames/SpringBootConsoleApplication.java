package com.codefromjames;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                .setAddresses(
                        // Cluster A
                        "localhost:10800", "localhost:10801", "localhost:10802",
                        // Cluster B
                        "localhost:10803", "localhost:10804", "localhost:10805"
                )
                .setPartitionAwarenessEnabled(true)
                .setReconnectThrottlingPeriod(5_000);

        try (final IgniteClient client = Ignition.startClient(cfg)) {
            IntStream.range(0, 8)
                    .mapToObj(i -> {
                        Thread t = new Thread(new LargeThrashingTest(client, 50, 1024 * 10 * (i + 1)));
                        t.setName("ignite-pusher-" + i);
                        t.start();
                        return t;
                    })
                    .collect(Collectors.toList())
                    .forEach(t -> {
                        try {
                            t.join();
                        } catch (InterruptedException ex) {
                            LOGGER.info("Interrupted", ex);
                        }
                    });
        }
    }
}
