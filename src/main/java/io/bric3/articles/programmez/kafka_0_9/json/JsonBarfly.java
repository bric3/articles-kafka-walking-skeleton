package io.bric3.articles.programmez.kafka_0_9.json;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static io.bric3.articles.programmez.kafka_0_9.json.JsonDeserializer.JSON_DESERIALIZER_TYPE;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonBarfly {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(GROUP_ID_CONFIG, "barfly-group");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(JSON_DESERIALIZER_TYPE, GotABier.class);
        KafkaConsumer<String, GotABier> consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Collections.singletonList("bier-bar"));
        try {
            for(int maxPoll = 0; maxPoll<100; maxPoll++) {
                ConsumerRecords<String, GotABier> records = consumer.poll(1000);
                for (ConsumerRecord<String, GotABier> record : records) {
                    System.out.printf("%d:%d %s%n", record.partition(), record.offset(), record.value());
                }
            }
        } finally {
            System.out.println("Barfly shutting down ...");
            consumer.close();
        }
    }

    private static class GotABier {
        final String message;
        final String bierName;
        final LocalTime at;

        @JsonCreator
        public GotABier(@JsonProperty("message") String message,
                        @JsonProperty("bierName") String bierName,
                        @JsonProperty("timestamp") LocalTime at) {
            this.message = message;
            this.bierName = bierName;
            this.at = at;
        }

        @Override
        public String toString() {
            return String.format("%s, a '%s' at %s", message, bierName, at);
        }
    }
}
