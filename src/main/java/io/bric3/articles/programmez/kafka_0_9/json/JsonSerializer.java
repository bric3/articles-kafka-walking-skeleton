package io.bric3.articles.programmez.kafka_0_9.json;

import java.io.UncheckedIOException;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonSerializer<T> implements Serializer<T> {

    private ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD,
                                                                         Visibility.NON_PRIVATE)
                                                          .registerModules(new Jdk8Module(),
                                                                           new JavaTimeModule());

    @Override
    public void configure(Map<String, ?> nothingToConfigure, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
    }
}
