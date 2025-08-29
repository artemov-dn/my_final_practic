FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY kafka_stream_state_store/pom.xml .
RUN mvn dependency:go-offline
COPY kafka_stream_state_store/src /app/src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
COPY kafka_stream_state_store/kafka-creds /app/kafka-creds
COPY --from=build /app/target/kafka_stream_state_store*.jar  kafka_stream.jar
ENTRYPOINT ["java","-jar","/kafka_stream.jar"]

