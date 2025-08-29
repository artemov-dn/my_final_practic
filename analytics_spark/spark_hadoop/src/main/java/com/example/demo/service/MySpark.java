package com.example.demo.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

@Component
public class MySpark {
    public static final Logger LOG = LoggerFactory.getLogger(MySpark.class);

    KafkaProducer<String, String> producer;
    FileSystem hdfs;
    String hdfsUri;

    @Value("${hadoop-server}")
    String hadoopServer;

    @Value("${hadoop-in-path}")
    String hadoopInPath;

    @Value("${hadoop-out-path}")
    String hadoopOutPath;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() throws Exception {

        hdfsUri = "hdfs://" + hadoopServer;
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);

        hdfs = FileSystem.get(new URI(hdfsUri), conf, "root");

        SparkConf sparkConf = new SparkConf().setAppName("KafkaHdfsSparkConsumer").setMaster("local[*]");
        try (JavaSparkContext sc = new JavaSparkContext(sparkConf)) {
            JavaRDD<String> hdfsData = sc.textFile(hdfsUri + hadoopInPath +"/*");
            long count = hdfsData.count();
            System.out.println("Общее количество записей в HDFS: " + count);
            List<String> allData = new ArrayList<>(hdfsData.collect());
            LOG.info("Spark рекомендует: " + allData.get(0));

            String hdfsFilePath = hadoopOutPath + "/" + UUID.randomUUID();
            Path path = new Path(hdfsFilePath);

            // Запись файла в HDFS
            try (FSDataOutputStream outputStream = hdfs.create(path, true)) {
                outputStream.writeUTF(allData.get(0));
            }
            System.out.println("Сообщение записано в HDFS по пути: " + hdfsFilePath);

        }
    }
}
