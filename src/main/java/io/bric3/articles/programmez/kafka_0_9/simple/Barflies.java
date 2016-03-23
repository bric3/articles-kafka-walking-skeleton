package io.bric3.articles.programmez.kafka_0_9.simple;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

public class Barflies {
    private static int parallelism = 3;

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        ShutdownHook shutdownHook = ShutdownHook.register().registerExecutor(executorService);

        IntStream.range(0, parallelism)
                 .mapToObj(ignored -> new Barfly())
                 .peek(shutdownHook::registerNewConsumer)
                 .forEach(executorService::submit);
    }


    private static class Barfly implements Runnable {
        private UUID uuid;
        private final KafkaConsumer<String, String> consumer;

        public Barfly() {
            uuid = UUID.randomUUID();
            Properties props = new Properties();
            props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(GROUP_ID_CONFIG, "barfly-group");
            props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumer = new KafkaConsumer<>(props);
        }

        @Override
        public void run() {
            System.out.printf("Starts consumer %s%n", uuid);
            consumer.subscribe(Collections.singletonList("bier-bar"));
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(1000);
                    for (ConsumerRecord<String, String> record : records) {
                        System.out.printf("%d:%s:%s -> %s%n", record.partition(), record.offset(), uuid, record.value());
                    }
                }
            } catch(WakeupException ignored) {
            } finally {
                System.out.println(String.format("Barfly '%s' shutting down ...", uuid));
                consumer.close(); // always close the consumer
            }
        }

        public void shutdown() {
            consumer.wakeup();
        }
    }

    private static class ShutdownHook extends Thread{
        private List<Barfly> toShutdown = Collections.synchronizedList(new ArrayList<>());
        private ExecutorService executor;

        @Override
        public void run() {
            toShutdown.forEach(Barfly::shutdown);
            executor.shutdown();
            try {
                executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        static ShutdownHook register() {
            ShutdownHook shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            return shutdownHook;
        }

        ShutdownHook registerNewConsumer(Barfly consumer) {
            this.toShutdown.add(consumer);
            return this;
        }

        ShutdownHook registerExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }
    }
}
