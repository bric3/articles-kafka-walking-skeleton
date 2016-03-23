package io.bric3.articles.programmez.kafka_0_9.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonDeserializer<T> implements Deserializer<T> {

    public static final String JSON_DESERIALIZER_TYPE = "json.deserializer.type";

    private ObjectMapper objectMapper = new ObjectMapper().registerModules(new Jdk8Module(),
                                                                           new JavaTimeModule());
    private Class<T> type;

    @Override
    public void configure(Map<String, ?> config, boolean isKey) {
        type = (Class<T>) config.get(JSON_DESERIALIZER_TYPE);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, type);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
    }
}
