для создания сертификата для брокера Kafka необходимо:
В папке с корневым сертификатом D:\Denis\Kafka\Lesson5\practic\CA создаем папку kafka-1-creds.
Далее все команды выполняются в папке D:\Denis\Kafka\Lesson5\practic\CA

1. Подготовить файл kafka-1.cnf – конфигурационный файл для создания сертификатов и ключей с использованием OpenSSL
 и положить его в папку kafka-1-creds

2. Для создания приватного ключа (kafka-1.key) и запроса на сертификат (kafka-1.csr) выполнить команду: 
  openssl req -new -newkey rsa:2048 -keyout kafka-1-creds/kafka-1.key -out kafka-1-creds/kafka-1.csr -config kafka-1-creds/kafka-1.cnf -nodes

3. Создадим сертификат брокера, подписанный CA
  openssl x509 -req -days 3650 -in kafka-1-creds/kafka-1.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out kafka-1-creds/kafka-1.crt -extfile kafka-1-creds/kafka-1.cnf -extensions v3_req

4. Создадим PKCS12-хранилище с сертификатом брокера:
  openssl pkcs12 -export -in kafka-1-creds/kafka-1.crt -inkey kafka-1-creds/kafka-1.key -chain -CAfile ca.pem -name kafka-1 -out kafka-1-creds/kafka-1.p12 -password pass:password

5. Создадим keystore для Kafka
  C:\Program` Files\Java\jdk-18.0.2.1\bin\keytool.exe -importkeystore -deststorepass password -destkeystore kafka-1-creds/kafka.kafka-1.keystore.pkcs12 -srckeystore kafka-1-creds/kafka-1.p12 -deststoretype PKCS12 -srcstoretype PKCS12 -noprompt -srcstorepass password
или
  keytool.exe -importkeystore -deststorepass password -destkeystore D:\Denis\Kafka\Lesson5\practic\CA\kafka-1-creds\kafka.kafka-1.keystore.pkcs12 -srckeystore D:\Denis\Kafka\Lesson5\practic\CA\kafka-1-creds\kafka-1.p12 -deststoretype PKCS12 -srcstoretype PKCS12 -noprompt -srcstorepass password

6. Создадим truststore для Kafka
  C:\Program` Files\Java\jdk-18.0.2.1\bin\keytool.exe -import -file ca.crt -alias ca -keystore kafka-1-creds/kafka.kafka-1.truststore.jks -storepass your-password -noprompt 
или
  keytool.exe -import -file D:\Denis\Kafka\Lesson5\practic\CA\ca.crt -alias ca -keystore D:\Denis\Kafka\Lesson5\practic\CA\kafka-1-creds\kafka.kafka-1.truststore.jks -storepass password -noprompt 

7. Сохраним пароли
echo "password" > kafka-1-creds/kafka-1_sslkey_creds
echo "password" > kafka-1-creds/kafka-1_keystore_creds
echo "password" > kafka-1-creds/kafka-1_truststore_creds 

8. Импортируем PKCS12 в JKS:
  C:\Program` Files\Java\jdk-18.0.2.1\bin\keytool.exe -importkeystore -srckeystore kafka-1-creds/kafka-1.p12 -srcstoretype PKCS12 -destkeystore kafka-1-creds/kafka-1.keystore.jks -deststoretype JKS -deststorepass password 

9. Импортируем CA в Truststore:
  C:\Program` Files\Java\jdk-18.0.2.1\bin\keytool.exe -import -trustcacerts -file ca.crt -keystore kafka-1-creds/kafka-1.truststore.jks -storepass password -noprompt -alias ca