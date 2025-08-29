package com.example.demo.service;

import com.example.demo.domain.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Component
public class MyKafkaProducer {
    public static final Logger LOG = LoggerFactory.getLogger(MyKafkaProducer.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bootstrap-servers}")
    String servers;

    @Value("${kafka-topic}")
    String topic;

    @Value("${kafka-username}")
    String kafkaUsername;

    @Value("${kafka-password}")
    String kafkaPassword;

    @Value("${truststore-location}")
    String truststoreLocation;

    @Value("${truststore-password}")
    String truststorePassword;

    @Value("${products-location}")
    String productsLocation;

    @Value("${schema-registry-url}")
    String schemaRegistryUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() throws Exception {
        // вычитываем список продуктов для отправки в кафку
        List<Product> products = new ArrayList<>();
        try {
            products = objectMapper.readValue(new File(productsLocation), new TypeReference<>() {});
        } catch (Exception e) {
        }

        if (products.isEmpty()){
            return;
        }

        // Конфигурация продюсера
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
        try(KafkaProducer<String, Product> producer = new KafkaProducer<>(properties)) {
            // Register schema
            String userSchema = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"Product\", \"type\": \"object\", " +
                    "\"properties\": { " +
                    " \"product_id\": { \"type\": \"string\" }, " +
                    " \"name\": { \"type\": \"string\" }, " +
                    " \"description\": { \"type\": \"string\" }, " +
                    " \"price\": { \"type\": \"object\", \"properties\": { \"amount\": { \"type\": \"number\" }, \"currency\": { \"type\": \"string\" } }, \"additionalProperties\": false, \"required\": [ \"amount\", \"currency\" ] }, " +
                    " \"category\": { \"type\": \"string\" }, " +
                    " \"brand\": { \"type\": \"string\" }, " +
                    " \"stock\": { \"type\": \"object\", \"properties\": { \"available\": { \"type\": \"integer\" }, \"reserved\": { \"type\": \"integer\" } }, \"additionalProperties\": false, \"required\": [ \"available\", \"reserved\" ] }, " +
                    " \"sku\": { \"type\": \"string\" }," +
                    " \"tags\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } }, " +
                    " \"images\": { \"type\": \"array\", \"items\": { \"type\": \"object\", \"properties\": { \"url\": { \"type\": \"string\" }, \"alt\": { \"type\": \"string\" } }, \"additionalProperties\": false, \"required\": [ \"url\", \"alt\" ] } }, " +
                    " \"specifications\": { \"type\": \"object\", \"properties\": { \"weight\": { \"type\": \"string\" }, \"dimensions\": { \"type\": \"string\" }, \"battery_life\": { \"type\": \"string\" }, \"water_resistance\": { \"type\": \"string\" } }, \"additionalProperties\": false, \"required\": [ \"weight\", \"dimensions\", \"battery_life\", \"water_resistance\" ] }, " +
                    " \"created_at\": { \"type\": \"string\" }, " +
                    " \"updated_at\": { \"type\": \"string\" }, " +
                    " \"index\": { \"type\": \"string\" }, " +
                    " \"store_id\": { \"type\": \"string\" } }, " +
                    "\"additionalProperties\": false, " +
                    "\"required\": [ \"product_id\", \"name\", \"description\", \"price\", \"category\", \"brand\", \"stock\", \"sku\", \"tags\", \"images\", \"specifications\", \"created_at\", \"updated_at\", \"index\", \"store_id\" ] }";
            SchemaRegistryHelper.registerSchema(schemaRegistryClient, topic, userSchema);


            for (Product product: products){
                LOG.info("Sending product: key={}, name={}", product.getProduct_id(), product.getName());

                ProducerRecord<String, Product> record = new ProducerRecord<>(topic, product.getProduct_id(), product);
                producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        LOG.info("Message sent: partition={}, offset={}", metadata.partition(), metadata.offset());
                    } else {
                        LOG.error("Error while producing: ", exception);
                    }
                });
                Thread.sleep(1000);

            }
            producer.flush();
        } catch (Exception e) {
            LOG.error("Error send message to kafka!");
        }

    }

}
