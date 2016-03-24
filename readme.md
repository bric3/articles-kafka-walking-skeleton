
# Kafka, a walking skeleton





# Starting Kafka

Download Kafka 0.9.0.1 and decompress it.

Mirrors available there

```bash
wget http://wwwftp.ciril.fr/pub/apache/kafka/0.9.0.1/kafka_2.11-0.9.0.1.tgz
tar zxf kafka_2.11-0.9.0.1.tgz
```

Now we'll run the command from the kafka directory :

```bash
cd kafka_2.11-0.9.0.1
```

Zookeeper is a companion server that must run **before** Kafka is started, Kafka will refuse to
run if the Zookeeper ensemble is not available. So in a separate terminal :


```bash
./bin/zookeeper-server-start.sh config/zookeeper.properties
```

Notice that the default zookeeper configuration is enough because this config runs only a single instance
Zookeeper ensemble.

```
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# the directory where the snapshot is stored.
dataDir=/tmp/zookeeper
# the port at which the clients will connect
clientPort=2181
# disable the per-ip limit on the number of connections since this is a non-production config
maxClientCnxns=0
```

One can check if the zookeeper is alive and operational, by sending a [_4 letter word_](https://zookeeper.apache.org/doc/trunk/zookeeperAdmin.html#The+Four+Letter+Words)
on the Zookeeper client port `2181`.

```bash
{ echo stat; sleep 0.1 } | telnet 127.0.0.1 2181
```

Without the lines printed by telnet, here's the output of zookeeper `stat`. If the output does not
print statistics, this instance or the Zookeeper ensemble is not available, thus this need to be fixed in order
for Kafka to run.

```
Zookeeper version: 3.4.6-1569965, built on 02/20/2014 09:09 GMT
Clients:
 /127.0.0.1:52946[0](queued=0,recved=1,sent=0)

Latency min/avg/max: 0/0/0Now start a single Kafka broker instance, once again at this time there's no
Received: 1
Sent: 0
Connections: 1
Outstanding: 0
Zxid: 0x0
Mode: standalone
Node count: 4
```


Now start a single Kafka broker instance, however this time around it is necessary to configure the
configuration file. Let's tweak the default configuration file `config/server.properties`. The main change are below
and are about identifying the Kafka broker instance as number `1`.

```
broker.id=1
logs.dirs=/tmp/kafka-logs-1
zookeeper.connect=localhost:2181
```

When done just start the server in a separate terminal :

```bash
./bin/kafka-server-start.sh ./config/server.properties
```

When you see this line in the console, Kafka is up and running

```bash
[2016-03-20 19:08:52,770] INFO [Kafka Server 1], started (kafka.server.KafkaServer)
```

# Using Kafka

The first thing is to create a topic

```bash
./bin/kafka-topics.sh --create                                 \
                      --topic bier-bar                         \
                      --partition 3                            \
                      --replication-factor 1                   \
                      --zookeeper localhost:2181
```

The topic metadata are created on Zookeeper, that's why it is necessary to pass the zookeeper connection string.
Notice that the number of _partitions_ and the _replication factor_ are mandatory parameters.

Now let's run the java code.

The producer is `io.bric3.articles.programmez.kafka_0_9.Barman` will produce records.
The consumer is `io.bric3.articles.programmez.kafka_0_9.Barfly` will consume records.

Just run both of them.

You should see the consumer printing beers.

# Regarding the parallelism

Brokers and producers will write in any number of partitions. However there's a direct correlation between the number
of _partitions_ on a topic and on the number of consumers.
Each partitions is consumed by exactly one consumer within a group, i.e. there cannot be more consumers in a group
than partitions.

So let's run a group of 3 barflies `io.bric3.articles.programmez.kafka_0_9.Barflies`. Notice how the consumers are
balanced on each partitions (first number is partition, second is the offset, third is the consumer id):

```
2:399:e8bfbf07-d740-458c-8ec8-515e81b115e6 -> Bier bought at '21:12:36.037'
1:1979:398e6f7f-b6a5-46f4-9f20-2aa027ffa5bd -> Bier bought at '21:12:37.041'
0:1978:6d5ac918-e2cb-47e6-bf81-b1aa9c9494fc -> Bier bought at '21:12:38.046'
2:400:e8bfbf07-d740-458c-8ec8-515e81b115e6 -> Bier bought at '21:12:39.047'
1:1980:398e6f7f-b6a5-46f4-9f20-2aa027ffa5bd -> Bier bought at '21:12:40.052'
```

If we run our other consumer `io.bric3.articles.programmez.kafka_0_9.Barfly` (that has the same group id), we notice
that the Barflies _stopped_ one of his consumer. I.e. the other consumer was assigned a partition that was read by
another consumer. If the `Barfly` consumer stops, then it is assigned back to a Consumer that is available.


# Command line with `kafkacat`

On the command line, Kafka distribution, comes with command line shell scripts, however they rely on launching
the JVM, while there's nothing wrong with it (considering the scope of these tools).
It would be more interesting to use a tool written with another language.

That would be [`kafkacat`](https://github.com/edenhill/kafkacat), it is a command line producer or consumer
written in C that uses the [`librdkafka`](https://github.com/edenhill/librdkafka).

For example using `kafkacat` as a consumer would be as simple as that.

Note however that the message that can be consumed, are simple strings.

```bash
kafkacat -b localhost:9092 -t bier-bar -f '%p:%o -> %s\n' -C
```


# Re-balancing a server

Add the new Kafka server, first copy the actual `server.properties`, to another file say `server2.properties`, keep
in mind the broker 1 is still running.

```bash
cp config/server.properties config/server2.properties
```

We'll give this new server the id `2`, and modify some properties such as the port and the logs.

```
broker.id=2
listeners=PLAINTEXT://:9093
logs.dirs=/tmp/kafka-logs-2
zookeeper.connect=localhost:2181
```

Then start the broker :

```bash
./bin/kafka-server-start.sh ./config/server.properties
```

Create a list of topics you want to move.

```json
{
  "topics": [
    {
      "topic": "bier-bar"
    }
  ],
  "version": 1
}
```

_Don't list consumer offset topics, those are technical topics._


Then pass this json file to the `kafka-reassign-partitions.sh`

```bash
./bin/kafka-reassign-partitions.sh --zookeeper localhost:2181                              \
                                   --topics-to-move-json-file topics-to-move.json          \
                                   --broker-list 1,2                                       \
                                   --generate
```

That should list the current distribution of partitions and replicas on current brokers, then the output should be
followed by a list of suggested locations for partitions on both brokers. The result of this command would output
something like :

**Current partition replica assignment**

```json
{
  "version": 1,
  "partitions": [
    {
      "topic": "bier-bar",
      "partition": 0,
      "replicas": [
        1
      ]
    },
    {
      "topic": "bier-bar",
      "partition": 2,
      "replicas": [
        1
      ]
    },
    {
      "topic": "bier-bar",
      "partition": 1,
      "replicas": [
        1
      ]
    }
  ]
}
```

**Proposed partition reassignment configuration**

```json
{
  "version": 1,
  "partitions": [
    {
      "topic": "bier-bar",
      "partition": 0,
      "replicas": [
        1
      ]
    },
    {
      "topic": "bier-bar",
      "partition": 2,
      "replicas": [
        1
      ]
    },
    {
      "topic": "bier-bar",
      "partition": 1,
      "replicas": [
        2
      ]
    }
  ]
}
```

Notice that some partitions are assigned to broker `2`. Here the tool proposes to assign partition `1` of
topic `bier-bar` to broker `2`.

If relevant or you want to play with reassignment then edit the json. Then save the proposed json document to
a file, say `expand-cluster-reassignment.json`.

```bash
./bin/kafka-reassign-partitions.sh --zookeeper localhost:2181                                     \
                                   --reassignment-json-file expand-cluster-reassignment.json      \
                                   --execute
```

Then watch the Kafka servers do the reassignment.

Broker 1

```
[2016-03-24 14:15:44,581] INFO [ReplicaFetcherManager on broker 1] Removed fetcher for partitions [bier-bar,1] (kafka.server.ReplicaFetcherManager)
[2016-03-24 14:15:44,847] INFO Partition [bier-bar,1] on broker 1: Expanding ISR for partition [bier-bar,1] from 1 to 1,2 (kafka.cluster.Partition)
[2016-03-24 14:15:44,876] INFO [ReplicaFetcherManager on broker 1] Removed fetcher for partitions [bier-bar,1] (kafka.server.ReplicaFetcherManager)
[2016-03-24 14:15:44,878] INFO No more partitions need to be reassigned. Deleting zk path /admin/reassign_partitions (kafka.utils.ZkUtils)
[2016-03-24 14:15:44,881] INFO [ReplicaFetcherManager on broker 1] Removed fetcher for partitions [bier-bar,1] (kafka.server.ReplicaFetcherManager)
[2016-03-24 14:15:44,904] INFO Deleting index /tmp/kafka-logs-1/bier-bar-1/00000000000000000000.index (kafka.log.OffsetIndex)
[2016-03-24 14:15:44,905] INFO Deleted log for partition [bier-bar,1] in /tmp/kafka-logs-1/bier-bar-1. (kafka.log.LogManager)
```

Broker 2

```
[2016-03-24 14:15:44,705] INFO Completed load of log bier-bar-1 with log end offset 0 (kafka.log.Log)
[2016-03-24 14:15:44,727] INFO Created log for partition [bier-bar,1] in /tmp/kafka-logs-2 with properties {compression.type -> producer, file.delete.delay.ms -> 60000, max.message.bytes -> 1000012, min.insync.replicas -> 1, segment.jitter.ms -> 0, preallocate -> false, min.cleanable.dirty.ratio -> 0.5, index.interval.bytes -> 4096, unclean.leader.election.enable -> true, retention.bytes -> -1, delete.retention.ms -> 86400000, cleanup.policy -> delete, flush.ms -> 9223372036854775807, segment.ms -> 604800000, segment.bytes -> 1073741824, retention.ms -> 604800000, segment.index.bytes -> 10485760, flush.messages -> 9223372036854775807}. (kafka.log.LogManager)
[2016-03-24 14:15:44,728] INFO Partition [bier-bar,1] on broker 2: No checkpointed highwatermark is found for partition [bier-bar,1] (kafka.cluster.Partition)
[2016-03-24 14:15:44,735] INFO [ReplicaFetcherManager on broker 2] Removed fetcher for partitions [bier-bar,1] (kafka.server.ReplicaFetcherManager)
[2016-03-24 14:15:44,739] INFO Truncating log bier-bar-1 to offset 0. (kafka.log.Log)
[2016-03-24 14:15:44,781] INFO [ReplicaFetcherThread-0-1], Starting  (kafka.server.ReplicaFetcherThread)
[2016-03-24 14:15:44,788] INFO [ReplicaFetcherManager on broker 2] Added fetcher for partitions List([[bier-bar,1], initOffset 0 to broker BrokerEndPoint(1,10.1.101.177,9092)] ) (kafka.server.ReplicaFetcherManager)
[2016-03-24 14:15:44,863] INFO [ReplicaFetcherManager on broker 2] Removed fetcher for partitions [bier-bar,1] (kafka.server.ReplicaFetcherManager)
[2016-03-24 14:15:44,871] INFO [ReplicaFetcherThread-0-1], Shutting down (kafka.server.ReplicaFetcherThread)
[2016-03-24 14:15:45,353] INFO [ReplicaFetcherThread-0-1], Shutdown completed (kafka.server.ReplicaFetcherThread)
[2016-03-24 14:15:45,353] INFO [ReplicaFetcherThread-0-1], Stopped  (kafka.server.ReplicaFetcherThread)
[2016-03-24 14:15:45,358] INFO [ReplicaFetcherManager on broker 2] Removed fetcher for partitions [bier-bar,1] (kafka.server.ReplicaFetcherManager)
```

In order to verify the partition reassignment let's check several commands. First with the `verify` :

```bash
./bin/kafka-reassign-partitions.sh --zookeeper localhost:2181                                     \
                                   --reassignment-json-file expand-cluster-reassignment.json      \
                                   --verify
```

The output should be :

```
Status of partition reassignment:
Reassignment of partition [bier-bar,0] completed successfully
Reassignment of partition [bier-bar,2] completed successfully
Reassignment of partition [bier-bar,1] completed successfully
```

But the output don't show the brokers, `kafka-topics.sh --describe` :

```bash
./bin/kafka-topics.sh --zookeeper localhost:2181           \
                      --topic bier-bar                     \
                      --describe
```



```bash
Topic:bier-bar  PartitionCount:3        ReplicationFactor:1     Configs:
        Topic: bier-bar	Partition: 0    Leader: 1       Replicas: 1     Isr: 1
        Topic: bier-bar	Partition: 1    Leader: 2       Replicas: 2     Isr: 2
        Topic: bier-bar	Partition: 2    Leader: 1       Replicas: 1     Isr: 1
```

Notice that the leader for partition `1` is indeed located on the broker `2`. _As the current topic configuration
does not have multiple replicas the partition is uniquely assigned on broker `2`_


That should be it both producer and consumer should be aware of the new Kafka broker once the partitions
have been reassigned. Remember they have been running the whole time the server 2 has been added to the cluster.

For the `kafkacat` consumer :

```bash
lsof -p `pgrep kafkacat` -P -n
```

For the java apps both the producer and the consumer are now aware of the cluster topology change.

**Producer**

```bash
lsof -p <producer pid> -P -n
```

```
...
java    43437 brice   58u    IPv6 0x934ff95cde1037e3       0t0      TCP 10.1.101.177:61096->10.1.101.177:9093 (ESTABLISHED)
java    43437 brice   59u    unix 0x934ff95cd408a023       0t0          ->(none)
java    43437 brice   60u    IPv6 0x934ff95ce6d61cc3       0t0      TCP 10.1.101.177:58484->10.1.101.177:9092 (ESTABLISHED)
```

**Consumer**

```bash
lsof -p <consumer pid> -P -n
```


```
...
java    43549 brice   71u    IPv6 0x934ff95cdd540cc3       0t0      TCP 10.1.101.177:60201->10.1.101.177:9093 (ESTABLISHED)
java    43549 brice   72u    IPv6 0x934ff95cdd544d43       0t0      TCP 10.1.101.177:60200->10.1.101.177:9093 (ESTABLISHED)
java    43549 brice   73u    IPv6 0x934ff95cdd5452a3       0t0      TCP 10.1.101.177:60202->10.1.101.177:9093 (ESTABLISHED)
java    43549 brice   74u    unix 0x934ff95ce4d194d3       0t0          ->(none)
java    43549 brice   75u    IPv6 0x934ff95cdc3ee223       0t0      TCP 10.1.101.177:60740->10.1.101.177:9092 (ESTABLISHED)
java    43549 brice   76u    IPv6 0x934ff95cdc3ef7a3       0t0      TCP 10.1.101.177:60741->10.1.101.177:9092 (ESTABLISHED)
java    43549 brice   77u    IPv6 0x934ff95cdc3f07c3       0t0      TCP 10.1.101.177:60742->10.1.101.177:9092 (ESTABLISHED)
java    43549 brice   78u    IPv6 0x934ff95cdc3f1283       0t0      TCP 10.1.101.177:58552->10.1.101.177:9092 (ESTABLISHED)
java    43549 brice   79u    IPv6 0x934ff95cdc3f17e3       0t0      TCP 10.1.101.177:58553->10.1.101.177:9092 (ESTABLISHED)
java    43549 brice   80u    IPv6 0x934ff95cdc3edcc3       0t0      TCP 10.1.101.177:58551->10.1.101.177:9092 (ESTABLISHED)
```

Notice on the consumer there's several connections 1 connection per partition per consumer thread :

* `3 consumers x 2 partitions (0 and 2) = 6` to broker `1` (listening on port `9092`)
* `3 consumers x 1 partion (1) = 3` to broker `2` (listening on port `9093`)


