FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app
COPY kafka_to_hadoop/pom.xml .
RUN mvn dependency:go-offline
COPY kafka_to_hadoop/src /app/src
RUN mvn clean package -DskipTests

FROM adoptopenjdk/openjdk11:alpine-jre
COPY --from=build /app/target/kafka_to_hadoop*.jar  kafka_to_hadoop.jar
ENTRYPOINT ["java","-jar","/kafka_to_hadoop.jar"]