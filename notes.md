If a partition loses data because owners all died you must acknowledge loss by resetting the cache state:

```bash
docker exec -it cluster-a-ignite01-1 /opt/ignite/apache-ignite/bin/control.sh --cache reset_lost_partitions myCache
```

If you have a cache group, you need to reset by the group:

```bash
docker exec -it cluster-a-ignite01-1 /opt/ignite/apache-ignite/bin/control.sh --cache reset_lost_partitions debugGroup
```

Partition loss matters:
- `READ_ONLY_SAFE` no one can write anything to any partition (this is a pedantic level of safety)
- `READ_WRITE_SAFE` you can write but only to available partitions (this option seems most appropriate if data is well-partitioned)
- `IGNORE` would allow writes regardless, but can only be used with in-memory mode (no disk persistence)


Checking baseline nodes:

```
docker exec -it cluster-a-ignite01-1 /opt/ignite/apache-ignite/bin/control.sh --baseline
```

Checking distribution of partitions and statuses:

```
docker exec -it cluster-a-ignite01-1 /opt/ignite/apache-ignite/bin/control.sh --cache distribution null debugGroup
```
