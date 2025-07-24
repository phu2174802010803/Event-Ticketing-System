FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/gateway-app-0.0.1-SNAPSHOT.jar gateway-app.jar
ENTRYPOINT ["java", "-jar", "gateway-app.jar"]