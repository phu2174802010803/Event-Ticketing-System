FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/identity-service-0.0.1-SNAPSHOT.jar identity-service.jar
ENTRYPOINT ["java", "-jar", "identity-service.jar"]