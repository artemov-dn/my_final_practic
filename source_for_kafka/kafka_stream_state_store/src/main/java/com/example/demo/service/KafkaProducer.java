package com.example.demo.service;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class KafkaProducer {
    public static final Logger LOG = LoggerFactory.getLogger(KafkaStream.class);

    @Value("${blocked-products}")
    String blockedProductsTopic;

    @Value("${bootstrap-servers}")
    String servers;

    @Value("${kafka-username}")
    String kafkaUsername;

    @Value("${kafka-password}")
    String kafkaPassword;

    @Value("${truststore-location}")
    String truststoreLocation;

    @Value("${truststore-password}")
    String truststorePassword;

    org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;


    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        producerProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        producerProps.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + kafkaUsername + "\" password=\"" + kafkaPassword + "\";");
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation); // Truststore
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword); // Truststore password

        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(producerProps);
    }

    public void sendMessages(String key, String value) {
        if (producer != null) {

            try {
                LOG.info("Отправка данных о заблокированном товаре ...");

                // Отправляем данные (ID товара, "+" заблокировать товар / "-" разблокировать товар)
                producer.send(new ProducerRecord<>(blockedProductsTopic, key, value));
                producer.flush();
                LOG.info("Данные о блокировках успешно отправлены");


            } catch (Exception e) {
                LOG.error("Ошибка при отправке данных: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
