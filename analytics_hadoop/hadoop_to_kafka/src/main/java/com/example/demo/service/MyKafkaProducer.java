package com.example.demo.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

@Component
public class MyKafkaProducer {
    public static final Logger LOG = LoggerFactory.getLogger(MyKafkaProducer.class);

    KafkaProducer<String, String> producer;
    FileSystem hdfs;
    Path pathToList;

    @Value("${bootstrap-servers}")
    String servers;

    @Value("${kafka-topic}")
    String topic;

    @Value("${hadoop-server}")
    String hadoopServer;

    @Value("${hadoop-path}")
    String hadoopPath;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() throws Exception {
        // Конфигурация продюсера
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(properties);

        String hdfsUri = "hdfs://" + hadoopServer;
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);

        hdfs = FileSystem.get(new URI(hdfsUri), conf, "root");
        pathToList = new Path(hadoopPath);

    }

    @Scheduled(fixedDelay = 5000)
    public void pollMessage() {
        if (pathToList != null && hdfs != null && producer != null) {
            try {
                if (hdfs.exists(pathToList)) {
                    RemoteIterator<LocatedFileStatus> fileIterator = hdfs.listFiles(pathToList, true);

                    while (fileIterator.hasNext()) {
                        LocatedFileStatus fileStatus = fileIterator.next();
                        LOG.info("Get file: " + fileStatus.getPath().getName());

                        StringBuilder content;
                        //  Чтение файла из HDFS
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(hdfs.open(fileStatus.getPath()), StandardCharsets.UTF_8))) {
                            content = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                content.append(line).append("\n");
                            }
                            LOG.info(" Содержимое: '" + content.toString().trim() + "'");
                        }
                        ProducerRecord<String, String> record = new ProducerRecord<>(topic, UUID.randomUUID().toString(), content.toString().trim());
                        producer.send(record, (metadata, exception) -> {
                            if (exception == null) {
                                LOG.info("Message sent: partition={}, offset={}", metadata.partition(), metadata.offset());
                            } else {
                                LOG.error("Error while producing: ", exception);
                            }
                        });
                        hdfs.delete(fileStatus.getPath(), true);

                    }
                    producer.flush();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (Exception e) {
                LOG.error("Error get message from hadoop and send to kafka!");
            }
        }
    }
}
