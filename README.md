What is this?
================

I'm setting up production-like Apache Ignite clusters with Docker. From here I'll attempt test important things like:

- Native persistence
- Replication
- Partitioning
- Instance failures
- Net splits
- Entire availability zone failures

How?
=======

Three main components:

- Docker compose `cluster-a`
- Docker compose `cluster-b`
- A simple Spring Boot console application that injects data and checks that data stays consistent

Generally, run the app and have it create load and check future read results against its own accounting to find inconsistencies.
While this is happening, create various failures, add instances, remove instances, and so on as you would in production upgrades, zone evacuations, and more.
