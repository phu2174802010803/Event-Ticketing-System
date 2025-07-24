FROM openjdk:17-jdk-slim
WORKDIR /app

# Set timezone Vietnam
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY target/payment-service-0.0.1-SNAPSHOT.jar payment-service.jar
ENTRYPOINT ["java", "-jar", "payment-service.jar"]