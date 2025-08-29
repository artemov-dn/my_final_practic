FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app
COPY hadoop_to_kafka/pom.xml .
RUN mvn dependency:go-offline
COPY hadoop_to_kafka/src /app/src
RUN mvn clean package -DskipTests

FROM adoptopenjdk/openjdk11:alpine-jre
COPY --from=build /app/target/hadoop_to_kafka*.jar  hadoop_to_kafka.jar
ENTRYPOINT ["java","-jar","/hadoop_to_kafka.jar"]