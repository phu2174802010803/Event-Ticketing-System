FROM openjdk:17-jdk-slim
WORKDIR /app

# Set timezone Vietnam
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY target/identity-service-0.0.1-SNAPSHOT.jar identity-service.jar
ENTRYPOINT ["java", "-jar", "identity-service.jar"]