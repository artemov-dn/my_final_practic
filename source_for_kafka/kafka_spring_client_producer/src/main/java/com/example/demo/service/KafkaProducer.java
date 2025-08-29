package com.example.demo.service;

import com.example.demo.domain.Request;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
    public static final Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);

    @Value("${kafka-topic}")
    String topic;

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

    @Value("${schema-registry-url}")
    String schemaRegistryUrl;

    org.apache.kafka.clients.producer.KafkaProducer<String, Request> producer;


    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class.getName());

        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + kafkaUsername + "\" password=\"" + kafkaPassword + "\";");
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation); // Truststore
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword); // Truststore password
        properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, ""); // Отключение проверки hostname
        properties.put("schema.registry.url", schemaRegistryUrl);
        properties.put("auto.register.schemas", true);
        properties.put("use.latest.version", true);

        SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 10);

        // Создание продюсера
        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(properties);
        // Register schema
        String userSchema = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"Request\", \"type\": \"object\", " +
                "\"properties\": { " +
                " \"request_id\": { \"type\": \"string\" }, " +
                " \"client_id\": { \"type\": \"string\" }, " +
                " \"action\": { \"type\": \"string\" }, " +
                " \"description\": { \"type\": \"string\" }, " +
                " \"created_at\": { \"type\": \"string\" } }, " +
                "\"additionalProperties\": false, " +
                "\"required\": [ \"request_id\", \"client_id\", \"action\", \"description\", \"created_at\" ] }";
        SchemaRegistryHelper.registerSchema(schemaRegistryClient, topic, userSchema);
    }

    public void sendMessages(Request request) {
        if (producer != null) {

            try {
                LOG.info("Sending request: key={}, client={}, action={}", request.getRequest_id(), request.getClient_id(), request.getAction());

                ProducerRecord<String, Request> record = new ProducerRecord<>(topic, request.getClient_id(), request);
                producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        LOG.info("Message sent: partition={}, offset={}", metadata.partition(), metadata.offset());
                    } else {
                        LOG.error("Error while producing: ", exception);
                    }
                });
                producer.flush();
                LOG.info("Данные успешно отправлены");
            } catch (Exception e) {
                LOG.error("Ошибка при отправке данных: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
