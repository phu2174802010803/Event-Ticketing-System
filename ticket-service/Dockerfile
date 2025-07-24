FROM openjdk:17-jdk-slim
WORKDIR /app

# Set timezone Vietnam
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY target/ticket-service-0.0.1-SNAPSHOT.jar ticket-service.jar
ENTRYPOINT ["java", "-jar", "ticket-service.jar"]