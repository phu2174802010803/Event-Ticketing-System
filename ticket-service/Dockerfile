FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/ticket-service-0.0.1-SNAPSHOT.jar ticket-service.jar
ENTRYPOINT ["java", "-jar", "ticket-service.jar"]