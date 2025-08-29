## Описание проекта

Финальный проект. Источники данных для системы — SHOP API и CLIENT API. SHOP API позволяет магазинам отправлять данные о товарах.
CLIENT API предоставляет пользователям сервиса возможность выполнять запросы. Связывать все сервисы платформы будет Apache Kafka.
Аналитическая система перекладывает данные в Data Lake на базе Hadoop Distributed File System (HDFS). 
На основе этого Data Lake платформа Apache Spark, выполняет аналитические вычисления.
Обработчик в реальном времени проверяет данные от магазинов и пропускать только разрешённые товары. 
Для обеспечения надёжности и отслеживания производительности кластера Kafka реализован мониторинг на основе Prometheus и Grafana.
Кроме того, Kafka Connect сохраняет поступающие данные в файл.

### Структура проекта:
- `analytics_hadoop\hadoop_to_kafka` – приложение для вычитки данных из HDFS и отправки в Kafka.
- `analytics_hadoop\kafka_to_hadoop` – приложение для вычитки данных из Kafka и отправки в HDFS.
- `analytics_hadoop\docker-compose.yaml` – настройки для запуска образа с приложениями, взаимодействющими с HDFS
- `analytics_spark\spark_hadoop` – приложение для аналитических вычислений на базе Apache Spark.
- `analytics_spark\docker-compose.yaml` – настройки для запуска образа с аналитическим приложением
- `main_kafka_cluster\CA` – сертификаты и ключи для CA
- `main_kafka_cluster\confluent-hub-components` - папка с библиотекой connect-file
- `main_kafka_cluster\etc\alertmanager` – настройки AlertManager
- `main_kafka_cluster\etc\jmx_exporter` – настройки Jmx Exporter
- `main_kafka_cluster\etc\prometheus` – настройки Prometheus
- `main_kafka_cluster\grafana` – настройки Grafana
- `main_kafka_cluster\jaas-confs` – настройки JAAS
- `main_kafka_cluster\kafka-0-creds` – сертификаты и ключи для kafka-0
- `main_kafka_cluster\kafka-1-creds` – сертификаты и ключи для kafka-1
- `main_kafka_cluster\kafka-2-creds` – сертификаты и ключи для kafka-2
- `main_kafka_cluster\kafka-connect` – настройки kafka-connect
- `main_kafka_cluster\docker-compose.yaml` – настройки для запуска образа с основным кластером Kafka
- `other_kafka_cluster\config` – папка с настройками для HDFS
- `other_kafka_cluster\etc\alertmanager` – настройки AlertManager
- `other_kafka_cluster\etc\jmx_exporter` – настройки Jmx Exporter
- `other_kafka_cluster\etc\prometheus` – настройки Prometheus
- `other_kafka_cluster\grafana` – настройки Grafana
- `other_kafka_cluster\docker-compose.yaml` – настройки для запуска образа с дополнительным кластером Kafka
- `source_for_kafka\kafka_spring_client_producer` – приложение, реализующее CLIENT API
- `source_for_kafka\kafka_spring_shop_producer` – приложение, реализующее SHOP API
- `source_for_kafka\kafka_stream_state_store` – приложение для потоковой обработки данных о товарах, поступающих из магазинов
- `source_for_kafka\docker-compose.yaml` – настройки для запуска образа с приложениями


## Инструкция по запуску

1. Для запуска основного кластера Kafka необходимо перейти в папку main_kafka_cluster и выполнить команду:
    ```
    docker-compose up -d
    ```
	
2. Необходимо подключится к брокеру
    ```
    docker exec -it kafka-0 /bin/sh
    ```

3. Задать для топика shop-products запрет на запись сообщений в него для пользователя user,
   а для топика client-requests запрет на запись сообщений в него для пользователя shop
    ```
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:shop --operation write --topic shop-products
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:shop --operation read --topic shop-products
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:shop --operation describe --topic shop-products    
    kafka-acls --bootstrap-server kafka-0:9092 --add --deny-principal User:user --operation write --topic shop-products
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:user --operation read --topic shop-products
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:user --operation describe --topic shop-products    
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:user --operation write --topic client-requests
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:user --operation read --topic client-requests
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:user --operation describe --topic client-requests    
    kafka-acls --bootstrap-server kafka-0:9092 --add --deny-principal User:shop --operation write --topic client-requests
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:shop --operation read --topic client-requests
    kafka-acls --bootstrap-server kafka-0:9092 --add --allow-principal User:shop --operation describe --topic client-requests    
    ```

4. Для запуска дополнительного кластера Kafka необходимо перейти в папку other_kafka_cluster и выполнить команду:
    ```
    docker-compose up -d
    ```

5. Для запуска CLIENT API, SHOP API, и потоковой обработки данных необходимо перейти в папку source_for_kafka и выполнить команду:
    ```
    docker-compose up -d
    ```

5. Для запуска взаимодействия с HDFS необходимо перейти в папку analytics_hadoop и выполнить команду:
    ```
    docker-compose up -d
    ```

6. Для запуска аналитических вычислений необходимо перейти в папку analytics_spark и выполнить команду:
    ```
    docker-compose up -d
    ```

7. Сервисы, которые поднимутся:
    - Основной кластер Kafka c включеной TLS и ACL (ограничение доступа к топикам)
    - Kafka Connect для записи поступающих данных в файл
    - Второй кластер Kafka 
    - JMX Exporter для каждого брокера
    - Kafka UI для визуализации сообщений для каждого кластера Kafka.
    - Prometheus для каждого кластера Kafka.
    - Grafana для каждого кластера Kafka.
    - AlertManager для каждого кластера Kafka.
    - Приложение kafka_spring_client_producer, реализующее CLIENT API
    - Приложение kafka_spring_shop_producer, реализующее SHOP API
    - Приложение kafka_stream_state_store для потоковой обработки данных о товарах
    - Кластер Hadoop: 1 NameNode (порт 9000) и 3 DataNode (порты 9864, 9865, 9866)
    - Приложение hadoop_to_kafka, передающее сообщения из Hadoo в топик Kafka.
    - Приложение kafka_to_hadoop, передающее сообщения из топика Kafka в Hadoop.
    - Приложение spark_hadoop для аналитических вычислений на базе Apache Spark.


## Проверка работы

### Веб-интерфейсы
    Kafka UI: http://localhost:8080/
    Kafka UI2: http://localhost:8085/
	API для блокировки/разблокировки товаров: http://localhost:8090/
	CLIENT API: http://localhost:8091/
	Prometheus: http://localhost:9090/
	Prometheus2: http://localhost:9090/
	Grafana: http://localhost:3001/
	Grafana: http://localhost:3002/
	Spark: http://localhost:8086/
	Hadoop NameNode: http://localhost:9870
	HDFS Browser: http://localhost:9870/explorer.html#/data


### Принцип работа
	Приложение `kafka_spring_shop_producer` вычитывает данные о товарах из файла `\source_for_kafka\kafka_spring_shop_producer\src\main\resources\products.json` и отправляет их в топик `shop-products` основного кластера.
	Приложение `kafka_spring_client_producer` генерирует данные о товарах и отправляет их в топик `client-requests` основного кластера. 
	Кроме того, приложение `kafka_spring_client_producer` поднимает веб-интерфейс для формирования запросов клиентов, и отправляет их тоже топик `client-requests`.
	Примеры запросов:
	  http://localhost:8091/rest/get/recommendation/222 - запрос рекомендаций для клиента с ид = 222
	  http://localhost:8091/rest/search/product/smartTV/222 - запрос поиска товара = smartTV от клиента с ид = 222
	Приложение `kafka_stream_state_store` вычитывает данные о товарах из топика `shop-products` основного кластера отфильтровывает заблокированные и отправляет в топик `filtered-products`. 
	Заблокированные товары хранятся в топике `blocked-products` (поле key содержит ИД товара).
	Кроме того, приложение `kafka_stream_state_store` поднимает веб-интерфейс для добавления заблокированных товаров и их разблокировки.
	Примеры запросов:
	  http://localhost:8090/rest/block/product/12347 - запрос для блокировки товара с ид = 12347
	  http://localhost:8090/rest/unblock/product/12347 - запрос для разблокировки товара с ид = 12347
	Mirror-maker клонирует топики `client-requests`,`filtered-products` во второй кластер Kafka
	Приложение `kafka_to_hadoop` вычитывает данные из топика `filtered-products` второго кластера и отправляет в HDFS в каталог `/data/filtered-products/`.
	Приложение `spark_hadoop` вычитывает данные из каталога `/data/filtered-products/` в HDFS, обрабатывает и записывает результат (рекомендацию) в каталог `/data/recommended-products` в HDFS.
	Приложение `hadoop_to_kafka` вычитывает данные из каталога `/data/recommended-products` в HDFS и отправляет в топик `recommended-products` второго кластера.
	Kafka Connect сохраняет отфильтрованные товары и запросы клиентов в файлы. Проверка содержимого файлов, созданных Kafka Connect
	```
	docker exec <ИД контейнера kafka-connect> cat filtered-products.out
	docker exec <ИД контейнера kafka-connect> cat client-requests.out
	```
	На обоих кластерах метрики собираются через Prometheus и JMX Exporter, а Alertmanager отправляет оповещение в Telegram при сбоях.


