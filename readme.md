
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
port=9092
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



