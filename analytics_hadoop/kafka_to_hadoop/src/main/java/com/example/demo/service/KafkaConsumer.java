package com.example.demo.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

@Component
public class KafkaConsumer {
    public static final Logger LOG = LoggerFactory.getLogger(KafkaConsumer.class);

    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;
    FileSystem hdfs;

    @Value("${bootstrap-servers}")
    String servers;

    @Value("${kafka-topics}")
    String topics;

    @Value("${hadoop-server}")
    String hadoopServer;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() throws Exception {
        // Конфигурация консумера
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-group-2");        // Уникальный идентификатор группы
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");        // Начало чтения с самого начала
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");           // Автоматический коммит смещений
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000");

        // Создание консьюмера
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(properties);
        // Подписка на топик
        consumer.subscribe(Arrays.asList(topics.split(",")));

        String hdfsUri = "hdfs://" + hadoopServer;
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);

        hdfs = FileSystem.get(new URI(hdfsUri), conf, "root");

    }

    @Scheduled(fixedDelay = 5000)
    public void pollMessage() {
        if (consumer != null && hdfs != null) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));  // Получение сообщений
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, String> record : records) {
                        LOG.info("Received message from topic '{}': {}", record.topic(), record.value());

                        String value = record.value();
                        String hdfsFilePath = "/data/" + record.topic() + "/" + UUID.randomUUID();
                        Path path = new Path(hdfsFilePath);

                        // Запись файла в HDFS
                        try (FSDataOutputStream outputStream = hdfs.create(path, true)) {
                            outputStream.writeUTF(value);
                        }
                        System.out.println("Сообщение записано в HDFS по пути: " + hdfsFilePath);

                    }
                }
            } catch (RecordDeserializationException e) {
                LOG.error("Error deserializing message!");
            } catch (Exception e) {
                LOG.error("Error get message from kafka!");
            }
        }
    }
}
