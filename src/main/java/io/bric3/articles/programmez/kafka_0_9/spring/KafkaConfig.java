package io.bric3.articles.programmez.kafka_0_9.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {
     // Infrastructure @Beans omitted. 
     // See Reference Manual and tests for comprehensive sample

     @Bean
     public Listener listener() {
        return new Listener();
     }

}
