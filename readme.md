
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
                      --partition 2                            \
                      --replication-factor 1                   \
                      --zookeeper localhost:2181
```

The topic metadata are created on Zookeeper, that's why it is necessary to pass the zookeeper connection string.
Notice that the number of _partitions_ and the _replication factor_ are mandatory parameters.

Now let's run the java code.

The producer is `io.bric3.articles.programmez.kafka_0_9.Barman` will produce records.
The consumer is `io.bric3.articles.programmez.kafka_0_9.Barfly` will consume records.

Just run both of them.

