package io.bric3.articles.programmez.kafka_0_9.spring;

public class Listener {

     @KafkaListener(topics = "myTopic")
     public void handleFromKakfa(String payload) {
       ...
    }

}
