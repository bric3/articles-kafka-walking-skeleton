package io.bric3.articles.programmez.kafka_0_9.json;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class JsonBarman {

    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());

        KafkaProducer<String, BierServed> producer = new KafkaProducer<>(props);
        ShutdownHook.register().registerNewProducer(producer);

        System.out.println("Barman giving bier");
        while (true) {
            producer.send(new ProducerRecord<>("bier-bar",
                                               BierServed.of("Bier served", "Gallia", LocalTime.now())));
            SECONDS.sleep(1);
        }
    }

    private static class BierServed {
        final String message;
        final String bierName;
        final LocalTime timestamp;

        BierServed(String message, String bierName, LocalTime timestamp) {
            this.message = message;
            this.bierName = bierName;
            this.timestamp = timestamp;
        }

        public static BierServed of(String message, String bierName, LocalTime timestamp) {
            return new BierServed(message, bierName, timestamp);
        }
    }

    static private class ShutdownHook extends Thread {

        private List<Producer> toShutdown = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void run() {
            System.out.println("Barman shutting down ...");
            toShutdown.forEach(producer -> producer.close(1000, MILLISECONDS));
        }

        static ShutdownHook register() {
            ShutdownHook shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            return shutdownHook;
        }
        ShutdownHook registerNewProducer(Producer<?, ?> producer) {
            this.toShutdown.add(producer);
            return this;
        }
    }
}
