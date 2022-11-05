package ru.romanow.kafka.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.slf4j.LoggerFactory.getLogger;

@SpringBootApplication
public class KafkaProducerApplication {
    private static final Logger logger = getLogger(KafkaProducerApplication.class);

    private static final String TOPIC_NAME = "my-topic";
    private static final long SEND_TIMEOUT = 5000;
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static long counter = 0;

    public static void main(String[] args) {
        SpringApplication.run(KafkaProducerApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(KafkaTemplate<String, String> kafkaTemplate) {
        return args -> {
            int length = 12;
            while (true) {
                final var z = (int) counter / 26;
                final var idx = (int) counter % 26;
                final var a = length - (z + 1);

                final String data = "A".repeat(a) + LETTERS.charAt(idx) + "Z".repeat(z);
                counter++;

                kafkaTemplate.send(TOPIC_NAME, UUID.randomUUID().toString(), data);
                logger.info("Send data to topic {}: '{}'", TOPIC_NAME, data);
                Thread.sleep(SEND_TIMEOUT);
            }
        };
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder
                .name(TOPIC_NAME)
                .partitions(2)
                .replicas(1)
                .build();
    }
}
