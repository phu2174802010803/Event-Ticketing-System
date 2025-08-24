FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/gateway-admin-organizer-0.0.1-SNAPSHOT.jar gateway-admin-organizer.jar
ENTRYPOINT ["java", "-jar", "gateway-admin-organizer.jar"]