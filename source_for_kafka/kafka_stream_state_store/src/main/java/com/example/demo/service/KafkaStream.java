package com.example.demo.service;

import com.example.demo.domain.Product;
import com.example.demo.processors.BlockedProductsProcessor;
import com.example.demo.processors.FilterBlockedProductsProcessor;
import com.example.demo.serializers.ProductSerializer;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.example.demo.SpringBootApplication.BLOCKED_PRODUCTS_STORE_NAME;

@Component
public class KafkaStream {
    public static final Logger LOG = LoggerFactory.getLogger(KafkaStream.class);

    @Value("${blocked-products}")
    String blockedProductsTopic;

    @Value("${kafka-products-topic:}")
    String productsTopic;

    @Value("${kafka-filtered-topic}")
    String filteredMessagesTopic;

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

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() throws Exception {
        try {
            // Создаем топики
            createTopics();

            // Отправляем тестовые данные о заблокированных товаров
            sendTestMessages();

            // Создаем и запускаем Kafka Streams приложение
            KafkaStreams streams = buildStreamsApplication();
            final CountDownLatch latch = new CountDownLatch(1);

            // Обработка завершения работы
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Завершение работы приложения...");
                streams.close();
                latch.countDown();
            }));

            // Запускаем Kafka Streams
            streams.start();
            LOG.info("Приложение Kafka Stream запущено");

            // Ждем, пока приложение полностью запустится
            waitUntilKafkaStreamsIsRunning(streams);

            // Даем время на обработку данных
            LOG.info("Ожидание обработки данных...");
            TimeUnit.SECONDS.sleep(10);

            // Запрашиваем из глобального хранилища списки заблокированных товаров
            queryBlockedProductsStore(streams);

            // Ожидаем сигнала завершения
            latch.await();

        } catch (Exception e) {
            LOG.error("Ошибка при запуске приложения: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Создает необходимые топики в Kafka
     */
    private void createTopics() throws Exception {
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", servers);
        adminProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        adminProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        adminProps.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + kafkaUsername + "\" password=\"" + kafkaPassword + "\";");
        adminProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation); // Truststore
        adminProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword); // Truststore password
        adminProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, ""); // Отключение проверки hostname

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            // Проверяем существование топика
            Set<String> existingTopics = adminClient.listTopics().names().get();

            // Создаем топик для отфильтрованных сообщений
            createTopic(adminClient, existingTopics, filteredMessagesTopic);
        }
    }

    private void createTopic(AdminClient adminClient, Set<String> existingTopics, String topic) throws ExecutionException, InterruptedException {
        if (!existingTopics.contains(topic)) {
            NewTopic newTopic = new NewTopic(topic, 3, (short) 3);
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
            LOG.info("Топик " + topic + " создан");
        } else {
            LOG.info("Топик " + topic + " уже существует");
        }
    }

    /**
     * Создает и настраивает приложение Kafka Streams с использованием Processor API
     */
    private KafkaStreams buildStreamsApplication() {
        // Настройка свойств Kafka Streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "blocked-products-processor-2");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // Отключаем кеширование для наглядности
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + kafkaUsername + "\" password=\"" + kafkaPassword + "\";");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation); // Truststore
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword); // Truststore password
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, ""); // Отключение проверки hostname

        // Создаем топологию с использованием Processor API
        Topology topology = new Topology();

        // Создаем билдер для глобального хранилища заблокированных пользователей
        StoreBuilder<KeyValueStore<String, String>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(BLOCKED_PRODUCTS_STORE_NAME),
                Serdes.String(),
                Serdes.String()
        ).withLoggingDisabled(); // Отключаем логирование для глобального хранилища

        // Добавляем хранилище к топологии
        topology.addGlobalStore(
                storeBuilder,                   // Билдер хранилища
                "BlockedUsersSource",             // Имя для источника глобального хранилища
                Serdes.String().deserializer(), // Десериализатор ключа
                Serdes.String().deserializer(), // Десериализатор значения
                blockedProductsTopic,             // Входной топик для хранилища
                "BlockedProductsProcessor",          // Имя процессора
                () -> new BlockedProductsProcessor() // Поставщик процессора
        );

        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put("schema.registry.url", schemaRegistryUrl);
        serdeConfig.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, Product.class.getName());
        KafkaJsonSchemaDeserializer<Product> valueProductDeserializer = new KafkaJsonSchemaDeserializer<Product>();
        valueProductDeserializer.configure(serdeConfig, false);

        topology.addSource("messages", Serdes.String().deserializer(), valueProductDeserializer, productsTopic);
        topology.addProcessor("filterBlockedProducts", FilterBlockedProductsProcessor::new, "messages");
        topology.addSink("filteredMessages", filteredMessagesTopic, Serdes.String().serializer(), new ProductSerializer(), "filterBlockedProducts");

        LOG.info("Топология: " + topology.describe());

        // Создаем приложение Kafka Streams
        return new KafkaStreams(topology, props);
    }


    /**
     * Отправляет тестовые данные о заблокированных пользователях и запрещенных словах
     */
    private void sendTestMessages() {
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

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            LOG.info("Отправка данных о заблокированных пользоватеклях ...");

            // Отправляем данные (ID товара, "+" заблокировать товар / "-" разблокировать товар)
            producer.send(new ProducerRecord<>(blockedProductsTopic, "12348", "+Неумные часы ZIC"));
            producer.flush();
            LOG.info("Тестовые данные о блокировках успешно отправлены");


        } catch (Exception e) {
            LOG.error("Ошибка при отправке тестовых данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Запрашивает и выводит данные о заблокированных пользователях из глобального хранилища
     */
    private void queryBlockedProductsStore(KafkaStreams streams) {
        try {
            // Получаем доступ к глобальному хранилищу
            ReadOnlyKeyValueStore<String, String> blockedUsersStore = streams.store(
                    org.apache.kafka.streams.StoreQueryParameters.fromNameAndType(
                            BLOCKED_PRODUCTS_STORE_NAME,
                            org.apache.kafka.streams.state.QueryableStoreTypes.<String, String>keyValueStore()
                    )
            );

            LOG.info("=== Заблокированные товары ===");

            // Получаем все данные о блокироваках
            KeyValueIterator<String, String> all = blockedUsersStore.all();

            while (all.hasNext()) {
                org.apache.kafka.streams.KeyValue<String, String> next = all.next();
                String id = next.key;
                String name = next.value;

                LOG.info("Заблокирован ИД: " + id + " товар: " + name);
            }

            all.close();

        } catch (Exception e) {
            LOG.error("Ошибка при запросе к хранилищу: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ожидает, пока Kafka Streams перейдет в состояние RUNNING
     */
    private void waitUntilKafkaStreamsIsRunning(KafkaStreams streams) throws Exception {
        int maxRetries = 20;
        int retryIntervalMs = 2000;
        int attempt = 0;

        while (attempt < maxRetries) {
            if (streams.state() == KafkaStreams.State.RUNNING) {
                LOG.info("Kafka Streams успешно запущен");
                return;
            }

            LOG.info("Ожидание запуска Kafka Streams... Текущее состояние: " + streams.state());
            Thread.sleep(retryIntervalMs);
            attempt++;
        }

        throw new RuntimeException("Превышено время ожидания запуска Kafka Streams");
    }


}
