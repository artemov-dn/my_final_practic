FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app
COPY spark_hadoop/pom.xml .
RUN mvn dependency:go-offline
COPY spark_hadoop/src /app/src
RUN mvn clean package -DskipTests

FROM adoptopenjdk/openjdk11:alpine-jre
COPY --from=build /app/target/spark_hadoop*.jar  spark_hadoop.jar
ENTRYPOINT ["java","-jar","/spark_hadoop.jar"]